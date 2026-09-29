package com.openai.threekingdoms;

public final class DropCardData {
    public static final class Card {
        public final String dropId;
        public final String name;
        public final int faction;
        public final int troopType;
        public final int rarity;
        public final int maxLevel;
        public final int hp;
        public final int atk;
        public final int recovery;
        public final int maxHp;
        public final int maxAtk;
        public final int maxRecovery;
        public final String skillName;
        public final String skillDescription;
        public final String leaderName;
        public final String leaderDescription;
        public final String sourceCardNo;
        public final boolean statsVerified;
        public final boolean identityVerified;
        public final String verificationNote;

        public Card(
                String dropId,
                String name,
                int faction,
                int troopType,
                int rarity,
                int maxLevel,
                int hp,
                int atk,
                int recovery,
                int maxHp,
                int maxAtk,
                int maxRecovery,
                String skillName,
                String skillDescription,
                String leaderName,
                String leaderDescription,
                String sourceCardNo,
                boolean statsVerified,
                boolean identityVerified,
                String verificationNote) {
            this.dropId = dropId;
            this.name = name;
            this.faction = faction;
            this.troopType = troopType;
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
            this.leaderName = leaderName;
            this.leaderDescription = leaderDescription;
            this.sourceCardNo = sourceCardNo;
            this.statsVerified = statsVerified;
            this.identityVerified = identityVerified;
            this.verificationNote = verificationNote;
        }

        public String factionName() {
            return GameData.factionName(faction);
        }

        public String troopTypeName() {
            return troopType < 0 ? "未校正" : GameData.troopTypeName(troopType);
        }

        public String stars() {
            if (rarity <= 0) return "★?";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rarity; i++) sb.append("★");
            return sb.toString();
        }

        public String statSummary() {
            if (!statsVerified) {
                return "HP／攻擊／回復：未校正";
            }

            return "HP " + hp + "→" + maxHp
                    + "　ATK " + atk + "→" + maxAtk
                    + "　回復 " + recovery + "→" + maxRecovery;
        }
    }

    private static Card partial(
            String id,
            String name,
            int faction,
            int troop,
            int rarity,
            int maxLevel,
            String skillName,
            String skillDescription,
            String cardNo,
            String note) {
        return new Card(
                id, name, faction, troop, rarity, maxLevel,
                0, 0, 0, 0, 0, 0,
                skillName, skillDescription,
                "無／未校正", "無／未校正",
                cardNo, false, true, note);
    }

    public static final Card[] CARDS = {
            partial(
                    "g_sun_archer_1_1",
                    "孫軍弓兵",
                    GameData.WU,
                    GameData.BOW,
                    1,
                    0,
                    "未校正",
                    "低星本體技能資料尚未查齊",
                    "No.195",
                    "圖鑑編號、勢力與弓兵身分已校正；三圍待補。"),

            partial(
                    "g_zumao_1_1",
                    "祖茂",
                    GameData.WU,
                    GameData.SWORD,
                    1,
                    0,
                    "吳軍轉換",
                    "其進化系可把「桃」轉成「吳」；低星本體技能名稱待核對",
                    null,
                    "原作攻略確認祖茂進化系為劍兵、可桃→吳；低星本體三圍待補。"),

            partial(
                    "g_liu_spear_1_1",
                    "劉軍槍兵",
                    GameData.SHU,
                    GameData.SPEAR,
                    1,
                    0,
                    "未校正",
                    "低星本體技能資料尚未查齊",
                    "No.197",
                    "圖鑑編號、勢力與槍兵身分已校正；三圍待補。"),

            partial(
                    "g_jianyong_1_1",
                    "簡雍",
                    GameData.SHU,
                    -1,
                    1,
                    0,
                    "未校正",
                    "目前查到的是其他版本簡雍，不能直接套用低星本體",
                    null,
                    "勢力已校正；兵種與低星本體數值尚待原卡頁。"),

            partial(
                    "g_yellow_spear_1_1",
                    "黃巾槍兵",
                    GameData.QUN,
                    GameData.SPEAR,
                    1,
                    0,
                    "未校正",
                    "低星本體技能資料尚未查齊",
                    "No.205",
                    "圖鑑編號、勢力與槍兵身分已校正；三圍待補。"),

            partial(
                    "g_mayuanyi_1_1",
                    "馬元義",
                    GameData.QUN,
                    -1,
                    1,
                    0,
                    "未校正",
                    "低星本體技能資料尚未查齊",
                    "No.54",
                    "圖鑑編號與勢力已校正；兵種與三圍待補。"),

            partial(
                    "g_zhangmancheng_1_1",
                    "張曼成",
                    GameData.QUN,
                    GameData.BARBARIAN,
                    1,
                    0,
                    "黃巾的武勇",
                    "1 回合降低敵方防禦；原作進化卡 CD 12→7",
                    "No.96",
                    "圖鑑編號已校正；進化卡確認為群／蠻兵與同技能系，低星三圍待補。"),

            partial(
                    "g_zhangbao_1_1",
                    "張寶",
                    GameData.QUN,
                    GameData.GUI_MOU,
                    1,
                    0,
                    "群軍轉換",
                    "其進化系可把「桃」轉成「群」；低星本體技能名稱待核對",
                    null,
                    "原作攻略確認張寶進化系為鬼謀與桃→群；低星三圍待補。"),

            partial(
                    "g_zhuzhi_2_2",
                    "朱治",
                    GameData.WU,
                    -1,
                    2,
                    0,
                    "小霸王への進言",
                    "3 回合小幅降低敵方防禦",
                    "No.58",
                    "圖鑑編號、勢力與技能系已校正；兵種與三圍待補。"),

            partial(
                    "g_handang_2_2",
                    "韓當",
                    GameData.WU,
                    -1,
                    2,
                    0,
                    "剛弓猛擊",
                    "對敵單體造成自身攻擊力 10 倍傷害",
                    "No.68",
                    "圖鑑編號、勢力與技能已校正；兵種與三圍待補。"),

            partial(
                    "g_liu_spear_1_2",
                    "劉軍槍兵",
                    GameData.SHU,
                    GameData.SPEAR,
                    1,
                    0,
                    "未校正",
                    "與 Lv.1 掉落為同一卡片版本、掉落時等級不同",
                    "No.197",
                    "同一原卡，掉落等級為 Lv.2。"),

            partial(
                    "g_guanyu_2_2",
                    "關羽",
                    GameData.SHU,
                    -1,
                    2,
                    0,
                    "未校正",
                    "此為低星 No.80 關羽，不能套用〖美髯公〗No.180 的資料",
                    "No.80",
                    "卡片身分與勢力已校正；兵種、三圍與低星技能待補。"),

            partial(
                    "g_yellow_spear_1_2",
                    "黃巾槍兵",
                    GameData.QUN,
                    GameData.SPEAR,
                    1,
                    0,
                    "未校正",
                    "與 Lv.1 掉落為同一卡片版本、掉落時等級不同",
                    "No.205",
                    "同一原卡，掉落等級為 Lv.2。"),

            partial(
                    "g_yellow_captain_2_2",
                    "黃巾兵長",
                    GameData.QUN,
                    -1,
                    2,
                    0,
                    "未校正",
                    "低星本體技能資料尚未查齊",
                    "No.207",
                    "圖鑑編號與勢力已校正；兵種與三圍待補。"),

            partial(
                    "g_zhangbao_1_2",
                    "張寶",
                    GameData.QUN,
                    GameData.GUI_MOU,
                    1,
                    0,
                    "群軍轉換",
                    "其進化系可把「桃」轉成「群」；低星本體技能名稱待核對",
                    null,
                    "同一低星原卡，掉落等級為 Lv.2。"),

            new Card(
                    "g_zhangliang_2_2",
                    "張梁",
                    GameData.QUN,
                    GameData.SWORD,
                    2,
                    15,
                    57,
                    51,
                    13,
                    342,
                    166,
                    54,
                    "黃巾的暴威",
                    "3 回合小幅降低敵方防禦，CD 14→6",
                    "無",
                    "無",
                    "No.64",
                    true,
                    true,
                    "原作卡頁完整校正。"),

            partial(
                    "g_zhoucang_2_2",
                    "周倉",
                    GameData.QUN,
                    -1,
                    2,
                    0,
                    "未校正",
                    "低星本體技能與三圍尚待原卡頁",
                    "No.238",
                    "圖鑑編號與勢力已校正；低星完整資料待補。")
    };

    private DropCardData() {}

    public static Card get(String dropId) {
        if (dropId == null) return null;
        for (Card card : CARDS) {
            if (card.dropId.equals(dropId)) return card;
        }
        return null;
    }
}
