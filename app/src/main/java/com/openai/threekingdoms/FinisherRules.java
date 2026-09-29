package com.openai.threekingdoms;

public final class FinisherRules {
    public static final int TROOP_COUNT = 7;

    private FinisherRules() {}

    public static final class Result {
        public final int[] charge = new int[TROOP_COUNT];
        public final int[] stage = new int[TROOP_COUNT];
        public final float[] damageMultiplier = new float[TROOP_COUNT];

        Result() {
            for (int i = 0; i < damageMultiplier.length; i++) {
                damageMultiplier[i] = 1.0f;
            }
        }

        public String summary() {
            StringBuilder sb = new StringBuilder();

            for (int troop = 0; troop < TROOP_COUNT; troop++) {
                if (stage[troop] <= 0) continue;

                if (sb.length() > 0) sb.append("、");

                sb.append(GameData.troopTypeName(troop))
                        .append(" ")
                        .append(finisherName(troop, stage[troop]))
                        .append("（")
                        .append(charge[troop])
                        .append("）");

                if (stage[troop] >= 2) {
                    sb.append("※高階門檻重建");
                }
            }

            return sb.toString();
        }
    }

    public static Result compute(
            GameData.General[] team,
            int[] groupsByFaction) {

        Result result = new Result();

        if (team == null || groupsByFaction == null) {
            return result;
        }

        for (GameData.General general : team) {
            if (general == null) continue;
            if (general.faction < 0 || general.faction >= groupsByFaction.length) continue;
            if (general.troopType < 0 || general.troopType >= TROOP_COUNT) continue;

            result.charge[general.troopType] += groupsByFaction[general.faction];
        }

        for (int troop = 0; troop < TROOP_COUNT; troop++) {
            int stage = stageForCharge(troop, result.charge[troop]);
            result.stage[troop] = stage;
            result.damageMultiplier[troop] =
                    damageMultiplierFor(troop, stage);
        }

        return result;
    }

    public static int stageForCharge(int troopType, int charge) {
        int offset = troopType == GameData.SWORD ? 2 : 0;

        int first = 7 - offset;
        int second = 10 - offset;
        int third = 14 - offset;

        if (charge >= third) return 3;
        if (charge >= second) return 2;
        if (charge >= first) return 1;
        return 0;
    }

    public static float damageMultiplierFor(int troopType, int stage) {
        if (stage <= 0) return 1.0f;

        if (troopType == GameData.SPEAR) {
            switch (stage) {
                case 1: return 1.5f;
                case 2: return 1.8f;
                default: return 2.2f;
            }
        }

        switch (stage) {
            case 1: return 1.2f;
            case 2: return 1.5f;
            default: return 2.0f;
        }
    }

    public static String finisherName(int troopType, int stage) {
        String[][] names = {
                {"", "斬擊", "薙拂", "亂閃"},
                {"", "突破", "突進", "突擊"},
                {"", "突出", "連突", "亂突"},
                {"", "連射", "齊射", "嵐射"},
                {"", "剛力", "豪擊", "破碎"},
                {"", "內通", "偽降", "埋伏"},
                {"", "火計", "伏兵", "大火計"}
        };

        if (troopType < 0 || troopType >= names.length) return "";
        int s = Math.max(0, Math.min(stage, 3));
        return names[troopType][s];
    }

    public static boolean isGuaranteedThreshold(int troopType, int stage) {
        // 第一段門檻是原作明確資料；第二、三段仍是攻略資料的推定值。
        return stage <= 1;
    }
}
