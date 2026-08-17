package co.za.wethinkcode.trafficflowf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngestionServiceTest {

    @Test
    @DisplayName("GET /intersections")
    public void shouldGetHelloWorld() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpResponse<String> response = Unirest.get("http://localhost:7020/intersections").asString();
        assertEquals(200, response.getStatus());
        ArrayNode intersections = objectMapper.readValue(response.getBody(), ArrayNode.class);

        for (JsonNode intersection : intersections) {
            assertTrue(intersection.asText().contains("intersection_id"));
            assertTrue(intersection.asText().contains("district"));
            assertTrue(intersection.asText().contains("active_flag"));
            assertTrue(intersection.asText().contains("signal_type"));
        }

    }
}
