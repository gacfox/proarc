package com.gacfox.proarc.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSS配置对象
 */
@Data
@ConfigurationProperties(prefix = "proarc.oss")
public class OssConfigure {
    private String provider;
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String defaultBucketName;
    private boolean pathStyleAccessEnabled = true;
}
