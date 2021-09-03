package ru.netology;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {
    public static final String REMOTE_SERVICE_URI = "https://api.nasa.gov/planetary/apod?api_key=MyKeyWasHere";

    public static void main(String[] args) {
        try (final CloseableHttpClient httpClient = createHttpClient()) {
            final HttpGet requestUrl = new HttpGet(REMOTE_SERVICE_URI);
            try (final CloseableHttpResponse responseUrl = httpClient.execute(requestUrl)) {
                downloadFile(httpClient, responseUrl);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(5_000)    // максимальное время ожидание подключения к серверу
                        .setSocketTimeout(30_000)    // максимальное время ожидания получения данных
                        .setRedirectsEnabled(false) // возможность следовать редиректу в ответе
                        .build())
                .build();
    }

    private static void downloadFile(CloseableHttpClient httpClient, CloseableHttpResponse responseUrl) throws IOException, ParseException {
        final String url = getFileUrl(responseUrl);
        final String[] urlParts = url.split("/");
        final String fileName = urlParts[urlParts.length - 1];

        final HttpGet requestFile = new HttpGet(url);
        try (final CloseableHttpResponse responseFile = httpClient.execute(requestFile)) {
            final String pathToFile = "directoryForNasaFiles" + File.separator + fileName;
            Files.copy(responseFile.getEntity().getContent(), new File(pathToFile).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String getFileUrl(CloseableHttpResponse response) throws IOException, ParseException {
        final JSONParser parser = new JSONParser();

        final String body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        final JSONObject obj = (JSONObject) parser.parse(body);
        return (String) obj.get("url");
    }
}
