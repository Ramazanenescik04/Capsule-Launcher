package net.capsule.update.util;

public record DownloadProgress(String progressName, int percent, double speedKBps, boolean isFinished) {
	
	@Override
    public String toString() {
        if (percent >= 0) {
            return String.format(
                "%%%d | %.2f KB/s",
                percent,
                speedKBps
            );
        } else {
            return String.format(
                "??%% | %.2f KB/s",
                speedKBps
            );
        }
    }
	
}