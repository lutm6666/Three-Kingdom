package com.openai.threekingdoms;

import android.content.Context;
import android.content.SharedPreferences;

public final class PlayerData {
    private static final String PREFS = "progress";
    private static final int MAX_LEVEL = 50;

    private PlayerData() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int getCoins(Context context) {
        return prefs(context).getInt("coins", 0);
    }

    public static int getHighestUnlockedStage(Context context) {
        return prefs(context).getInt("highest_unlocked_stage", 0);
    }

    public static void unlockStage(Context context, int stageIndex) {
        SharedPreferences p = prefs(context);
        int current = p.getInt("highest_unlocked_stage", 0);
        if (stageIndex > current) {
            p.edit().putInt("highest_unlocked_stage", stageIndex).apply();
        }
    }

    public static void addCoins(Context context, int amount) {
        SharedPreferences p = prefs(context);
        p.edit().putInt("coins", Math.max(0, p.getInt("coins", 0) + amount)).apply();
    }

    public static boolean spendCoins(Context context, int amount) {
        SharedPreferences p = prefs(context);
        int coins = p.getInt("coins", 0);
        if (amount < 0 || coins < amount) return false;

        p.edit().putInt("coins", coins - amount).apply();
        return true;
    }

    public static boolean isGeneralOwned(Context context, int generalId) {
        SharedPreferences p = prefs(context);
        String key = "owned_" + generalId;

        if (p.contains(key)) {
            return p.getBoolean(key, false);
        }

        return generalId >= 0 && generalId <= 4;
    }

    public static void setGeneralOwned(Context context, int generalId, boolean owned) {
        prefs(context)
                .edit()
                .putBoolean("owned_" + generalId, owned)
                .apply();
    }

    public static void migrateOwnership(Context context, int[] currentTeam) {
        SharedPreferences.Editor editor = prefs(context).edit();

        for (int id = 0; id <= 4; id++) {
            editor.putBoolean("owned_" + id, true);
        }

        if (currentTeam != null) {
            for (int id : currentTeam) {
                if (id >= 0 && id < GameData.ROSTER.length) {
                    editor.putBoolean("owned_" + id, true);
                }
            }
        }

        editor.apply();
    }

    public static int getLootCount(Context context, String itemId) {
        if (DropData.get(itemId) == null) return 0;
        return prefs(context).getInt("loot_" + itemId, 0);
    }

    public static void addLoot(Context context, String itemId, int amount) {
        if (DropData.get(itemId) == null || amount <= 0) return;

        SharedPreferences p = prefs(context);
        int current = p.getInt("loot_" + itemId, 0);

        p.edit()
                .putInt("loot_" + itemId, current + amount)
                .apply();
    }

    public static boolean consumeLoot(
            Context context,
            String itemId,
            int amount) {
        if (DropData.get(itemId) == null || amount <= 0) return false;

        SharedPreferences p = prefs(context);
        int current = p.getInt("loot_" + itemId, 0);
        if (current < amount) return false;

        p.edit()
                .putInt("loot_" + itemId, current - amount)
                .apply();

        return true;
    }

    public static int distinctLootCount(Context context) {
        int count = 0;
        for (DropData.Item item : DropData.ITEMS) {
            if (getLootCount(context, item.id) > 0) count++;
        }
        return count;
    }

    public static int totalLootCount(Context context) {
        int count = 0;
        for (DropData.Item item : DropData.ITEMS) {
            count += getLootCount(context, item.id);
        }
        return count;
    }

    private static int defaultDropCardLevel(String dropId) {
        DropData.Item item = DropData.get(dropId);
        return item == null ? 1 : Math.max(1, item.level);
    }

    public static int getDropCardGrowth(
            Context context,
            String dropId) {
        DropCardData.Card card = DropCardData.get(dropId);
        if (card == null) return 0;

        String key = "dropcard_growth_" + dropId;
        SharedPreferences p = prefs(context);

        if (p.contains(key)) {
            return p.getInt(key, 0);
        }

        return EnhancementData.cumulativeForLevel(
                card,
                defaultDropCardLevel(dropId));
    }

    public static int getDropCardLevel(
            Context context,
            String dropId) {
        DropCardData.Card card = DropCardData.get(dropId);
        if (card == null) return 1;

        return EnhancementData.levelForCumulative(
                card,
                getDropCardGrowth(context, dropId));
    }

    public static int addDropCardGrowth(
            Context context,
            String dropId,
            int amount) {
        DropCardData.Card card = DropCardData.get(dropId);
        if (card == null || amount <= 0) {
            return getDropCardGrowth(context, dropId);
        }

        int current = getDropCardGrowth(context, dropId);
        int maxGrowth = EnhancementData.cumulativeForLevel(
                card,
                EnhancementData.maxLevel(card));

        int updated = Math.min(maxGrowth, current + amount);

        prefs(context)
                .edit()
                .putInt("dropcard_growth_" + dropId, updated)
                .apply();

        return updated;
    }

    public static boolean ownsWeapon(Context context, int weaponId) {
        return prefs(context).getBoolean("weapon_owned_" + weaponId, false);
    }

    public static void grantWeapon(Context context, int weaponId) {
        if (EquipmentData.get(weaponId) == null) return;
        prefs(context)
                .edit()
                .putBoolean("weapon_owned_" + weaponId, true)
                .apply();
    }

    public static int getEquippedWeapon(Context context, int generalId) {
        return prefs(context).getInt("equipped_weapon_" + generalId, -1);
    }

    public static void equipWeapon(Context context, int generalId, int weaponId) {
        if (weaponId >= 0 && !ownsWeapon(context, weaponId)) return;

        SharedPreferences p = prefs(context);
        SharedPreferences.Editor editor = p.edit();

        if (weaponId >= 0) {
            for (GameData.General g : GameData.ROSTER) {
                if (g.id != generalId
                        && p.getInt("equipped_weapon_" + g.id, -1) == weaponId) {
                    editor.remove("equipped_weapon_" + g.id);
                }
            }

            editor.putInt("equipped_weapon_" + generalId, weaponId);
        } else {
            editor.remove("equipped_weapon_" + generalId);
        }

        editor.apply();
    }

    public static int weaponAtkBonus(Context context, int generalId) {
        EquipmentData.Weapon weapon =
                EquipmentData.get(getEquippedWeapon(context, generalId));

        return weapon == null ? 0 : weapon.atkBonus;
    }

    public static int getLevel(Context context, int generalId) {
        return prefs(context).getInt("level_" + generalId, 1);
    }

    public static int getExp(Context context, int generalId) {
        return prefs(context).getInt("exp_" + generalId, 0);
    }

    public static int expToNext(int level) {
        if (level >= MAX_LEVEL) return 0;
        return 100 + (level - 1) * 40;
    }

    public static int expToNext(GameData.General g, int level) {
        if (level >= g.maxLevel) return 0;
        // 原作每級所需 EXP 表尚未匯入；暫保留本地測試成長需求。
        return 100 + (level - 1) * 40;
    }

    private static int interpolate(int start, int end, int level, int maxLevel) {
        if (maxLevel <= 1) return end;
        int lv = Math.max(1, Math.min(level, maxLevel));
        float t = (lv - 1) / (float) (maxLevel - 1);
        return Math.round(start + (end - start) * t);
    }

    public static int effectiveHpAtLevel(GameData.General g, int level) {
        if (g.verifiedOriginal) {
            return interpolate(g.hp, g.maxHp, level, g.maxLevel);
        }
        return g.hp + (Math.max(1, level) - 1) * 55;
    }

    public static int effectiveAtkAtLevel(GameData.General g, int level) {
        if (g.verifiedOriginal) {
            return interpolate(g.atk, g.maxAtk, level, g.maxLevel);
        }
        return g.atk + (Math.max(1, level) - 1) * 12;
    }

    public static int effectiveRecoveryAtLevel(GameData.General g, int level) {
        if (g.verifiedOriginal) {
            return interpolate(g.recovery, g.maxRecovery, level, g.maxLevel);
        }
        return g.recovery;
    }

    public static int effectiveHp(Context context, GameData.General g) {
        return effectiveHpAtLevel(g, getLevel(context, g.id));
    }

    public static int effectiveAtk(Context context, GameData.General g) {
        return effectiveAtkAtLevel(g, getLevel(context, g.id))
                + weaponAtkBonus(context, g.id);
    }

    public static int effectiveRecovery(Context context, GameData.General g) {
        return effectiveRecoveryAtLevel(g, getLevel(context, g.id));
    }

    public static GainResult addExp(Context context, int generalId, int amount) {
        SharedPreferences p = prefs(context);
        GameData.General g = GameData.get(generalId);
        int level = p.getInt("level_" + generalId, 1);
        int exp = p.getInt("exp_" + generalId, 0);
        int levelsGained = 0;

        if (level >= g.maxLevel) {
            return new GainResult(level, exp, 0);
        }

        exp += Math.max(0, amount);

        while (level < g.maxLevel) {
            int need = expToNext(g, level);
            if (exp < need) break;
            exp -= need;
            level++;
            levelsGained++;
        }

        if (level >= g.maxLevel) {
            exp = 0;
        }

        p.edit()
                .putInt("level_" + generalId, level)
                .putInt("exp_" + generalId, exp)
                .apply();

        return new GainResult(level, exp, levelsGained);
    }

    public static final class GainResult {
        public final int level;
        public final int exp;
        public final int levelsGained;

        public GainResult(int level, int exp, int levelsGained) {
            this.level = level;
            this.exp = exp;
            this.levelsGained = levelsGained;
        }
    }
}
