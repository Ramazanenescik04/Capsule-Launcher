package net.capsule.update.util;

import java.io.*;

import net.capsule.Version;

public class VersionChecker {
	private static final String CAPSULE_PATH = Util.getDirectory() + "jars/";
	private static final String USING_VERSION_FILE = Util.getDirectory() + "versions/";
	public static Version clientVersion = new Version("0.0.0");
	public static Version dikenVersion = new Version("0.0.0");
	
	public static void saveUsingLatestVersion() throws IOException {
		File using = new File(USING_VERSION_FILE);
		
		if (!using.exists()) {
			using.mkdirs();
		}
		
		try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(using, "using.dat")))) {
			outputStream.writeUTF(clientVersion.toString());
			outputStream.writeUTF(dikenVersion.toString());
			outputStream.close();
		}
		
	}
	
	public static void initVersionChecker() {
		File file = new File(CAPSULE_PATH);
		
		if (!file.exists())
			file.mkdirs();
		
		File using = new File(USING_VERSION_FILE);
		
		if (!using.exists()) {
			using.mkdirs();
			
			try {
				saveUsingLatestVersion();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try (DataInputStream outputStream = new DataInputStream(new FileInputStream(new File(using, "using.dat")));){
				var version = outputStream.readUTF();
				var version2 = outputStream.readUTF();
				outputStream.close();
				
				clientVersion = new Version(version);
				dikenVersion = new Version(version2);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
