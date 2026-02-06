package net.capsule.update.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;

import net.capsule.Version;
import net.capsule.update.UpdateFrame;

public class UpdateManager {

    // Singleton instance
    public static final UpdateManager instance = new UpdateManager();

    // HttpClient ağır bir nesnedir, tek bir instance olarak tutulmalı ve tekrar kullanılmalıdır.
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .executor(Executors.newVirtualThreadPerTaskExecutor()) // Java 21+ Virtual Threads
            .build();

    private static final String API_BASE_URL = "http://capsule.net.tr/api/v1/assets/check_update.php?name=";
    private static final String USER_AGENT = "Capsule-Launcher/" + UpdateFrame.capsuleLauncherVersion;

    private Version repoVersion = VersionChecker.clientVersion;
    private Version dikenEngineRepoVersion = VersionChecker.dikenVersion;
    private Version clVersion = UpdateFrame.capsuleLauncherVersion;

    // URI ve File çiftini tutmak için Java Record kullanıyoruz (Daha temiz veri yapısı)
    private final List<DownloadTask> downloadTasks = new CopyOnWriteArrayList<>();

    // Veri taşıyıcı record
    private record DownloadTask(URI uri, File destination) {}

    public void installAndRunUpdate(Consumer<DownloadProgress> progressConsumer, Consumer<UpdateException> crash) {
        if (updateIsAvailable()) {
            // İndirme işlemini Virtual Thread üzerinde başlat
            Thread.startVirtualThread(() -> {
                try {
                    int totalFiles = downloadTasks.size();
                    for (int i = 0; i < totalFiles; i++) {
                        DownloadTask task = downloadTasks.get(i);
                        // Util.downloadFile metodunun imzasını değiştirmedim, mevcut yapına uyduruyorum
                        Util.downloadFile(task.uri, task.destination, progressConsumer);
                    }

                    // Versiyonları güncelle
                    VersionChecker.clientVersion = repoVersion;
                    VersionChecker.dikenVersion = dikenEngineRepoVersion;

                    if (progressConsumer != null) {
                        progressConsumer.accept(new DownloadProgress("Starting", 100, 0, true));
                    }
                } catch (IOException | InterruptedException e) {
                    if (crash != null) {
                        crash.accept(new UpdateException(e.getMessage(), e));
                    } else {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            if (progressConsumer != null) {
                progressConsumer.accept(new DownloadProgress("No Update - Starting", 100, 0, true));
            }
        }
    }

    public List<File> getCapsuleLibs() {
        return downloadTasks.stream().map(DownloadTask::destination).toList();
    }

    public boolean updateIsAvailable() {
        return repoVersion.compareTo(VersionChecker.clientVersion) > 0 
            || dikenEngineRepoVersion.compareTo(VersionChecker.dikenVersion) > 0;
    }

    public boolean capsuleLauncherUpdateIsAvailable() {
        return clVersion.compareTo(UpdateFrame.capsuleLauncherVersion) > 0;
    }

    /**
     * Tüm versiyon kontrollerini PARALEL olarak yapar.
     */
    public void downloadCapsuleAndLibs() {
        downloadTasks.clear();

        // CompletableFuture ile asenkron ve paralel istekler (Virtual Thread Executor kullanarak)
        var launcherFuture = CompletableFuture.runAsync(this::checkCapsuleLauncherVersion);
        var engineFuture = CompletableFuture.runAsync(this::checkDikenEngineVersion);
        var capsuleFuture = CompletableFuture.runAsync(this::checkCapsuleVersion);

        // Tüm isteklerin bitmesini bekle
        CompletableFuture.allOf(launcherFuture, engineFuture, capsuleFuture).join();
    }

    // --- Private Helper Methods ---

    /**
     * Tekrarlanan HTTP isteği ve JSON parse mantığını tek metoda indirdik.
     */
    private JSONObject fetchUpdateData(String componentName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + componentName))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", USER_AGENT)
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return new JSONObject(response.body());
        } else {
            System.err.println("API Error for " + componentName + ": " + response.statusCode());
            return null;
        }
    }

    private void checkCapsuleLauncherVersion() {
        try {
            JSONObject json = fetchUpdateData("launcher");
            if (json != null) {
                this.clVersion = new Version(json.getString("tag_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkDikenEngineVersion() {
        try {
            JSONObject json = fetchUpdateData("engine");
            if (json != null) {
                this.dikenEngineRepoVersion = new Version(json.getString("tag_name"));
                
                // Engine için tüm assetleri indir
                parseAndAddAssets(json.getJSONArray("assets"), null); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkCapsuleVersion() {
        try {
            JSONObject json = fetchUpdateData("capsule");
            if (json != null) {
                this.repoVersion = new Version(json.getString("tag_name"));
                
                // Sadece Capsule.jar'ı indir
                parseAndAddAssets(json.getJSONArray("assets"), "Capsule.jar");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * JSON Assets arrayini parse eder ve indirme listesine ekler.
     * @param filterName Eğer null değilse, sadece bu isme sahip dosyayı ekler.
     */
    private void parseAndAddAssets(JSONArray assets, String filterName) {
        String baseDir = Util.getDirectory() + "jars/";
        long timestamp = System.currentTimeMillis();

        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String fileName = asset.getString("name");
            String downloadUrl = asset.getString("browser_download_url");

            if (filterName == null || fileName.equals(filterName)) {
                File destFile = new File(baseDir + fileName);
                // Cache busting için timestamp ekliyoruz
                URI uri = URI.create(downloadUrl + "?t=" + timestamp);
                
                downloadTasks.add(new DownloadTask(uri, destFile));
            }
        }
    }

	public String getLatestLauncherVersion() {
		return this.clVersion.toString();
	}
}