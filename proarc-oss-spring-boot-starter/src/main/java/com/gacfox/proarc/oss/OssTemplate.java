package com.gacfox.proarc.oss;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OSS操作模板
 */
public interface OssTemplate {
    /**
     * 获取OSS配置信息
     *
     * @return 配置对象
     */
    OssConfigure getOssConfigure();

    /**
     * 上传文件
     *
     * @param fileKey OSS文件Key
     * @param file    文件对象
     */
    void uploadFile(String fileKey, File file);

    /**
     * 上传文件
     *
     * @param fileKey     OSS文件Key
     * @param inputStream 上传流
     */
    void uploadFile(String fileKey, InputStream inputStream);

    /**
     * 上传文件
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    OSS文件Key
     * @param file       文件对象
     */
    void uploadFile(String bucketName, String fileKey, File file);

    /**
     * 上传文件
     *
     * @param bucketName  OSS存储桶名称
     * @param fileKey     OSS文件Key
     * @param inputStream 上传流
     */
    void uploadFile(String bucketName, String fileKey, InputStream inputStream);

    /**
     * 异步上传文件
     *
     * @param fileKey OSS文件Key
     * @param file    文件对象
     * @return 异步任务
     */
    CompletableFuture<Void> asyncUploadFile(String fileKey, File file);

    /**
     * 异步上传文件
     *
     * @param fileKey     OSS文件Key
     * @param inputStream 上传流
     * @return 异步任务
     */
    CompletableFuture<Void> asyncUploadFile(String fileKey, InputStream inputStream);

    /**
     * 异步上传文件
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    OSS文件Key
     * @param file       文件对象
     * @return 异步任务
     */
    CompletableFuture<Void> asyncUploadFile(String bucketName, String fileKey, File file);

    /**
     * 异步上传文件
     *
     * @param bucketName  OSS存储桶名称
     * @param fileKey     OSS文件Key
     * @param inputStream 上传流
     * @return 异步任务
     */
    CompletableFuture<Void> asyncUploadFile(String bucketName, String fileKey, InputStream inputStream);

    /**
     * 删除文件
     *
     * @param fileKey OSS文件Key
     */
    void deleteFile(String fileKey);

    /**
     * 删除文件
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    OSS文件Key
     */
    void deleteFile(String bucketName, String fileKey);

    /**
     * 获取文件列表
     *
     * @return 文件Key列表
     */
    List<String> listFileKeys();

    /**
     * 获取文件列表
     *
     * @param bucketName OSS存储桶名称
     * @return 文件Key列表
     */
    List<String> listFileKeys(String bucketName);

    /**
     * 使用Key前缀获取文件列表
     *
     * @param prefix 文件前缀
     * @return 文件Key列表
     */
    List<String> listFileKeysByPrefix(String prefix);

    /**
     * 使用Key前缀获取文件列表
     *
     * @param bucketName OSS存储桶名称
     * @param prefix     文件Key列表
     * @return
     */
    List<String> listFileKeysByPrefix(String bucketName, String prefix);

    /**
     * 获取文件元数据
     *
     * @param fileKey 文件Key
     * @return 文件元数据对象
     */
    OssFileMetadata getFileMetaData(String fileKey);

    /**
     * 获取文件元数据
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    文件Key
     * @return 文件元数据对象
     */
    OssFileMetadata getFileMetaData(String bucketName, String fileKey);

    /**
     * 生成文件预签名URL
     *
     * @param fileKey    OSS文件Key
     * @param expiration 过期时间
     * @return 预签名URL
     */
    URL generatePresignedUrl(String fileKey, Date expiration);

    /**
     * 生成文件预签名URL
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    OSS文件Key
     * @param expiration 过期时间
     * @return 预签名URL
     */
    URL generatePresignedUrl(String bucketName, String fileKey, Date expiration);

    /**
     * 获取文件字节数组
     *
     * @param fileKey OSS文件Key
     * @return 文件字节数组
     */
    byte[] getFileBytes(String fileKey);

    /**
     * 获取文件字节数组
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    OSS文件Key
     * @return 文件字节数组
     */
    byte[] getFileBytes(String bucketName, String fileKey);

    /**
     * 获取文件Base64值
     *
     * @param fileKey OSS文件Key
     * @return Base64值
     */
    String getFileBase64(String fileKey);

    /**
     * 获取文件Base64值
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    OSS文件Key
     * @return Base64值
     */
    String getFileBase64(String bucketName, String fileKey);

    /**
     * 获取文件流
     *
     * @param fileKey OSS文件Key
     * @return 文件流
     */
    InputStream getFileStream(String fileKey);

    /**
     * 获取文件流
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    OSS文件Key
     * @return 文件流
     */
    InputStream getFileStream(String bucketName, String fileKey);

    /**
     * 获取Range文件流
     *
     * @param fileKey OSS文件Key
     * @param begin   起始字节
     * @param end     结束字节
     * @return 文件流
     */
    InputStream getFileStreamByRange(String fileKey, long begin, long end);

    /**
     * 获取Range文件流
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    OSS文件Key
     * @param begin      起始字节
     * @param end        结束字节
     * @return 文件流
     */
    InputStream getFileStreamByRange(String bucketName, String fileKey, long begin, long end);

    /**
     * 拷贝文件
     *
     * @param sourceBucketName      源存储桶名称
     * @param sourceKey             源文件Key
     * @param destinationBucketName 目标存储桶名称
     * @param destinationKey        目标文件Key
     */
    void copyFile(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey);

    /**
     * 判断文件是否存在
     *
     * @param fileKey 文件Key
     * @return 结果
     */
    boolean doesFileExist(String fileKey);

    /**
     * 判断文件是否存在
     *
     * @param bucketName OSS存储桶名称
     * @param fileKey    文件Key
     * @return 结果
     */
    boolean doesFileExist(String bucketName, String fileKey);
}
