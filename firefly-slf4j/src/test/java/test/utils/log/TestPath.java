package test.utils.log;

import com.firefly.utils.exception.CommonRuntimeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class TestPath {
    public static void main(String[] args) throws IOException {
        String userHome = System.getProperty("user.home");
        String logPath = Paths.get(userHome,"/Develop/logs").toString();
        String name = "firefly-system";
        List<Path> list = Files.list(Paths.get(logPath)).filter(path ->
                Files.exists(path) && Files.isReadable(path) && !Files.isDirectory(path) && path.toFile().getName().startsWith(name)
        ).sorted((p1, p2) -> {
            try {
                return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
            } catch (IOException e) {
                throw new CommonRuntimeException(e);
            }
        }).collect(Collectors.toList());

        list.forEach(path -> {
            try {
                System.out.println(path.toFile().getName() + "|" + Files.size(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println(Files.exists(Paths.get(logPath, "firefly-system.2016-11-01.txt")));
        System.out.println(Paths.get(logPath, "firefly-system.2016-11-02.txt"));
    }
}
