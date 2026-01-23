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

public class UpdateManager {
	public static final String GITHUB_REPO_URI = "https://api.github.com/repos/Ramazanenescik04/Capsule/releases/latest";
	public static final String DE_GITHUB_REPO_URI = "https://api.github.com/repos/Ramazanenescik04/DikenEngine/releases/latest";
	public static UpdateManager instance = new UpdateManager();
	
	private Version repoVersion = VersionChecker.clientVersion, dikenEngine_repoVerison = VersionChecker.dikenVersion;
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
	
	public void downloadCapsuleAndLibs() {
		downloadFileURIs = new ArrayList<>();
		libs = new ArrayList<>();
		
		getRepoVersionAndDownloadURL();
		getDikenEngine();
	}
	
	private void getDikenEngine() {
		try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DE_GITHUB_REPO_URI))
                    .header("Accept", "application/vnd.github+json")
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
	
	private void getRepoVersionAndDownloadURL() {
		try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_REPO_URI))
                    .header("Accept", "application/vnd.github+json")
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
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
