package ai.mcp.server.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class RagLookupTool {

    private static final Logger log = LoggerFactory.getLogger(RagLookupTool.class);
    private static final String RAG_URL = "http://localhost:8081/rag/ask";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Tool(name = "rag_lookup", description = """
            Search the internal knowledge base for context on a topic.
            Use when the question requires domain-specific or internal knowledge.
            Be descriptive — RAG uses semantic search, more context yields better results.
            """)
    public String ragLookup(
            @ToolParam(description = "The search query string. Be descriptive for better semantic match. Required.") String query) {

        log.debug("rag_lookup invoked — query={}", query);

        if (query == null || query.isBlank()) {
            return "ERROR: query is required for rag_lookup.";
        }

        String enrichedQuery = query + " and be concise no long answers";
        String body = "{\"query\":\"" + enrichedQuery.replace("\"", "\\\"") + "\"}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RAG_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "ERROR: RAG service returned status " + response.statusCode();
            }

            log.debug("RAG result for '{}': {}", query, response.body());
            return response.body().trim();

        } catch (Exception e) {
            log.error("rag_lookup failed for query='{}': {}", query, e.getMessage());
            return "ERROR: RAG service unreachable — " + e.getMessage();
        }
    }
}