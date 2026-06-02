package com.gacfox.proarc.agentic.structured;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 结构化输出字段名
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StructuredName {
    /**
     * 字段名
     *
     * @return 字段名
     */
    String value();
}
