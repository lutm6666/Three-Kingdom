package com.openai.threekingdoms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DropData {
    public static final int TYPE_GENERAL = 0;
    public static final int TYPE_MATERIAL = 1;

    // 原作一般關卡基準掉率未查到；目前以可替換重建值運作。
    public static final int RECON_DROP_RATE_PERCENT = 50;

    public static final class Item {
        public final String id;
        public final String name;
        public final int type;
        public final int rarity;
        public final int level;
        public final String source;

        public Item(
                String id,
                String name,
                int type,
                int rarity,
                int level,
                String source) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.rarity = rarity;
            this.level = level;
            this.source = source;
        }

        public String typeName() {
            return type == TYPE_GENERAL ? "武將" : "素材";
        }

        public String displayName() {
            if (type == TYPE_GENERAL) {
                return "★" + rarity + " " + name + " Lv." + level;
            }
            return name;
        }
    }

    public static final Item[] ITEMS = {
            new Item("g_sun_archer_1_1", "孫軍弓兵", TYPE_GENERAL, 1, 1, "黃巾討伐戰"),
            new Item("g_zumao_1_1", "祖茂", TYPE_GENERAL, 1, 1, "黃巾討伐戰"),
            new Item("g_liu_spear_1_1", "劉軍槍兵", TYPE_GENERAL, 1, 1, "黃巾討伐戰"),
            new Item("g_jianyong_1_1", "簡雍", TYPE_GENERAL, 1, 1, "黃巾討伐戰"),
            new Item("g_yellow_spear_1_1", "黃巾槍兵", TYPE_GENERAL, 1, 1, "黃巾討伐戰／鐵門峽之戰"),
            new Item("g_mayuanyi_1_1", "馬元義", TYPE_GENERAL, 1, 1, "黃巾討伐戰"),
            new Item("m_dayuanma", "大宛馬", TYPE_MATERIAL, 0, 0, "黃巾討伐戰"),
            new Item("m_blue_elixir", "青色秘藥", TYPE_MATERIAL, 0, 0, "鐵門峽之戰"),
            new Item("g_zhangmancheng_1_1", "張曼成", TYPE_GENERAL, 1, 1, "鐵門峽之戰"),
            new Item("g_zhangbao_1_1", "張寶", TYPE_GENERAL, 1, 1, "鐵門峽之戰"),
            new Item("m_blue_secret_jade", "青秘玉", TYPE_MATERIAL, 0, 0, "廣宗之戰"),
            new Item("g_zhuzhi_2_2", "朱治", TYPE_GENERAL, 2, 2, "廣宗之戰"),
            new Item("g_handang_2_2", "韓當", TYPE_GENERAL, 2, 2, "廣宗之戰"),
            new Item("g_liu_spear_1_2", "劉軍槍兵", TYPE_GENERAL, 1, 2, "廣宗之戰"),
            new Item("g_guanyu_2_2", "關羽", TYPE_GENERAL, 2, 2, "廣宗之戰"),
            new Item("g_yellow_spear_1_2", "黃巾槍兵", TYPE_GENERAL, 1, 2, "廣宗之戰"),
            new Item("g_yellow_captain_2_2", "黃巾兵長", TYPE_GENERAL, 2, 2, "廣宗之戰"),
            new Item("g_zhangbao_1_2", "張寶", TYPE_GENERAL, 1, 2, "廣宗之戰"),
            new Item("g_zhangliang_2_2", "張梁", TYPE_GENERAL, 2, 2, "廣宗之戰"),
            new Item("g_zhoucang_2_2", "周倉", TYPE_GENERAL, 2, 2, "廣宗之戰")
    };

    private DropData() {}

    public static Item get(String id) {
        if (id == null) return null;
        for (Item item : ITEMS) {
            if (item.id.equals(id)) return item;
        }
        return null;
    }

    public static Item possibleDrop(int stageId, String enemyName) {
        if (enemyName == null) return null;

        if (stageId == 0) {
            if (enemyName.equals("孫軍弓兵")) return get("g_sun_archer_1_1");
            if (enemyName.equals("祖茂")) return get("g_zumao_1_1");
            if (enemyName.equals("劉軍槍兵")) return get("g_liu_spear_1_1");
            if (enemyName.equals("簡雍")) return get("g_jianyong_1_1");
            if (enemyName.equals("黃巾槍兵")) return get("g_yellow_spear_1_1");
            if (enemyName.equals("馬元義")) return get("g_mayuanyi_1_1");
            if (enemyName.equals("大宛馬・左慈")) return get("m_dayuanma");
            return null;
        }

        if (stageId == 1) {
            if (enemyName.equals("青色秘藥・左慈")) return get("m_blue_elixir");
            if (enemyName.equals("黃巾槍兵")) return get("g_yellow_spear_1_1");
            if (enemyName.equals("張曼成")) return get("g_zhangmancheng_1_1");
            if (enemyName.equals("張寶")) return get("g_zhangbao_1_1");
            return null;
        }

        if (stageId == 2) {
            if (enemyName.equals("青秘玉・左慈")) return get("m_blue_secret_jade");
            if (enemyName.equals("朱治")) return get("g_zhuzhi_2_2");
            if (enemyName.equals("韓當")) return get("g_handang_2_2");
            if (enemyName.equals("劉軍槍兵")) return get("g_liu_spear_1_2");
            if (enemyName.equals("關羽")) return get("g_guanyu_2_2");
            if (enemyName.equals("黃巾槍兵")) return get("g_yellow_spear_1_2");
            if (enemyName.equals("黃巾兵長")) return get("g_yellow_captain_2_2");
            if (enemyName.equals("張寶")) return get("g_zhangbao_1_2");
            if (enemyName.equals("張梁")) return get("g_zhangliang_2_2");
            if (enemyName.equals("周倉")) return get("g_zhoucang_2_2");
            return null;
        }

        return null;
    }

    public static Item[] roll(StageData.Stage stage) {
        if (stage == null || stage.waves == null) {
            return new Item[0];
        }

        List<Item> result = new ArrayList<>();
        Random random = new Random(
                stage.runSeed
                        ^ 0x5A17D20FL
                        ^ ((long) stage.id << 40));

        for (StageData.Wave wave : stage.waves) {
            for (StageData.Enemy enemy : wave.enemies) {
                Item possible = possibleDrop(stage.id, enemy.name);
                if (possible == null) continue;

                if (random.nextInt(100) < RECON_DROP_RATE_PERCENT) {
                    result.add(possible);
                }
            }
        }

        return result.toArray(new Item[0]);
    }

    public static String possibleDropText(int stageId, StageData.Enemy enemy) {
        Item item = possibleDrop(stageId, enemy.name);
        return item == null ? "無已知掉落" : item.displayName();
    }
}
