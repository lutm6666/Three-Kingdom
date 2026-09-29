package com.openai.threekingdoms;

public final class OriginalArtData {
    public static final String BACKGROUND_RESOURCE = "sgpz_bg_download";

    public static final class Entry {
        public final String assetId;
        public final String resourceName;
        public final int cardNo;
        public final String characterName;
        public final String variant;
        public final String confidence;

        public Entry(String assetId) {
            this(assetId, -1, "", "", "UNVERIFIED");
        }

        public Entry(
                String assetId,
                int cardNo,
                String characterName,
                String variant,
                String confidence) {
            this.assetId = assetId;
            this.resourceName = "sgpz_" + assetId;
            this.cardNo = cardNo;
            this.characterName = characterName;
            this.variant = variant;
            this.confidence = confidence;
        }

        public boolean isHighConfidence() {
            return "HIGH".equals(confidence)
                    && cardNo > 0
                    && characterName != null
                    && !characterName.isEmpty();
        }

        public String verifiedLabel() {
            if (!isHighConfidence()) {
                return "【UNVERIFIED】Asset：" + assetId;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("【HIGH】No.")
                    .append(cardNo)
                    .append(" ");

            if (variant != null && !variant.isEmpty()) {
                if (variant.startsWith("〖")) {
                    sb.append(variant);
                } else {
                    sb.append(characterName)
                            .append("（")
                            .append(variant)
                            .append("）");
                    return sb.append("\nAsset：")
                            .append(assetId)
                            .toString();
                }
            }

            sb.append(characterName)
                    .append("\nAsset：")
                    .append(assetId);

            return sb.toString();
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

    public static int verifiedCount() {
        int count = 0;

        for (Entry entry : CHARACTER_ART) {
            if (entry.isHighConfidence()) {
                count++;
            }
        }

        return count;
    }
}
