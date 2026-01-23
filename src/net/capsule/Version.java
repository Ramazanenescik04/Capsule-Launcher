package net.capsule;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    
    // Versiyon formatını zorlamak için Regex (Örn: 1.0.2)
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String originalString;

    /**
     * String versiyonu parse eder.
     * Format: Major.Minor veya Major.Minor.Patch (Örn: "1.0" veya "1.0.2")
     */
    public Version(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Versiyon numarası boş olamaz.");
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Geçersiz versiyon formatı: " + version);
        }

        this.major = Integer.parseInt(matcher.group(1));
        this.minor = Integer.parseInt(matcher.group(2));
        // Patch kısmı opsiyonel olabilir, yoksa 0 kabul edilir.
        this.patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        
        this.originalString = version;
    }

    // Alternatif Constructor: Doğrudan sayılarla oluşturmak için
    public Version(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Versiyon numaraları negatif olamaz.");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.originalString = String.format("%d.%d.%d", major, minor, patch);
    }

    // Getter Metotları
    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }

    /**
     * Bu versiyonun diğer versiyondan büyük olup olmadığını kontrol eder.
     */
    public boolean isGreaterThan(Version other) {
        return this.compareTo(other) > 0;
    }

    /**
     * Versiyonları karşılaştırma mantığı (Comparable implementasyonu).
     * Sırasıyla Major -> Minor -> Patch kontrol edilir.
     */
    @Override
    public int compareTo(Version other) {
        if (other == null) return 1;

        int majorDiff = Integer.compare(this.major, other.major);
        if (majorDiff != 0) return majorDiff;

        int minorDiff = Integer.compare(this.minor, other.minor);
        if (minorDiff != 0) return minorDiff;

        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString() {
        return originalString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major && minor == version.minor && patch == version.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }
}