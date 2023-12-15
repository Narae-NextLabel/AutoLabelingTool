package com.next.label.controller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    public static void zipDirectory(String sourceDirectory, String zipFilePath) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            Path sourcePath = Paths.get(sourceDirectory);
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> addToZip(sourcePath, path, zipOutputStream));
        }
    }

    private static void addToZip(Path sourcePath, Path path, ZipOutputStream zipOutputStream) {
        try {
            String relativePath = sourcePath.relativize(path).toString();
            zipOutputStream.putNextEntry(new ZipEntry(relativePath));

            Files.copy(path, zipOutputStream);

            zipOutputStream.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

