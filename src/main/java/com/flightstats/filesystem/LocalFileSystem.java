package com.flightstats.filesystem;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class LocalFileSystem implements FileSystem {

    @Override
    @SneakyThrows
    public OutputStream outputStream(Path fileName) {
        fileName.toFile().getParentFile().mkdirs();
        return new FileOutputStream(fileName.toFile());
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
    @SneakyThrows
    public List<Path> listFiles(Path directory) {
        return Files.list(directory).collect(toList());
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
