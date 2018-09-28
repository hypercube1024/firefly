package test.utils.io;

import com.firefly.utils.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pengtao Qiu
 */
public class FileUtilsDemo {
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("/Users/bjhl/Develop/git_workspace/test/firefly-web-seed");
        FileUtils.delete(path, ".DS_Store.mustache");

//        FileUtils.filter(path, "*", filePath -> {
//            try {
//                Files.move(filePath, Paths.get(filePath.toString() + ".mustache"));
//                System.out.println(filePath);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
    }
}
