package be.esi.prj.model.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.http.*;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.*;

public class GeminiApiServiceTest {

    private static HttpClient mockClient;
    private static HttpResponse<String> mockResponse;

    @BeforeAll
    static void beforeAll() {
        // 1) Crée le mock
        mockClient   = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);

        // 2) Injecte-le dans la classe sous test
        GeminiApiService.setHttpClientForTests(mockClient);
    }

    @AfterAll
    static void afterAll() {
        // Restaure un client « normal » pour ne pas polluer d'autres tests
        GeminiApiService.setHttpClientForTests(HttpClient.newHttpClient());
    }

    @Test
    public void testGenerateQuizFromText_success() throws Exception {
        // 200 + JSON direct {question,answer}
        String body = "{\"question\":\"Q?\",\"answer\":\"A.\"}";
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(body);
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        List<String> result = GeminiApiService.generateQuizFromText("unused");
        assertEquals(2, result.size());
        assertEquals("Q?", result.get(0));
        assertEquals("A.", result.get(1));
    }

    @Test
    public void testGenerateQuizFromText_malformedJsonReturnsNulls() throws Exception {
        // status 200 mais body pas JSON
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("pas du JSON");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        List<String> quiz = GeminiApiService.generateQuizFromText("unused");
        assertEquals(Arrays.asList(null, null), quiz);
    }

    @Test
    public void testGenerateQuizFromText_httpErrorReturnsNulls() throws Exception {
        // status != 200
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("Server Error");
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        List<String> quiz = GeminiApiService.generateQuizFromText("unused");
        assertEquals(Arrays.asList(null, null), quiz);
    }

    @Test
    public void testGenerateQuizFromText_ioExceptionReturnsNulls() throws Exception {
        // IOException à l’envoi
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("boom"));

        List<String> quiz = GeminiApiService.generateQuizFromText("unused");
        assertEquals(Arrays.asList(null, null), quiz);
    }
}
