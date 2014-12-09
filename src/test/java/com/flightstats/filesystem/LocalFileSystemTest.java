package com.flightstats.filesystem;

import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import java.nio.file.Files;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static junit.framework.Assert.assertEquals;

public class LocalFileSystemTest {
    @Test
    public void testListFiles() throws Exception {
        Path tempdir = Files.createTempDirectory("test");
        try {
            for (int x = 1; x < 9; x++) {
                for (int y=1; y < 9; y++) {
                    final Path dir = tempdir.resolve(String.valueOf(x) + "/" + String.valueOf(y));
                    dir.toFile().mkdirs();
                    new FileWriter(dir.resolve(String.valueOf(x)).toFile()).append("").close();
                }
            }
            final List<Path> paths = new LocalFileSystem().listFiles(Paths.get("/tmp/t/9/9/"));
            assertEquals(11, paths.size());
            assertEquals(
                    "/tmp/t/9/1/9\n" +
                    "/tmp/t/9/10/9\n" +
                    "/tmp/t/9/2/9\n" +
                    "/tmp/t/9/3/9\n" +
                    "/tmp/t/9/4/9\n" +
                    "/tmp/t/9/5/9\n" +
                    "/tmp/t/9/6/9\n" +
                    "/tmp/t/9/7/9\n" +
                    "/tmp/t/9/8/9\n" +
                    "/tmp/t/9/9/9\n" +
                    "/tmp/t/9/9/9",

                    Joiner.on("\n").join(Ordering.natural().sortedCopy(paths)));
        } finally {
            for (int x = 1; x < 9; x++) {
                for (int y=1; y < 9; y++) {
                    final Path dir = tempdir.resolve(String.valueOf(x) + "/" + String.valueOf(y));
                    dir.resolve(String.valueOf(x)).toFile().delete();
                    dir.toFile().delete();
                }
                tempdir.resolve(String.valueOf(x)).toFile().delete();
            }

            tempdir.toFile().delete();
//            for (int x = 1; x < 9; x++) {
//                for (int y=1; y < 9; y++) {
//
//                }
//            }
//            tempdir.delete();
        }
    }
}
