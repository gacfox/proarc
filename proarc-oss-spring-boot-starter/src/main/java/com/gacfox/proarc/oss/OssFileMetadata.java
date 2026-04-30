package com.gacfox.proarc.oss;

import lombok.Data;

import java.util.Date;

/**
 * 文件信息
 */
@Data
public class OssFileMetadata {
    private String bucketName;
    private String key;
    private long size;
    private Date lastModified;
}
