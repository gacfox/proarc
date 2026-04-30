package com.gacfox.proarc.agentic.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 参数属性
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Property implements Serializable {
    /**
     * 参数类型
     */
    @JsonProperty("type")
    private String type;
    /**
     * 参数描述
     */
    @JsonProperty("description")
    private String description;
}
