package co.wethinkcode.trafficflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.javalin.Javalin;
import org.apache.commons.text.WordUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class IngestionServiceApp {

    private static ArrayNode available_data = null;

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7020);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/intersections-legacy.csv (intersections, districts, signal types data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
        try {
            System.out.println(readAndCleanCSV().toPrettyString());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static ArrayNode readAndCleanCSV() throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode hubData = mapper.createArrayNode();
        available_data = mapper.createArrayNode();
        String text;
        String[] rows;

        try (InputStream is = IngestionServiceApp.class.getClassLoader().getResourceAsStream("intersections-legacy.csv")) {
            if (is == null) {throw new IllegalArgumentException("File not found!");}
            text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        rows = text.split("\n");
        String[] row = rows[0].split(",");

        for (int i = 1; i < rows.length; i++) {
            Map<String, String> data = new HashMap<>();

            for (int j = 0; j < row.length; j++) {
                String key = formatText(row[j]).toLowerCase();
                String value = formatText(rows[i].split(",")[j]);
                data.put(key, value);
            }
            String jsonString = mapper.writeValueAsString(data);
            if (!checkForDuplicates(data)) {
                hubData.add(jsonString);
                available_data = hubData;
            }
        }

        return hubData;
    }

    private static String formatText(String text) {
        text =  text.replace("\n", "").replace("\r", "");

        try {
            String[] words = text.split(" ");
            text = "";
            for (String word : words) {
                if (!word.isEmpty()) {
                    word = word.trim();
                    word = word.toLowerCase();
                    word = WordUtils.capitalize(word);

                    text += word + " ";
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        text = text.trim();

        switch (text.toLowerCase()) {
            case "true", "1", "yes" -> text = "Y";
            case "false", "0", "no" -> text = "N";
            case "unknown", "", "null", "-" -> text = "N/A";
        }

        return text;
    }

    private static boolean checkForDuplicates(Map<String, String> map) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        for (int i = 0; i < available_data.size(); i++) {
            JsonNode intersection = mapper.readTree(available_data.get(i).asText());
            if (intersection.get("intersection_id").asText().equals(map.get("intersection_id"))) {
                return true;
            }
        }
        return false;
    }

}
