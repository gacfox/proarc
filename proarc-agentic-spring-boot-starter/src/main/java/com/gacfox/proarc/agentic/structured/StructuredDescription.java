package com.gacfox.proarc.agentic.structured;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 结构化输出描述
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StructuredDescription {
    /**
     * 描述内容
     *
     * @return 描述内容
     */
    String value();
}
