package com.fireflysource.common.jni;

import com.fireflysource.common.concurrent.AutoLock;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public class JniLibLoader {

    private static final LazyLogger logger = SystemLogger.create(JniLibLoader.class);
    private static final Set<String> loadedLibs = new HashSet<>();
    private static final AutoLock lock = new AutoLock();

    /**
     * Load JNI lib by lib name.
     *
     * @param libName The lib name.
     */
    public static void load(String libName) {
        String libPath = getLibPath(libName);
        loadByLibPath(libPath);
    }

    /**
     * Load JNI lib by lib file path.
     *
     * @param libPath The lib file path of the lib file.
     */
    public static void loadByLibPath(String libPath) {
        lock.lock(() -> {
            if (libPath.startsWith("/")) {
                throw new IllegalArgumentException("The lib path must be not start with /");
            }

            if (loadedLibs.contains(libPath)) {
                logger.info("The lib is loaded. path: {}", libPath);
                return;
            }

            File file = createLibTempFile(libPath);
            copyLibToTempFile(libPath, file);

            logger.info("Start to load lib. path: {}", libPath);
            System.load(getLibCanonicalPath(libPath, file));
            loadedLibs.add(libPath);
            logger.info("Load lib success. path: {}", libPath);
        });
    }

    public static String getLibPath(String libName) {
        String osName = System.getProperty("os.name").toLowerCase();
        String libSuffix;
        String libDir;
        if (osName.contains("mac")) {
            libSuffix = ".dylib";
            libDir = "macos";
        } else if (osName.contains("win")) {
            libSuffix = ".dll";
            libDir = "windows";
        } else {
            libSuffix = ".so";
            libDir = "linux";
        }
        return "lib/" + libDir + "/lib" + libName + libSuffix;
    }

    public static String getLibFileName(String libPath) {
        int pos = libPath.lastIndexOf("/");
        String libFileName;
        if (pos >= 0) {
            libFileName = libPath.substring(pos + 1);
        } else {
            libFileName = libPath;
        }
        return libFileName;
    }

    private static String getLibCanonicalPath(String libPath, File file) {
        String tempFilePath;
        try {
            tempFilePath = file.getCanonicalPath();
        } catch (IOException e) {
            logger.error("Get lib temp file path exception.", e);
            throw new JniLibTempFileException("get lib temp file path exception. path: " + libPath);
        }
        return tempFilePath;
    }

    private static void copyLibToTempFile(String libPath, File file) {
        try (InputStream input = JniLibLoader.class.getResourceAsStream("/" + libPath)) {
            if (input == null) {
                throw new JniLibNotFoundException("The lib not found. path: " + libPath);
            }

            logger.info("Copy lib to temp file. lib path: {}, temp file: {}", libPath, file.toPath());
            Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Copy lib exception.", e);
            throw new JniLibTempFileException("Copy lib exception. lib file path: " + file.toPath());
        }
    }

    private static File createLibTempFile(String libPath) {
        String libFileName = getLibFileName(libPath);
        File file;
        try {
            file = Files.createTempFile("jni", libFileName).toFile();
        } catch (IOException e) {
            logger.error("Create lib temp file exception.", e);
            throw new JniLibTempFileException("create lib temp file exception. file name: " + libFileName);
        }
        file.deleteOnExit();
        return file;
    }

}
