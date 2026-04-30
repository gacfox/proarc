package com.gacfox.proarc.oss;

import lombok.Getter;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AWS S3 SDK v2 实现
 */
@Getter
public class S3OssTemplate implements OssTemplate {
    private final OssConfigure ossConfigure;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3OssTemplate(OssConfigure ossConfigure) {
        this.ossConfigure = ossConfigure;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                ossConfigure.getAccessKey(), ossConfigure.getSecretKey());
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        Region region = Region.of(ossConfigure.getRegion());
        URI endpoint = URI.create(ossConfigure.getEndpoint());

        S3Configuration serviceConfig = S3Configuration.builder()
                .pathStyleAccessEnabled(ossConfigure.isPathStyleAccessEnabled())
                .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfig)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfig)
                .build();
    }

    @Override
    public void uploadFile(String fileKey, File file) {
        uploadFile(ossConfigure.getDefaultBucketName(), fileKey, file);
    }

    @Override
    public void uploadFile(String fileKey, InputStream inputStream) {
        uploadFile(ossConfigure.getDefaultBucketName(), fileKey, inputStream);
    }

    @Override
    public void uploadFile(String bucketName, String fileKey, File file) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        s3Client.putObject(request, RequestBody.fromFile(file));
    }

    @Override
    public void uploadFile(String bucketName, String fileKey, InputStream inputStream) {
        try {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> asyncUploadFile(String fileKey, File file) {
        return CompletableFuture.runAsync(() -> uploadFile(fileKey, file));
    }

    @Override
    public CompletableFuture<Void> asyncUploadFile(String fileKey, InputStream inputStream) {
        return CompletableFuture.runAsync(() -> uploadFile(fileKey, inputStream));
    }

    @Override
    public CompletableFuture<Void> asyncUploadFile(String bucketName, String fileKey, File file) {
        return CompletableFuture.runAsync(() -> uploadFile(bucketName, fileKey, file));
    }

    @Override
    public CompletableFuture<Void> asyncUploadFile(String bucketName, String fileKey, InputStream inputStream) {
        return CompletableFuture.runAsync(() -> uploadFile(bucketName, fileKey, inputStream));
    }

    @Override
    public void deleteFile(String fileKey) {
        deleteFile(ossConfigure.getDefaultBucketName(), fileKey);
    }

    @Override
    public void deleteFile(String bucketName, String fileKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        s3Client.deleteObject(request);
    }

    @Override
    public List<String> listFileKeys() {
        return listFileKeys(ossConfigure.getDefaultBucketName());
    }

    @Override
    public List<String> listFileKeys(String bucketName) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listFileKeysByPrefix(String prefix) {
        return listFileKeysByPrefix(ossConfigure.getDefaultBucketName(), prefix);
    }

    @Override
    public List<String> listFileKeysByPrefix(String bucketName, String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();
        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    @Override
    public OssFileMetadata getFileMetaData(String fileKey) {
        return getFileMetaData(ossConfigure.getDefaultBucketName(), fileKey);
    }

    @Override
    public OssFileMetadata getFileMetaData(String bucketName, String fileKey) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        HeadObjectResponse response = s3Client.headObject(request);
        OssFileMetadata metadata = new OssFileMetadata();
        metadata.setBucketName(bucketName);
        metadata.setKey(fileKey);
        metadata.setSize(response.contentLength());
        metadata.setLastModified(Date.from(response.lastModified()));
        return metadata;
    }

    @Override
    public java.net.URL generatePresignedUrl(String fileKey, Date expiration) {
        return generatePresignedUrl(ossConfigure.getDefaultBucketName(), fileKey, expiration);
    }

    @Override
    public java.net.URL generatePresignedUrl(String bucketName, String fileKey, Date expiration) {
        Duration duration = Duration.between(Instant.now(), expiration.toInstant());
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build())
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url();
    }

    @Override
    public byte[] getFileBytes(String fileKey) {
        return getFileBytes(ossConfigure.getDefaultBucketName(), fileKey);
    }

    @Override
    public byte[] getFileBytes(String bucketName, String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        try (InputStream in = s3Client.getObject(request)) {
            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFileBase64(String fileKey) {
        return getFileBase64(ossConfigure.getDefaultBucketName(), fileKey);
    }

    @Override
    public String getFileBase64(String bucketName, String fileKey) {
        byte[] bytes = getFileBytes(bucketName, fileKey);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public InputStream getFileStream(String fileKey) {
        return getFileStream(ossConfigure.getDefaultBucketName(), fileKey);
    }

    @Override
    public InputStream getFileStream(String bucketName, String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        return s3Client.getObject(request);
    }

    @Override
    public InputStream getFileStreamByRange(String fileKey, long begin, long end) {
        return getFileStreamByRange(ossConfigure.getDefaultBucketName(), fileKey, begin, end);
    }

    @Override
    public InputStream getFileStreamByRange(String bucketName, String fileKey, long begin, long end) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .range("bytes=" + begin + "-" + end)
                .build();
        return s3Client.getObject(request);
    }

    @Override
    public void copyFile(String sourceBucketName, String sourceKey,
                         String destinationBucketName, String destinationKey) {
        CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(sourceBucketName)
                .sourceKey(sourceKey)
                .destinationBucket(destinationBucketName)
                .destinationKey(destinationKey)
                .build();
        s3Client.copyObject(request);
    }

    @Override
    public boolean doesFileExist(String fileKey) {
        return doesFileExist(ossConfigure.getDefaultBucketName(), fileKey);
    }

    @Override
    public boolean doesFileExist(String bucketName, String fileKey) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        try {
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
