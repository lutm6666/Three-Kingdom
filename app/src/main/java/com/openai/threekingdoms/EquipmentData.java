package com.openai.threekingdoms;

public final class EquipmentData {
    public static final class Weapon {
        public final int id;
        public final String name;
        public final int atkBonus;
        public final String source;

        public Weapon(int id, String name, int atkBonus, String source) {
            this.id = id;
            this.name = name;
            this.atkBonus = atkBonus;
            this.source = source;
        }
    }

    public static final Weapon[] WEAPONS = {
            new Weapon(0, "青銅劍", 20, "舊重建版遺留裝備"),
            new Weapon(1, "環首刀", 35, "舊重建版遺留裝備"),
            new Weapon(2, "方天畫戟", 60, "舊重建版遺留裝備")
    };

    private EquipmentData() {}

    public static Weapon get(int id) {
        if (id < 0 || id >= WEAPONS.length) return null;
        return WEAPONS[id];
    }

    public static int rewardWeaponForStage(int stageIndex) {
        // v1.8 起原作關卡不再使用舊版自創固定武器掉落。
        return -1;
    }
}
