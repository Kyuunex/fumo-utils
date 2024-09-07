package moe.kyuunex.fumo_utils.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import moe.kyuunex.fumo_utils.FumoUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Webhook {
    public static void sendAsync(String webhookUrl, String username, String message) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("content", message);

            Gson gson = new Gson();
            String requestBody = gson.toJson(json);

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            FumoUtils.LOG.info(e.toString());
        }
    }
}
