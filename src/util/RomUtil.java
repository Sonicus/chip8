package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RomUtil {

    public static byte[] LoadRom(String filepath) throws IOException{
        Path path = Paths.get(filepath);
        return Files.readAllBytes(path);
    }
}
