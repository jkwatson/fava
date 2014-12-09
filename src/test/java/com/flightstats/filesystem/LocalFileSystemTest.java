package com.flightstats.filesystem;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
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
            final List<Path> paths = new LocalFileSystem().listFiles(tempdir.resolve("8/8/"));
            assertEquals(9, paths.size());
            final List<Path> relativePaths = Lists.transform(paths, tempdir::relativize);
            assertEquals("8/1/8\n" +
                        "8/2/8\n" +
                        "8/3/8\n" +
                        "8/4/8\n" +
                        "8/5/8\n" +
                        "8/6/8\n" +
                        "8/7/8\n" +
                        "8/8/8\n" +
                        "8/8/8",

                    Joiner.on("\n").join(Ordering.natural().sortedCopy(relativePaths)));
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
