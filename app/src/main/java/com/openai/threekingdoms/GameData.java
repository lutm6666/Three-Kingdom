package com.openai.threekingdoms;

public final class GameData {
    public static final int WEI = 0;
    public static final int WU = 1;
    public static final int SHU = 2;
    public static final int HAN = 3;
    public static final int QUN = 4;

    public static final int FIRE = WEI;
    public static final int WATER = WU;
    public static final int WOOD = SHU;
    public static final int LIGHT = HAN;
    public static final int DARK = QUN;

    public static final int SWORD = 0;
    public static final int CAVALRY = 1;
    public static final int SPEAR = 2;
    public static final int BOW = 3;
    public static final int BARBARIAN = 4;
    public static final int GUI_MOU = 5;
    public static final int SHEN_SUAN = 6;

    public static final int SKILL_HEAL = 0;
    public static final int SKILL_DAMAGE = 1;
    public static final int SKILL_DELAY = 2;
    public static final int SKILL_SELF_ATK = 10;
    public static final int SKILL_TEAM_FACTION_ATK = 11;
    public static final int SKILL_CONVERT = 12;
    public static final int SKILL_FREE_MOVE = 13;

    public static final class General {
        public final int id;
        public final String name;
        public final int faction;
        public final int element;
        public final int troopType;
        public final String sourceVariant;
        public final boolean verifiedOriginal;
        public final String rarity;
        public final int maxLevel;

        // 原作卡面 Lv.1 / Lv.Max 端點。未校正卡沿用測試值。
        public final int hp;
        public final int atk;
        public final int recovery;
        public final int maxHp;
        public final int maxAtk;
        public final int maxRecovery;

        public final String skillName;
        public final String skillDescription;
        public final int skillType;
        public final int skillValue;
        public final int skillAux;
        public final int skillCd;
        public final int skillCdMin;

        public final String leaderName;
        public final String leaderDescription;

        public General(
                int id,
                String name,
                int faction,
                int troopType,
                String sourceVariant,
                boolean verifiedOriginal,
                String rarity,
                int maxLevel,
                int hp,
                int atk,
                int recovery,
                int maxHp,
                int maxAtk,
                int maxRecovery,
                String skillName,
                String skillDescription,
                int skillType,
                int skillValue,
                int skillAux,
                int skillCd,
                int skillCdMin,
                String leaderName,
                String leaderDescription) {
            this.id = id;
            this.name = name;
            this.faction = faction;
            this.element = faction;
            this.troopType = troopType;
            this.sourceVariant = sourceVariant;
            this.verifiedOriginal = verifiedOriginal;
            this.rarity = rarity;
            this.maxLevel = maxLevel;
            this.hp = hp;
            this.atk = atk;
            this.recovery = recovery;
            this.maxHp = maxHp;
            this.maxAtk = maxAtk;
            this.maxRecovery = maxRecovery;
            this.skillName = skillName;
            this.skillDescription = skillDescription;
            this.skillType = skillType;
            this.skillValue = skillValue;
            this.skillAux = skillAux;
            this.skillCd = skillCd;
            this.skillCdMin = skillCdMin;
            this.leaderName = leaderName;
            this.leaderDescription = leaderDescription;
        }

        public String factionName() {
            return GameData.factionName(faction);
        }

        public String elementName() {
            return factionName();
        }

        public String troopTypeName() {
            return GameData.troopTypeName(troopType);
        }

        public String leaderSkillName() {
            return leaderName;
        }

        public String leaderSkillDescription() {
            return leaderDescription;
        }

        public float leaderHpMultiplier(General member) {
            if (id == 4 && member.faction == SHU) return 1.5f;
            if (!verifiedOriginal && id == 0) return 1.20f;
            return 1.0f;
        }

        public float leaderDamageMultiplier(
                General attacker,
                int combos,
                int currentHp,
                int maxHpValue) {
            if (id == 7) {
                return currentHp >= maxHpValue ? 3.5f : 1.0f;
            }
            if (id == 9 && attacker.troopType == GUI_MOU) {
                return 2.2f;
            }

            if (!verifiedOriginal) {
                switch (id) {
                    case 1:
                    case 2:
                    case 3:
                        return attacker.faction == SHU ? 1.25f : 1.0f;
                    case 4:
                        return combos >= 4 ? 1.20f : 1.0f;
                    case 5:
                        return attacker.faction == WEI ? 1.30f : 1.0f;
                    case 6:
                        return attacker.faction == WU ? 1.30f : 1.0f;
                    case 8:
                        return 1.0f;
                    default:
                        return 1.0f;
                }
            }

            return 1.0f;
        }

        public float leaderIncomingMultiplier(int enemyFaction) {
            if (id == 1 && enemyFaction == SHU) return 0.5f;
            // 趙雲「大きく減少」的精確倍率尚未由資料庫查到，先不猜。
            return 1.0f;
        }

        public float leaderHealMultiplier() {
            return (!verifiedOriginal && id == 8) ? 1.50f : 1.0f;
        }
    }

    public static final General[] ROSTER = {
            // 尚未逐卡校正者：保留 v1.2 測試資料。
            new General(
                    0, "劉備", SHU, SWORD, "孝行息子", false, "暫定", 50,
                    850, 160, 100, 3545, 748, 350,
                    "仁德", "暫定測試技能", SKILL_HEAL, 900, 0, 4, 4,
                    "漢室仁德（暫定）", "全隊最大生命 ×1.20"),
            new General(
                    1, "關羽", SHU, SPEAR, "美髯公", true, "★★★★★★", 99,
                    520, 300, 60, 3208, 1230, 191,
                    "青龍咆哮", "使用武將攻擊力50倍的「蜀」攻擊", SKILL_SELF_ATK, 50, SHU, 30, 20,
                    "義侠の武", "敵方「蜀」武將造成的傷害減半"),
            new General(
                    2, "張飛", SHU, BARBARIAN, "闘鬼", false, "暫定", 50,
                    1050, 225, 80, 3500, 900, 240,
                    "長坂怒吼", "暫定測試技能", SKILL_DELAY, 2, 0, 5, 5,
                    "燕人之勇（暫定）", "蜀勢力傷害 ×1.25"),
            new General(
                    3, "趙雲", SHU, CAVALRY, "一陣の風", true, "★★★★★☆", 50,
                    280, 249, 59, 1260, 872, 177,
                    "長坂一騎駆け", "10秒間自由移動單位", SKILL_FREE_MOVE, 10, 0, 20, 14,
                    "神威に至る槍術", "單位移動時間大幅延長；受到的傷害大幅減少"),
            new General(
                    4, "諸葛亮", SHU, SHEN_SUAN, "臥龍雌伏", true, "★★★★☆☆", 50,
                    287, 264, 53, 718, 554, 111,
                    "奇門遁甲", "將「魏」單位轉換為「蜀」單位", SKILL_CONVERT, WEI, SHU, 10, 5,
                    "八卦陣", "我方「蜀」武將 HP ×1.5"),
            new General(
                    5, "曹操", WEI, SWORD, "東郡太守", false, "暫定", 50,
                    920, 235, 70, 3600, 980, 220,
                    "魏武之威", "暫定測試技能", SKILL_DAMAGE, 1050, 0, 5, 5,
                    "魏武霸業（暫定）", "魏勢力傷害 ×1.30"),
            new General(
                    6, "孫權", WU, SWORD, "若き虎の覚悟", false, "暫定", 50,
                    940, 200, 90, 3550, 900, 300,
                    "江東固守", "暫定測試技能", SKILL_HEAL, 750, 0, 4, 4,
                    "江東基業（暫定）", "吳勢力傷害 ×1.30"),
            new General(
                    7, "呂布", QUN, CAVALRY, "戦鬼", true, "★★★★★★☆", 50,
                    341, 277, 22, 1705, 970, 176,
                    "天下無双", "敵單體受到使用武將攻擊力30倍的無視防禦攻擊", SKILL_SELF_ATK, 30, QUN, 35, 35,
                    "吠虎の猛勇", "HP全滿時，全武將攻擊力 ×3.5"),
            new General(
                    8, "貂蟬", QUN, GUI_MOU, "美女連環", false, "暫定", 50,
                    730, 175, 120, 2500, 800, 420,
                    "閉月", "暫定測試技能", SKILL_HEAL, 1000, 0, 5, 5,
                    "傾城（暫定）", "桃回復量 ×1.50"),
            new General(
                    9, "周瑜", WU, GUI_MOU, "小覇王盟友", true, "★★★★★☆", 50,
                    320, 291, 76, 1280, 1019, 243,
                    "孫呉の業火", "敵全體受到部隊「吳」攻擊力8倍的攻擊", SKILL_TEAM_FACTION_ATK, 8, WU, 28, 15,
                    "借刀殺人の計", "攻擊後進行強力再攻擊；鬼謀武將攻擊力 ×2.2")
    };

    private GameData() {}

    public static String factionName(int faction) {
        switch (faction) {
            case WEI: return "魏";
            case WU: return "吳";
            case SHU: return "蜀";
            case HAN: return "漢";
            case QUN: return "群";
            default: return "?";
        }
    }

    public static String troopTypeName(int troopType) {
        switch (troopType) {
            case SWORD: return "劍兵";
            case CAVALRY: return "騎兵";
            case SPEAR: return "槍兵";
            case BOW: return "弓兵";
            case BARBARIAN: return "蠻兵";
            case GUI_MOU: return "鬼謀";
            case SHEN_SUAN: return "神算";
            default: return "?";
        }
    }

    public static General get(int id) {
        if (id < 0 || id >= ROSTER.length) return ROSTER[0];
        return ROSTER[id];
    }

    public static int recruitPrice(int id) {
        switch (id) {
            case 5: return 900;
            case 6: return 900;
            case 7: return 1500;
            case 8: return 800;
            case 9: return 1000;
            default: return 0;
        }
    }

    public static int[] defaultTeam() {
        return new int[]{0, 1, 2, 3, 4};
    }
}
