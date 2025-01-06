package org.example.modrinth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModrinthDownloader {

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    /**
     * Sucht Mods per Modrinth-API, optional mit Facets.
     */
    public static JsonArray searchMods(String query, String facets) throws IOException, InterruptedException {
        String baseUrl = "https://api.modrinth.com/v2/search?limit=10";
        String fullUrl = baseUrl + "&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        if (!"[]".equals(facets)) {
            String encodedFacets = URLEncoder.encode(facets, StandardCharsets.UTF_8);
            fullUrl += "&facets=" + encodedFacets;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(fullUrl))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), JsonObject.class).getAsJsonArray("hits");
        } else {
            throw new IOException("Error: (HTTP " + response.statusCode() + ")");
        }
    }

    /**
     * Ruft Details zu einer Mod (z. B. “description”, “icon_url” usw.) ab.
     */
    public static JsonObject getProjectDetails(String projectId) throws IOException, InterruptedException {
        String url = "https://api.modrinth.com/v2/project/" + projectId;
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), JsonObject.class);
        } else {
            throw new IOException("details-error: (HTTP " + response.statusCode() + ")");
        }
    }

    /**
     * Ruft die Versionen eines Projekts ab.
     */
    public static JsonArray getProjectVersions(String projectId) throws IOException, InterruptedException {
        String url = "https://api.modrinth.com/v2/project/" + projectId + "/version";
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), JsonArray.class);
        } else {
            throw new IOException("version-error (HTTP " + response.statusCode() + ")");
        }
    }

    /**
     * Lädt die erste Datei einer Version herunter und speichert sie im angegebenen Ordner.
     */
    public static void downloadVersion(JsonObject versionData, String modsFolderPath)
            throws IOException, InterruptedException {
        if (!versionData.has("files")) {
            System.out.println("Keine Files in dieser Version.");
            return;
        }
        var files = versionData.getAsJsonArray("files");
        if (files.size() == 0) {
            System.out.println("No files found.");
            return;
        }
        var fileObj = files.get(0).getAsJsonObject();
        var downloadUrl = fileObj.get("url").getAsString();
        var filename = fileObj.get("filename").getAsString();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(downloadUrl))
                .build();

        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 200) {
            Path savePath = Paths.get(modsFolderPath, filename);
            try (var is = response.body(); var fos = new FileOutputStream(savePath.toFile())) {
                is.transferTo(fos);
            }
            System.out.println("Downloaded: " + filename);
        } else {
            throw new IOException("Download-error (HTTP " + response.statusCode() + ")");
        }
    }
}
