package ai.mcp.server.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DbLookupTool {

    private static final Logger log = LoggerFactory.getLogger(DbLookupTool.class);
    private final JdbcTemplate jdbc;

    public DbLookupTool(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Tool(name = "lookup_products", description = """
            Searches the product database. Use when the task involves finding products,
            checking prices, or listing items by category.
            Pass category to filter by category (e.g. Electronics, Furniture).
            Pass name to search by partial product name.
            Leave both null to get all products.
            """)
    public String lookupProducts(
            @ToolParam(description = "Product category to filter by e.g. Electronics, Furniture. Optional.", required = false) String category,
            @ToolParam(description = "Partial product name to search by. Optional.", required = false) String name) {

        log.debug("db_lookup invoked — category={}, name={}", category, name);

        List<Map<String, Object>> rows;

        if (category != null && !category.isBlank()) {
            rows = jdbc.queryForList(
                    "SELECT id, name, price, category FROM products WHERE category ILIKE ?",
                    "%" + category + "%");
        } else if (name != null && !name.isBlank()) {
            rows = jdbc.queryForList(
                    "SELECT id, name, price, category FROM products WHERE name ILIKE ?",
                    "%" + name + "%");
        } else {
            rows = jdbc.queryForList(
                    "SELECT id, name, price, category FROM products ORDER BY category, name");
        }

        if (rows.isEmpty()) return "No products found.";

        return rows.stream()
                .map(r -> String.format("[%s] %s — ₹%.2f (%s)",
                        r.get("id"), r.get("name"),
                        r.get("price"), r.get("category")))
                .collect(Collectors.joining("\n"));
    }
}