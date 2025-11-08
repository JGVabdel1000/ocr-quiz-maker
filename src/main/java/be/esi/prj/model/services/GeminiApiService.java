package be.esi.prj.model.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

public class GeminiApiService {

    /**
     * Constructeur privé pour empêcher l'instanciation de ce service.
     * Ce service expose uniquement des méthodes statiques et ne doit pas être instancié.
     */
    private GeminiApiService() {
        throw new UnsupportedOperationException("This service shouldn't be instantiated");
    }

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";

    private static final String API_KEY;
    static HttpClient HTTP_CLIENT;

    static {
        Dotenv dotenv = Dotenv.load();
        API_KEY = dotenv.get("GEMINI_API_KEY");
        HTTP_CLIENT = HttpClient.newHttpClient();
    }

    public static void setHttpClientForTests(HttpClient client) {
        HTTP_CLIENT = client;
    }

    /**
     * Generates a quiz question and answer from a given text input using Gemini API.
     *
     * @param inputText the user text from which to generate a quiz
     * @return list containing [question, answer] or [null, null] if failed
     */
    public static List<String> generateQuizFromText(String inputText) {
        String requestBody = buildPrompt(inputText);
        String url = BASE_URL + "?key=" + API_KEY;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseQuizFromResponse(response.body());
            } else {
                System.err.printf("HTTP %d Error: %s%n", response.statusCode(), response.body());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        return Arrays.asList(null, null);
    }

    /**
     * Builds the JSON prompt to send to the Gemini API.
     */
    private static String buildPrompt(String inputText) {
        String escapedText = inputText.replace("\"", "\\\"");
        return String.format("""
                {
                  "contents": [{
                    "parts": [{
                      "text": "Génère une seule question de quiz en français à partir du texte suivant. \
                        La réponse doit être au format JSON avec les champs 'question' et 'answer'. \
                        La question doit tester les connaissances contenues dans ce texte utilisateur. Texte = %s"
                    }]
                  }]
                }
                """, escapedText);
    }

    /**
     * Parses the Gemini API JSON response to extract the quiz question and answer.
     */
    private static List<String> parseQuizFromResponse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();

            // New direct format: {"question":"...","answer":"..."}
            if (root.has("question")) {
                String q = root.get("question").isJsonNull() ? null : root.get("question").getAsString();
                String a = root.has("answer") && !root.get("answer").isJsonNull()
                        ? root.get("answer").getAsString()
                        : null;
                return Arrays.asList(q, a);
            }


            String rawText = root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            String cleanedJson = rawText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonObject quizJson = JsonParser.parseString(cleanedJson).getAsJsonObject();

            String question = quizJson.has("question") && !quizJson.get("question").isJsonNull()
                    ? quizJson.get("question").getAsString()
                    : null;

            String answer = quizJson.has("answer") && !quizJson.get("answer").isJsonNull()
                    ? quizJson.get("answer").getAsString()
                    : null;

            return Arrays.asList(question, answer); // <- accepte nulls

        } catch (Exception e) {
            System.err.println("Failed to parse Gemini response: " + e.getMessage());
            return Arrays.asList(null, null);
        }
    }


}