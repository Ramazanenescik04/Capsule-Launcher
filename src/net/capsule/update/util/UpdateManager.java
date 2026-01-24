package net.capsule.update.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;

import net.capsule.Version;
import net.capsule.update.UpdateFrame;

public class UpdateManager {
	public static UpdateManager instance = new UpdateManager();
	
	private Version repoVersion = VersionChecker.clientVersion, dikenEngine_repoVerison = VersionChecker.dikenVersion, clVersion = UpdateFrame.capsuleLauncherVersion;
	private List<URI> downloadFileURIs = new ArrayList<>();
	private List<File> libs = new ArrayList<>();
	
	public void installAndRunUpdate(Consumer<DownloadProgress> progressConsumer, Consumer<UpdateException> crash) {
		if (updateIsAvailable()) {
			Thread.startVirtualThread(() -> {
				try {
					for (int i = 0; i < downloadFileURIs.size(); i++) {
						Util.downloadFile(downloadFileURIs.get(i), libs.get(i), progressConsumer);
					}
					
					VersionChecker.clientVersion = repoVersion;
					VersionChecker.dikenVersion = dikenEngine_repoVerison;
					
					if (progressConsumer != null) {
						progressConsumer.accept(new DownloadProgress("Starting", 100, 0, true));
					}
				} catch (IOException | InterruptedException e) {
					if (crash == null) {
						e.printStackTrace();
						return;
					}
					
					crash.accept(new UpdateException(e.getMessage(), e));
				}
			});
		} else {
			if (progressConsumer != null) {
				progressConsumer.accept(new DownloadProgress("No Update - Starting", 100, 0, true));
			}
		}
	}
	
	public List<File> getCapsuleLibs() {
		return new ArrayList<File>(libs);
	}
	
	public boolean updateIsAvailable() {
	    return repoVersion.compareTo(VersionChecker.clientVersion) > 0 || dikenEngine_repoVerison.compareTo(VersionChecker.dikenVersion) > 0;
	}
	
	public boolean capsuleLauncherUpdateIsAvailable() {
		return clVersion.compareTo(UpdateFrame.capsuleLauncherVersion) > 0;
	}
	
	public void downloadCapsuleAndLibs() {
		downloadFileURIs = new ArrayList<>();
		libs = new ArrayList<>();
		
		checkCapsuleVersion();
		checkDikenEngineVersion();
		checkCapsuleLauncherVersion();
	}
	
	private void checkCapsuleLauncherVersion() {
		try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://capsule.net.tr/api/v1/assets/check_update.php?name=launcher"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Capsule-UtilDownloadFile/" + UpdateFrame.capsuleLauncherVersion)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // JSON Ayrıştırma
                JSONObject jsonResponse = new JSONObject(response.body());

                // 1. Tag ismini al
                String tagName = jsonResponse.getString("tag_name");
                this.clVersion = new Version(tagName);
            } else {
                System.out.println("Hata: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void checkDikenEngineVersion() {
		try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://capsule.net.tr/api/v1/assets/check_update.php?name=engine"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Capsule-UtilDownloadFile/" + UpdateFrame.capsuleLauncherVersion)
                    // .header("Authorization", "Bearer YOUR_TOKEN") // Hız sınırı için gerekebilir
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // JSON Ayrıştırma
                JSONObject jsonResponse = new JSONObject(response.body());

                // 1. Tag ismini al
                String tagName = jsonResponse.getString("tag_name");
                this.dikenEngine_repoVerison = new Version(tagName);

                // 2. Dosyaları (Assets) listele
                JSONArray assets = jsonResponse.getJSONArray("assets");
                
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String fileName = asset.getString("name");
                    String downloadUrl = asset.getString("browser_download_url");                    
                    
                    this.libs.add(new File(Util.getDirectory() + "jars/" + fileName));
                    this.downloadFileURIs.add(URI.create(downloadUrl + "?t=" + System.currentTimeMillis()));
                }
            } else {
                System.out.println("Hata: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void checkCapsuleVersion() {
		try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://capsule.net.tr/api/v1/assets/check_update.php?name=capsule"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Capsule-UtilDownloadFile/" + UpdateFrame.capsuleLauncherVersion)
                    // .header("Authorization", "Bearer YOUR_TOKEN") // Hız sınırı için gerekebilir
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // JSON Ayrıştırma
                JSONObject jsonResponse = new JSONObject(response.body());

                // 1. Tag ismini al
                String tagName = jsonResponse.getString("tag_name");
                this.repoVersion = new Version(tagName);

                // 2. Dosyaları (Assets) listele
                JSONArray assets = jsonResponse.getJSONArray("assets");
                
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String fileName = asset.getString("name");
                    String downloadUrl = asset.getString("browser_download_url");
                    
                    if (fileName.equals("Capsule.jar")) {
                    	this.libs.add(new File(Util.getDirectory() + "jars/" + fileName));
                        downloadFileURIs.add(URI.create(downloadUrl + "?t=" + System.currentTimeMillis()));
                    }
                }
            } else {
                System.out.println("Hata: " + response.statusCode());
                System.out.println(response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
