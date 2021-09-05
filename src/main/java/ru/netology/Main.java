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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main {
    public static final String REMOTE_SERVICE_URI = "https://api.nasa.gov/planetary/apod?api_key=MyKeyWasHere";

    public static void main(String[] args) {
        try (final CloseableHttpClient httpClient = createHttpClient()) {
            final HttpGet request = new HttpGet(REMOTE_SERVICE_URI);
            try (final CloseableHttpResponse response = httpClient.execute(request)) {
                final String url = getFileUrl(response);
                downloadFile(httpClient, url);
            } catch (ParseException | URISyntaxException e) {
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

    private static String getFileUrl(CloseableHttpResponse response) throws IOException, ParseException, URISyntaxException {
        final JSONParser parser = new JSONParser();

        final String body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        final JSONObject obj = (JSONObject) parser.parse(body);
        final String url = (String) obj.get("url");
        if ("video".equals(obj.get("media_type"))) {
            return getYoutubePictureUrl(url);
        }

        return url;
    }

    private static String getYoutubePictureUrl(String urlToVideo) throws URISyntaxException {
        final URI uri = new URI(urlToVideo);
        final String[] uriParts = uri.getPath().split("/");
        final String pathForPicture = uriParts[uriParts.length - 1];

        return "https://img.youtube.com/vi/" + pathForPicture + "/hqdefault.jpg";
    }

    private static void downloadFile(CloseableHttpClient httpClient, String url) throws IOException, ParseException {
        final String pathToFile = getPathToFile(url);

        final HttpGet requestFile = new HttpGet(url);
        try (final CloseableHttpResponse responseFile = httpClient.execute(requestFile)) {
            Files.copy(responseFile.getEntity().getContent(), new File(pathToFile).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String getPathToFile(String url) {
        final String[] urlParts = url.split("/");
        final String fileName = urlParts[urlParts.length - 1];
        return "directoryForNasaFiles" + File.separator + fileName;
    }
}
