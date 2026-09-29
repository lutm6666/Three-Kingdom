package com.openai.threekingdoms;

public final class EnhancementData {
    public static final int GROWTH_NEXT_5 = 5;

    // 普通低星武將作素材時的 Lv.1 基礎成長度尚未找到原作表。
    // 目前集中為重建值，避免分散在 UI 或存檔邏輯。
    public static final int RECON_STAR1_BASE_GROWTH = 100;
    public static final int RECON_STAR2_BASE_GROWTH = 200;

    // 原作 Wiki 的 Next.5 累積成長度，Lv1～15。
    private static final int[] NEXT5_CUMULATIVE = {
            0,
            0,
            5,
            33,
            97,
            208,
            375,
            609,
            916,
            1305,
            1783,
            2357,
            3035,
            3822,
            4725,
            5751
    };

    private EnhancementData() {}

    public static boolean canEnhanceTarget(DropCardData.Card card) {
        return card != null
                && card.maxLevel > 1
                && card.statsVerified;
    }

    public static int growthType(DropCardData.Card card) {
        // 張梁的 Next 類型尚未查到；v2.2 暫以 Next.5 重建。
        return GROWTH_NEXT_5;
    }

    public static boolean growthTypeVerified(DropCardData.Card card) {
        return false;
    }

    public static int maxLevel(DropCardData.Card card) {
        return card == null ? 1 : Math.max(1, card.maxLevel);
    }

    public static int cumulativeForLevel(
            DropCardData.Card card,
            int level) {
        int max = maxLevel(card);
        int lv = Math.max(1, Math.min(level, max));

        if (growthType(card) == GROWTH_NEXT_5) {
            int index = Math.min(lv, NEXT5_CUMULATIVE.length - 1);
            return NEXT5_CUMULATIVE[index];
        }

        return 0;
    }

    public static int levelForCumulative(
            DropCardData.Card card,
            int cumulative) {
        int max = maxLevel(card);
        int level = 1;

        for (int lv = 2; lv <= max; lv++) {
            if (cumulative >= cumulativeForLevel(card, lv)) {
                level = lv;
            } else {
                break;
            }
        }

        return level;
    }

    public static int nextRequirement(
            DropCardData.Card card,
            int currentLevel,
            int cumulative) {
        int max = maxLevel(card);
        if (currentLevel >= max) return 0;

        return Math.max(
                0,
                cumulativeForLevel(card, currentLevel + 1)
                        - cumulative);
    }

    public static int enhancementCost(
            int targetLevel,
            int materialCount) {
        return Math.max(1, targetLevel)
                * 100
                * Math.max(0, materialCount);
    }

    public static int reconstructedBaseMaterialGrowth(
            DropData.Item material) {
        if (material == null || material.type != DropData.TYPE_GENERAL) {
            return 0;
        }

        if (material.rarity >= 2) {
            return RECON_STAR2_BASE_GROWTH;
        }

        return RECON_STAR1_BASE_GROWTH;
    }

    public static int materialGrowth(
            DropCardData.Card target,
            DropData.Item material,
            DropCardData.Card materialCard) {
        if (target == null
                || material == null
                || materialCard == null
                || material.type != DropData.TYPE_GENERAL) {
            return 0;
        }

        int base =
                reconstructedBaseMaterialGrowth(material)
                        * Math.max(1, material.level);

        if (target.faction == materialCard.faction) {
            return Math.round(base * 1.5f);
        }

        return base;
    }

    public static String curveText(DropCardData.Card card) {
        return growthTypeVerified(card)
                ? "Next.5（已校正）"
                : "Next.5（重建；Next 類型待原卡確認）";
    }
}
