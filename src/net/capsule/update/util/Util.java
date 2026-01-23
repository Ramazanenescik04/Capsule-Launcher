package net.capsule.update.util;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import net.capsule.update.SystemInfo;

public class Util {
   public static String linuxHomeDir;

   public static void findLinuxHomeDirectory() {
      String linux_home = System.getenv("HOME");
      if (linux_home == null) {
         String linux_user = System.getenv("USER");
         if (linux_user == "root") {
            linuxHomeDir = "/root";
         } else {
            linuxHomeDir = "/home/" + linux_user;
         }
      } else {
         linuxHomeDir = linux_home;
      }

   }

   public static String getWebData(URI url) {
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(url.toURL().openStream()));
         StringBuilder sb = new StringBuilder();
         String s = "";
         
         while((s = reader.readLine()) != null) {
			sb.append(s);
		 }
       
		 return sb.toString();
	  } catch (IOException var4) {
		  var4.printStackTrace();
		 return """
		 		{
		 			"status": "error",
		 			"message": "Cannot access the URL"
		 		}
		 		""";
	  }
   }

   // SAKIN BUNDAN ÖRNEK ALMA
   public static String getFileData(String path) {
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(Util.class.getResourceAsStream(path)));

         StringBuilder sb = new StringBuilder();
         String s = "";
         
         while((s = reader.readLine()) != null) {
			sb.append(s);
		 }

         return sb.toString();
      } catch (IOException var5) {
         var5.printStackTrace();
         return """
         		{
         			"status": "error",
		 			"message": "File not found"
		 		}
         		""";
      }
   }

   public static String getWebData(String string) {
      try {
		return getWebData(new URI(string));
	  } catch (URISyntaxException e) {
		return """
				{
					"status": "error",
					"message": """ + e.getMessage() + """
				}
				""";
	  }
   }

   private static String backslashes(String input) {
      return input.replaceAll("/", "\\\\");
   }

   public static String getConfigPath() {
      switch(SystemInfo.instance.getOS()) {
      case SystemInfo.OS.WINDOWS:
         return System.getProperty("user.home") + "/AppData/Roaming/.capsule/config.cfg".replaceAll("/", "\\\\");
      case SystemInfo.OS.MACOS:
         return String.format("~/Library/Application Support/capsule/config.cfg");
      case SystemInfo.OS.LINUX:
         return linuxHomeDir + "/.capsule/config.cfg";
      default:
         return System.getProperty("user.home") + "/AppData/Roaming/.capsule/config.cfg".replaceAll("/", "\\\\");
      }
   }

   public static String getDirectory() {
      switch(SystemInfo.instance.getOS()) {
      case SystemInfo.OS.WINDOWS:
         return backslashes(System.getProperty("user.home") + "/AppData/Roaming/.capsule/");
      case SystemInfo.OS.MACOS:
         return String.format("~/Library/Application Support/capsule/");
      case SystemInfo.OS.LINUX:
         return linuxHomeDir + "/.capsule/";
      case SystemInfo.OS.OTHER:
         System.out.println("Unsupported operating system (assuming Linux).");
         return linuxHomeDir + "/.capsule/";
      default:
         System.out.println("Unknown operating system (assuming Windows).");
         return backslashes(System.getProperty("user.home") + "/AppData/Roaming/.capsule/");
      }
   }

   public static String getDesktop() {
	   return backslashes(System.getProperty("user.home") + "/Desktop/");
   }
   
   public static BufferedImage getImageWeb(URL uri) {
	   try {
		   return ImageIO.read(uri);
	   } catch (Exception e) {
		   System.err.println("Failed to fetch image from URL: " + uri.toString());
		   return null;
	   }
   }
   
   public static BufferedImage getImageWeb(URI uri) {
	   try {
		   HttpClient client = HttpClient.newBuilder()
	    		   .followRedirects(HttpClient.Redirect.ALWAYS) // Bu satırı ekleyin
	    	       .build();

	       HttpRequest request = HttpRequest.newBuilder()
	    		   .uri(uri)
	    		   .header("User-Agent", "Capsule-UtilDownloadFile")
	    		   .header("Cache-Control", "no-cache")
	    		   .header("Pragma", "no-cache")
	               .GET()
	               .build();

	       HttpResponse<InputStream> response =
	               client.send(request, HttpResponse.BodyHandlers.ofInputStream());

	       if (response.statusCode() >= 400) {
	    	   throw new IOException("status code: " + response.statusCode());
	       }

	       try (InputStream in = response.body();) {
			   return ImageIO.read(in);
		   } catch (Exception e) {
			   throw new IOException(e.getMessage(), e);
		   }
	   } catch (Exception e) {
		   System.err.println("Failed to fetch image from URL: " + uri.toString());
		   return null;
	   }  
   }
   
   public synchronized static void downloadFile(
           URI downloadURI,
           File outputFile,
           Consumer<DownloadProgress> progressConsumer
   ) throws IOException, InterruptedException {

       HttpClient client = HttpClient.newBuilder()
    		   .followRedirects(HttpClient.Redirect.ALWAYS) // Bu satırı ekleyin
    	       .build();

       HttpRequest request = HttpRequest.newBuilder()
    		   .uri(downloadURI)
    		   .header("User-Agent", "Capsule-UtilDownloadFile")
    		   .header("Cache-Control", "no-cache")
    		   .header("Pragma", "no-cache")
               .GET()
               .build();

       HttpResponse<InputStream> response =
               client.send(request, HttpResponse.BodyHandlers.ofInputStream());

       long contentLength = response.headers()
               .firstValueAsLong("Content-Length")
               .orElse(-1);

       Files.createDirectories(outputFile.toPath().getParent());
       
       if (response.statusCode() >= 400) {
    	   throw new IOException("status code: " + response.statusCode());
       }

       try (InputStream in = response.body();
            OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {

           byte[] buffer = new byte[8192];
           long downloaded = 0;

           long lastTime = System.nanoTime();
           long lastBytes = 0;
           int lastPercent = 0;

           int read;
           while ((read = in.read(buffer)) != -1) {
               out.write(buffer, 0, read);
               downloaded += read;

               long now = System.nanoTime();
               long deltaTime = now - lastTime;

               if (deltaTime >= 1_000_000_000L) { // 1 saniye
                   long deltaBytes = downloaded - lastBytes;
                   double speedKBps = (deltaBytes / 1024.0)
                           / (deltaTime / 1_000_000_000.0);

                   int percent = contentLength > 0
                           ? (int) ((downloaded * 100) / contentLength)
                           : -1;

                   if (percent != lastPercent) {
                       lastPercent = percent;
                       if (progressConsumer != null) {
                    	   progressConsumer.accept(
                                   new DownloadProgress("Downloading File: " + outputFile.getName(), percent, speedKBps, false)
                           );
                       }
                   }

                   lastBytes = downloaded;
                   lastTime = now;
               }
           }
       }
       
       if (progressConsumer != null) {
    	   progressConsumer.accept(new DownloadProgress("Finished Downloaded File: " + outputFile.getName(), 100, 0, false));
       }
   }
}

