package com.openai.threekingdoms;

public final class OriginalArtData {
    public static final String BACKGROUND_RESOURCE = "sgpz_bg_download";

    public static final class Entry {
        public final String assetId;
        public final String resourceName;

        public Entry(String assetId) {
            this.assetId = assetId;
            this.resourceName = "sgpz_" + assetId;
        }
    }

    public static final Entry[] CHARACTER_ART = {
            new Entry("ch_14200004_l"),
            new Entry("ch_15200025_l"),
            new Entry("ch_23100019_l"),
            new Entry("ch_24200004_l"),
            new Entry("ch_25600027_l"),
            new Entry("ch_34200004_l"),
            new Entry("ch_35300025_l"),
            new Entry("ch_35300032_l"),
            new Entry("ch_35400027_l"),
            new Entry("ch_44200004_l"),
            new Entry("ch_45500027_l"),
            new Entry("ch_45500028_l"),
            new Entry("ch_53600019_l"),
            new Entry("ch_54100023_l"),
            new Entry("ch_55600025_l"),
            new Entry("ch_56400035_l")
    };

    private OriginalArtData() {}
}
