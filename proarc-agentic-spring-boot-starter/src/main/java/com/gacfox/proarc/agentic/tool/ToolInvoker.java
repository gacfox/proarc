package com.gacfox.proarc.agentic.tool;

/**
 * 智能体工具执行入口
 */
@FunctionalInterface
public interface ToolInvoker {
    /**
     * 执行工具调用
     *
     * @param arguments 工具入参的JSON字符串，无参工具为 null 或空串
     * @return 工具执行结果
     * @throws Exception 工具执行异常
     */
    String invoke(String arguments) throws Exception;
}
