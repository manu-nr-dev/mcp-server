package ai.mcp.server.config;

import ai.mcp.server.tools.DbLookupTool;
import ai.mcp.server.tools.RagLookupTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            RagLookupTool ragLookupTool,
            DbLookupTool dbLookupTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(ragLookupTool, dbLookupTool)
                .build();
    }
}
