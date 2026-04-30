package com.gacfox.proarc.oss;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(OssConfigure.class)
public class OssAutoConfiguration {
    static class RegisterCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String providerConfig = context.getEnvironment().getProperty("proarc.oss.provider");
            return StringUtils.hasText(providerConfig);
        }
    }

    @Bean
    @Conditional(RegisterCondition.class)
    public OssTemplate ossTemplate(OssConfigure ossConfigure) {
        if ("s3".equals(ossConfigure.getProvider())) {
            return new S3OssTemplate(ossConfigure);
        } else {
            throw new RuntimeException("OssTemplate provider not implemented!");
        }
    }
}
