package com.gacfox.proarc.configmap;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 配置监听刷新
 */
@Slf4j
public class ConfigMapWatch {
    private final ConfigurableEnvironment configurableEnvironment;
    private final ContextRefresher contextRefresher;

    public ConfigMapWatch(ConfigurableEnvironment configurableEnvironment, ContextRefresher contextRefresher) {
        this.configurableEnvironment = configurableEnvironment;
        this.contextRefresher = contextRefresher;
    }

    public void init() {
        String configPropertyStr = System.getProperty("spring.config.location");
        if (configPropertyStr != null) {
            String[] configProperties = configPropertyStr.split(",");
            List<FileAlterationObserver> observers = new ArrayList<>();
            for (String configPropertyPath : configProperties) {
                File configFile = new File(configPropertyPath);
                if ((checkPropertiesExt(configPropertyPath) || checkYamlExt(configPropertyPath)) &&
                        configFile.exists() &&
                        configFile.getParentFile().exists()) {
                    FileAlterationObserver fileAlterationObserver;
                    try {
                        fileAlterationObserver = FileAlterationObserver.builder()
                                .setFile(configFile.getParentFile())
                                .setFileFilter(pathname -> true)
                                .get();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    fileAlterationObserver.addListener(new FileAlterationListenerAdaptor() {
                        @Override
                        public void onFileChange(File file) {
                            // 更新ConfigurableEnvironment
                            log.info("Detect config file change on {}", configPropertyPath);
                            String key = null;
                            PropertySources propertySources = configurableEnvironment.getPropertySources();
                            MapPropertySource propertySource = null;
                            for (PropertySource<?> m : propertySources) {
                                if (m.getName().endsWith("'" + configPropertyPath + "'")) {
                                    key = m.getName();
                                    propertySource = (MapPropertySource) m;
                                    break;
                                }
                            }
                            if (propertySource != null) {
                                Map<String, Object> source = propertySource.getSource();
                                Map<String, Object> target = new HashMap<>(source.size());
                                target.putAll(source);
                                Path filePath = Paths.get(configPropertyPath);
                                if (checkPropertiesExt(configPropertyPath)) {
                                    // 加载properties配置
                                    PropertiesPropertySourceLoader propertiesPropertySourceLoader = new PropertiesPropertySourceLoader();
                                    try {
                                        List<PropertySource<?>> loadedPropertySourceList =
                                                propertiesPropertySourceLoader.load(key, new FileSystemResource(filePath));
                                        for (PropertySource<?> ps : loadedPropertySourceList) {
                                            @SuppressWarnings("unchecked") Map<? extends String, ?> map = (Map<? extends String, ?>) ps.getSource();
                                            target.putAll(map);
                                        }
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else if (checkYamlExt(configPropertyPath)) {
                                    // 加载YAML配置
                                    YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
                                    try {
                                        List<PropertySource<?>> loadedPropertySourceList =
                                                yamlPropertySourceLoader.load(key, new FileSystemResource(filePath));
                                        for (PropertySource<?> ps : loadedPropertySourceList) {
                                            @SuppressWarnings("unchecked") Map<? extends String, ?> map = (Map<? extends String, ?>) ps.getSource();
                                            target.putAll(map);
                                        }
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                configurableEnvironment.getPropertySources().replace(key, new MapPropertySource(key, target));
                            }

                            // 通知Scope上下文重新加载配置
                            contextRefresher.refresh();
                        }
                    });
                    observers.add(fileAlterationObserver);
                    log.info("Config file monitoring on {}", configPropertyPath);
                }
            }
            FileAlterationMonitor fileAlterationMonitor = new FileAlterationMonitor(1000L);
            for (FileAlterationObserver observer : observers) {
                fileAlterationMonitor.addObserver(observer);
            }
            try {
                fileAlterationMonitor.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean checkPropertiesExt(String filename) {
        return StringUtils.hasText(filename) && filename.endsWith(".properties");
    }

    private boolean checkYamlExt(String filename) {
        return StringUtils.hasText(filename) && (filename.endsWith(".yaml") || filename.endsWith(".yml"));
    }
}
