package com.openai.threekingdoms;

/**
 * Tracks where reconstructed game data came from.
 *
 * ORIGINAL_VERIFIED: directly supported by original APK/native data or a
 * source-backed public game database entry.
 * RECONSTRUCTED: rebuilt from documented original rules/data where an exact
 * original Master record is unavailable.
 * CUSTOM: deliberately introduced for offline playability/testing.
 */
public enum DataProvenance {
    ORIGINAL_VERIFIED("原作核實"),
    RECONSTRUCTED("重建"),
    CUSTOM("自訂");

    public final String label;

    DataProvenance(String label) {
        this.label = label;
    }

    public boolean isOriginalVerified() {
        return this == ORIGINAL_VERIFIED;
    }
}
