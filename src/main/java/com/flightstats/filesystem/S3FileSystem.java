package com.flightstats.filesystem;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.google.common.base.Joiner;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.transform;

public class S3FileSystem implements FileSystem {
    private final static Logger logger = LoggerFactory.getLogger(S3FileSystem.class);

    private final AmazonS3 s3;
    private final String bucketName;

    public S3FileSystem(AmazonS3 s3, String bucketName) {
        this.s3 = s3;
        this.bucketName = bucketName;
    }

    private String makeFileName(Path file) {
        return Joiner.on("/").join(file.iterator());
    }

    @Override
    public OutputStream outputStream(Path fileName) {
        return new ChunkingS3OutputStream(makeFileName(fileName));
    }

    @Override
    @SneakyThrows
    public String readContents(Path fileName) {
        return IOUtils.toString(inputStream(fileName));
    }

    @Override
    @SneakyThrows
    public boolean exists(Path fileName) {
        try {
            S3Object object = s3.getObject(bucketName, makeFileName(fileName));
            object.close();
            return true;
        } catch (AmazonS3Exception e) {
            //this is a bit of a kludge-o.  for some reason, we get a 403 when we can't read the file...sometimes.
            if (e.getStatusCode() == 404 || e.getStatusCode() == 403) {
                return false;
            }
            throw e;
        }
    }

    @Override
    @SneakyThrows
    public void saveContent(String content, Path fileName) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream(fileName)))) {
            writer.write(content);
        }
    }

    @Override
    public List<Path> listFiles(Path prefixPath) {
        String prefix = makeFileName(prefixPath);
        ObjectListing objectListing = s3.listObjects(bucketName, prefix);
        List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();
        //todo: figure out how best to get the rest of them if it's truncated.
//        if (objectListing.isTruncated()) {
//             objectListing = s3.listNextBatchOfObjects(objectListing);
//        }
        return transform(summaries, objectSummary -> Paths.get(objectSummary.getKey()));
    }

    @Override
    public void move(Path file, Path destinationDirectory) {
        String sourceKey = makeFileName(file);
        String destinationKey = makeFileName(destinationDirectory.resolve(file.getFileName()));
        s3.copyObject(bucketName, sourceKey, bucketName, destinationKey);
        s3.deleteObject(bucketName, sourceKey);
    }

    @Override
    @SneakyThrows
    public InputStream inputStream(Path fileName) {
        try (S3ObjectInputStream s3ObjectInputStream = s3.getObject(bucketName, makeFileName(fileName)).getObjectContent()) {
            File tempFile = File.createTempFile("s3Temp", null);
            tempFile.deleteOnExit();
            Files.copy(s3ObjectInputStream, Paths.get(tempFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
            return new FileInputStream(tempFile);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new UncheckedIOException(new FileNotFoundException("file not found in S3: " + fileName));
            }
            throw e;
        }
    }

    private class ChunkingS3OutputStream extends OutputStream {
        private static final int CHUNK_SIZE = 5 * 1024 * 1024;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream(CHUNK_SIZE);
        private final String fileName;
        private InitiateMultipartUploadResult initiateMultipartUploadResult;
        private int partNumber = 0;
        private final List<PartETag> eTags = new ArrayList<>();

        public ChunkingS3OutputStream(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void write(int b) throws IOException {
            bytes.write(b);
            if (bytes.size() >= CHUNK_SIZE) {
                flush();
            }
        }

        @Override
        public void flush() throws IOException {
            doFlush(false);
        }

        private void doFlush(boolean force) {
            if (initiateMultipartUploadResult == null) {
                initiateMultipartUploadResult = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, fileName));
            }
            if (bytes.size() == 0 && partNumber > 0) {
//                logger.info("skipping flushing...zero bytes remaining");
                return;
            }
            if (!force && bytes.size() < CHUNK_SIZE) {
//                logger.info("skipping flushing...not forced and not enough bytes");
                return;
            }
            logger.debug("Flushing to S3 with " + bytes.size() + " bytes");
            UploadPartRequest uploadPartRequest = new UploadPartRequest()
                    .withBucketName(bucketName)
                    .withPartNumber(++partNumber)
                    .withPartSize(bytes.size())
                    .withKey(fileName)
                    .withUploadId(initiateMultipartUploadResult.getUploadId())
                    .withInputStream(new ByteArrayInputStream(bytes.toByteArray()));
            UploadPartResult uploadPartResult = s3.uploadPart(uploadPartRequest);
            eTags.add(uploadPartResult.getPartETag());
            bytes.reset();
        }

        @Override
        public void close() throws IOException {
            doFlush(true);
            if (partNumber == 0 && bytes.size() == 0) {
                //nothing to save, so avoid the S3 error.
                return;
            }
            s3.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, fileName, initiateMultipartUploadResult.getUploadId(), eTags));
        }
    }


    public static void main(String[] args) {
        S3FileSystem fs = new S3FileSystem(new AmazonS3Client(), "analytics-dev-useast1-dwload");
        fs.saveContent("", Paths.get("jkw-dev-test", "testfile.txt"));
    }
}
