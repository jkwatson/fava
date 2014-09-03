package com.flightstats.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

public interface FileSystem {
    OutputStream outputStream(Path fileName);

    InputStream inputStream(Path fileName);

    String readContents(Path fileName);

    boolean exists(Path fileName);

    void saveContent(String content, Path fileName);

    List<Path> listFiles(Path directory);

    void move(Path file, Path destinationDirectory);
}
