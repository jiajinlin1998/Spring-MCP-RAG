package com.jjl.mcpclient.config;

import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallbackConfig {

    @Bean
    public AsyncMcpToolCallbackProvider toolCallbackProvider() {
        return new AsyncMcpToolCallbackProvider();
    }
}
