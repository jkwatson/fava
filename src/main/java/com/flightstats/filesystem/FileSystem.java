package com.flightstats.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

public interface FileSystem {
    OutputStream outputStream(Path fileName);

    OutputStream outputStream(Path fileName, String contentType);

    InputStream inputStream(Path fileName);

    String readContents(Path fileName);

    boolean exists(Path fileName);

    void saveContent(String content, Path fileName);

    void saveContent(String content, Path fileName, String contentType);

    /**
     * This will return a list of all paths that are prefixed with the provided Path, even if that Path doesn't point at
     * a "directory", per se.  If the path does not exist, an empty List will be returned.
     */
    List<Path> listFiles(Path prefixPath);

    void move(Path file, Path destinationDirectory);
}
