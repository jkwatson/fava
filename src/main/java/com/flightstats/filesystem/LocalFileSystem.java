package com.flightstats.filesystem;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class LocalFileSystem implements FileSystem {
    private final static Logger logger = LoggerFactory.getLogger(LocalFileSystem.class);

    @Override
    @SneakyThrows
    public OutputStream outputStream(Path fileName) {
        fileName.toFile().getParentFile().mkdirs();
        return new FileOutputStream(fileName.toFile());
    }

    @Override
    public OutputStream outputStream(Path fileName, String contentType) {
        return outputStream(fileName);
    }

    @Override
    @SneakyThrows
    public InputStream inputStream(Path fileName) {
        return new FileInputStream(fileName.toFile());
    }

    @Override
    @SneakyThrows
    public String readContents(Path fileName) {
        return Joiner.on('\n').join(Files.readAllLines(fileName, Charsets.UTF_8));
    }

    @Override
    public boolean exists(Path fileName) {
        return fileName.toFile().exists();
    }

    @Override
    @SneakyThrows
    public void saveContent(String content, Path fileName) {
        fileName.toFile().getParentFile().mkdirs();
        Files.write(fileName, content.getBytes(Charsets.UTF_8));
    }

    @Override
    public void saveContent(String content, Path fileName, String contentType) {
        saveContent(content, fileName);
    }

    @Override
    public List<Path> listFiles(Path prefixPath) {
        //all this stuff is to make this method work like S3 does when you give it a prefix to search for.
        Path parent = prefixPath.getParent();
        String prefix = prefixPath.getFileName().toString();

        try {
            Stream<Path> directories = Files.find(parent, 1, (path, attributes) -> path.getFileName().toString().startsWith(prefix));
            Stream<Path> files = directories.flatMap(d -> {
                try {
                    return Files.find(d, 100, (path, basicFileAttributes) -> basicFileAttributes.isRegularFile());
                } catch (IOException e) {
                    logger.warn("Error listing directory: " + prefixPath, e);
                    return Stream.empty();
                }
            });
            return files.collect(toList());
        } catch (IOException e) {
            logger.warn("Error listing directory: " + prefixPath, e);
            return Collections.emptyList();
        }
    }

    @Override
    @SneakyThrows
    public void move(Path file, Path destinationDirectory) {
        Files.move(file, destinationDirectory.resolve(makeFileName(file)));
    }

    private String makeFileName(Path file) {
        return Joiner.on("/").join(file.iterator());
    }

}
