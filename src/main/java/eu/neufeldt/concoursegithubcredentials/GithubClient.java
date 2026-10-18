package eu.neufeldt.concoursegithubcredentials;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class GithubClient {
    private final String baseUrl;
    private final HttpClient client;
    private final HttpRequest.Builder baseRequest;
    private static final int MAX_RETRIES = 3;
    private static final long MAX_RETRY_DELAY_MS = 60_000L;

    public GithubClient(String baseUrl, int appId, String privateKey, Instant now) throws GeneralSecurityException {
        this.baseUrl = baseUrl;
        PrivateKey pkcs1PrivateKey = EncryptionUtils.loadPkcs1PrivateKey(privateKey);
        Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) pkcs1PrivateKey);
        String token = JWT.create()
                .withIssuer(String.valueOf(appId))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(600)))
                .sign(algorithm);
        client = HttpClient.newHttpClient();
        baseRequest = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.machine-man-preview+json");
    }

    public <T> T call(String method, String path, Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest request = baseRequest.method(method, noBody()).uri(URI.create(baseUrl + path)).build();
        return send(request, path, clazz);
    }

    public <T> T call(String method, String path, String jsonBody, Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest request = baseRequest
                .method(method, ofString(jsonBody))
                .header("Content-Type", "application/json")
                .uri(URI.create(baseUrl + path)).build();
        return send(request, path, clazz);
    }

    // GitHub documents 429 and 5xx as transient failures worth retrying; other non-2xx are permanent.
    private <T> T send(HttpRequest request, String path, Class<T> clazz) throws IOException, InterruptedException {
        var attempt = 0;
        while (true) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            var status = response.statusCode();
            var retryable = status == 429 || status >= 500;
            if (!retryable || attempt == MAX_RETRIES) {
                if (status >= 200 && status <= 299) {
                    return new Gson().fromJson(response.body(), clazz);
                }
                throw new RuntimeException(status + " when calling " + baseUrl + path);
            }
            Thread.sleep(retryDelayMillis(response, attempt));
            attempt++;
        }
    }

    private static long retryDelayMillis(HttpResponse<String> response, int attempt) {
        var retryAfterSeconds = response.headers().firstValueAsLong("Retry-After");
        if (retryAfterSeconds.isPresent() && retryAfterSeconds.getAsLong() >= 0) {
            return Math.min(MAX_RETRY_DELAY_MS, 1000L * retryAfterSeconds.getAsLong());
        }
        return Math.min(MAX_RETRY_DELAY_MS, 1000L << attempt);
    }
}
