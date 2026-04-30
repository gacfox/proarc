package com.gacfox.proarc.configmap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

@Configuration
public class ConfigMapWatchAutoConfiguration {
    @Bean(initMethod = "init")
    public ConfigMapWatch configMapWatch(ConfigurableEnvironment configurableEnvironment, @Qualifier("configDataContextRefresher") ContextRefresher contextRefresher) {
        return new ConfigMapWatch(configurableEnvironment, contextRefresher);
    }
}
