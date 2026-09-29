package com.openai.threekingdoms;

import java.util.Random;

public final class StageData {
    public static final int[] ADV_HAN_WEI_QUN = {
            GameData.HAN, GameData.WEI,
            GameData.WEI, GameData.QUN
    };

    public static final int[] ADV_HAN_WEI_QUN_CYCLE = {
            GameData.HAN, GameData.WEI,
            GameData.WEI, GameData.QUN,
            GameData.QUN, GameData.HAN
    };

    public static final int[] ADV_WEI_QUN_HAN = {
            GameData.WEI, GameData.QUN,
            GameData.HAN, GameData.QUN,
            GameData.QUN, GameData.HAN
    };

    public static final int AI_ATTACK = 0;
    public static final int AI_SCOUT = 1;
    public static final int AI_POWER_UP = 2;
    public static final int AI_CONVERT = 3;
    public static final int AI_SKILL_SEAL = 4;

    public static final class EnemyAction {
        public final String name;
        public final int type;
        public final int value;
        public final int aux;

        private EnemyAction(String name, int type, int value, int aux) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.aux = aux;
        }

        public static EnemyAction attack(String name, int percent) {
            return new EnemyAction(name, AI_ATTACK, percent, 0);
        }

        public static EnemyAction scout(String name) {
            return new EnemyAction(name, AI_SCOUT, 0, 0);
        }

        public static EnemyAction powerUp(String name, int percent, int attacks) {
            return new EnemyAction(name, AI_POWER_UP, percent, attacks);
        }

        public static EnemyAction convert(String name, int fromUnit, int toUnit) {
            return new EnemyAction(name, AI_CONVERT, fromUnit, toUnit);
        }

        public static EnemyAction skillSeal(String name, int turns) {
            return new EnemyAction(name, AI_SKILL_SEAL, turns, 0);
        }

        public String shortText() {
            switch (type) {
                case AI_ATTACK:
                    return name + (value == 100 ? "" : " ×" + (value / 100f));
                case AI_SCOUT:
                    return name + "（不攻擊）";
                case AI_POWER_UP:
                    return name + "（攻擊×" + (value / 100f) + "，" + aux + "次）";
                case AI_CONVERT:
                    return name + "（" + unitName(value) + "→" + unitName(aux) + "）";
                case AI_SKILL_SEAL:
                    return name + "（技能封印" + value + "回合）";
                default:
                    return name;
            }
        }
    }

    public static final class Enemy {
        public final String name;
        public final int maxHp;
        public final int attack;
        public final int interval;
        public final int faction;
        public final int defense;
        public final boolean attackVerified;
        public final boolean turnVerified;
        public final boolean hpDefenseVerified;
        public final EnemyAction preemptive;
        public final EnemyAction[] actions;

        public Enemy(
                String name,
                int maxHp,
                int attack,
                int interval,
                int faction,
                int defense,
                boolean attackVerified,
                boolean turnVerified,
                boolean hpDefenseVerified,
                EnemyAction preemptive,
                EnemyAction... actions) {
            this.name = name;
            this.maxHp = maxHp;
            this.attack = attack;
            this.interval = interval;
            this.faction = faction;
            this.defense = defense;
            this.attackVerified = attackVerified;
            this.turnVerified = turnVerified;
            this.hpDefenseVerified = hpDefenseVerified;
            this.preemptive = preemptive;
            this.actions = (actions == null || actions.length == 0)
                    ? new EnemyAction[]{EnemyAction.attack("通常攻擊", 100)}
                    : actions;
        }

        public String statMark() {
            return hpDefenseVerified ? "" : "*";
        }

        public String attackMark() {
            return attackVerified ? "" : "*";
        }

        public String turnMark() {
            return turnVerified ? "" : "*";
        }

        public String aiSummary() {
            StringBuilder sb = new StringBuilder();
            if (preemptive != null) {
                sb.append("先制：").append(preemptive.shortText());
            }
            for (EnemyAction action : actions) {
                if (sb.length() > 0) sb.append("／");
                sb.append(action.shortText());
            }
            return sb.toString();
        }
    }

    public static final class Wave {
        public final Enemy[] enemies;

        public final String name;
        public final int maxHp;
        public final int attack;
        public final int interval;
        public final int faction;
        public final int element;
        public final int defense;

        public Wave(Enemy... enemies) {
            if (enemies == null || enemies.length == 0) {
                throw new IllegalArgumentException("Wave requires at least one enemy");
            }
            this.enemies = enemies;
            Enemy first = enemies[0];
            this.name = first.name;
            this.maxHp = first.maxHp;
            this.attack = first.attack;
            this.interval = first.interval;
            this.faction = first.faction;
            this.element = first.faction;
            this.defense = first.defense;
        }
    }

    public static final class EncounterPool {
        public final Enemy[] enemies;
        public final int minEnemiesPerWave;
        public final int maxEnemiesPerWave;
        public final String note;

        public EncounterPool(
                Enemy[] enemies,
                int minEnemiesPerWave,
                int maxEnemiesPerWave,
                String note) {
            this.enemies = enemies;
            this.minEnemiesPerWave = minEnemiesPerWave;
            this.maxEnemiesPerWave = maxEnemiesPerWave;
            this.note = note;
        }

        public Wave roll(Random random) {
            if (enemies == null || enemies.length == 0) {
                throw new IllegalStateException("Encounter pool is empty");
            }

            int min = Math.max(1, Math.min(minEnemiesPerWave, enemies.length));
            int max = Math.max(min, Math.min(maxEnemiesPerWave, enemies.length));
            int count = min + random.nextInt(max - min + 1);

            Enemy[] selected = new Enemy[count];
            boolean[] used = new boolean[enemies.length];

            for (int i = 0; i < count; i++) {
                int index;
                do {
                    index = random.nextInt(enemies.length);
                } while (used[index]);

                used[index] = true;
                selected[i] = enemies[index];
            }

            return new Wave(selected);
        }

        public String summary() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < enemies.length; i++) {
                if (i > 0) sb.append("、");
                sb.append(enemies[i].name);
            }
            return sb.toString();
        }
    }

    public static final class Stage {
        public final int id;
        public final String name;
        public final String chapter;
        public final int difficulty;
        public final int stamina;
        public final int coinReward;
        public final int expReward;
        public final String sourceNote;
        public final int[] advantagePairs;

        public final int waveCount;
        public final EncounterPool commonPool;
        public final Wave fixedBossWave;

        // 僅 materializeRun 後有內容。
        public final Wave[] waves;
        public final long runSeed;

        private Stage(
                int id,
                String name,
                String chapter,
                int difficulty,
                int stamina,
                int coinReward,
                int expReward,
                String sourceNote,
                int[] advantagePairs,
                int waveCount,
                EncounterPool commonPool,
                Wave fixedBossWave,
                Wave[] waves,
                long runSeed) {
            this.id = id;
            this.name = name;
            this.chapter = chapter;
            this.difficulty = difficulty;
            this.stamina = stamina;
            this.coinReward = coinReward;
            this.expReward = expReward;
            this.sourceNote = sourceNote;
            this.advantagePairs = advantagePairs;
            this.waveCount = waveCount;
            this.commonPool = commonPool;
            this.fixedBossWave = fixedBossWave;
            this.waves = waves;
            this.runSeed = runSeed;
        }

        public boolean isAdvantaged(int attackerFaction, int defenderFaction) {
            if (advantagePairs == null) return false;
            for (int i = 0; i + 1 < advantagePairs.length; i += 2) {
                if (advantagePairs[i] == attackerFaction
                        && advantagePairs[i + 1] == defenderFaction) {
                    return true;
                }
            }
            return false;
        }

        public String advantageText() {
            if (advantagePairs == null || advantagePairs.length == 0) {
                return "無特殊優劣";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i + 1 < advantagePairs.length; i += 2) {
                if (i > 0) sb.append("、");
                sb.append(GameData.factionName(advantagePairs[i]))
                        .append("→")
                        .append(GameData.factionName(advantagePairs[i + 1]));
            }
            return sb.toString();
        }

        public boolean isMaterialized() {
            return waves != null && waves.length == waveCount;
        }
    }

    private static Enemy enemy(
            String name,
            int hp,
            int attack,
            int turn,
            int faction,
            int defense,
            boolean attackVerified,
            boolean turnVerified) {
        return new Enemy(
                name, hp, attack, turn, faction, defense,
                attackVerified, turnVerified, false,
                null,
                EnemyAction.attack("通常攻擊", 100));
    }

    private static Stage baseStage(
            int id,
            String name,
            String chapter,
            int difficulty,
            int stamina,
            int coinReward,
            int expReward,
            String sourceNote,
            int[] advantagePairs,
            int waveCount,
            EncounterPool commonPool,
            Wave fixedBossWave) {
        return new Stage(
                id, name, chapter, difficulty, stamina,
                coinReward, expReward, sourceNote, advantagePairs,
                waveCount, commonPool, fixedBossWave,
                null, 0L);
    }

    private static final EncounterPool POOL_YELLOW_TURBAN_1 =
            new EncounterPool(
                    new Enemy[]{
                            enemy("孫軍弓兵", 180, 16, 2, GameData.WU, 0, true, true),
                            enemy("祖茂", 320, 38, 3, GameData.WU, 0, true, true),
                            enemy("劉軍槍兵", 180, 17, 2, GameData.SHU, 0, true, true),
                            enemy("簡雍", 280, 30, 2, GameData.SHU, 0, true, true),
                            enemy("黃巾槍兵", 220, 15, 2, GameData.QUN, 0, true, true),
                            enemy("馬元義", 360, 34, 4, GameData.QUN, 0, true, true),
                            enemy("白秘玉・左慈", 250, 20, 2, GameData.QUN, 5, false, false),
                            enemy("大宛馬・左慈", 300, 25, 2, GameData.QUN, 5, false, false)
                    },
                    1,
                    2,
                    "原作共通出現 8 種；每波 1～2 名、等權重為重建規則");

    private static final EncounterPool POOL_IRON_GATE_1 =
            new EncounterPool(
                    new Enemy[]{
                            enemy("青色秘藥・左慈", 500, 70, 2, GameData.WEI, 10, false, false),
                            enemy("黃巾槍兵", 520, 51, 2, GameData.QUN, 10, true, true),
                            enemy("張曼成", 780, 95, 4, GameData.QUN, 15, false, false)
                    },
                    1,
                    2,
                    "要衝之地 B3 前記錄 3 種敵人；每波 1～2 名、等權重為重建規則");

    private static final EncounterPool POOL_GUANGZONG =
            new EncounterPool(
                    new Enemy[]{
                            enemy("青秘玉・左慈", 550, 20, 2, GameData.WEI, 10, true, true),
                            enemy("朱治", 850, 149, 4, GameData.WU, 15, true, true),
                            enemy("韓當", 1000, 191, 5, GameData.WU, 20, true, true),
                            enemy("劉軍槍兵", 700, 48, 2, GameData.SHU, 10, true, true),
                            enemy("關羽", 1200, 167, 4, GameData.SHU, 25, true, true),
                            enemy("黃巾槍兵", 700, 122, 3, GameData.QUN, 20, true, true),
                            enemy("黃巾兵長", 900, 80, 2, GameData.QUN, 25, true, true),
                            enemy("張寶", 1300, 161, 4, GameData.QUN, 35, true, true),
                            enemy("張梁", 1500, 180, 5, GameData.QUN, 40, false, false)
                    },
                    1,
                    2,
                    "原作共通出現 9 種；每波 1～2 名、等權重為重建規則");

    public static final Stage[] STAGES = {
            baseStage(
                    0,
                    "奸雄的初陣",
                    "曹操軍編・黃巾討伐戰",
                    1, 3, 70, 30,
                    "原作：3合戰；B3 周倉＋黃巾槍兵×2。非B3從共通出現池生成。",
                    ADV_HAN_WEI_QUN,
                    3,
                    POOL_YELLOW_TURBAN_1,
                    new Wave(
                            enemy("周倉", 650, 54, 4, GameData.QUN, 5, true, true),
                            enemy("黃巾槍兵", 220, 15, 2, GameData.QUN, 0, true, true),
                            enemy("黃巾槍兵", 220, 15, 2, GameData.QUN, 0, true, true))),

            baseStage(
                    1,
                    "要衝之地",
                    "曹操軍編・鐵門峽之戰",
                    2, 5, 340, 190,
                    "原作：3合戰；B3 張寶＋黃巾槍兵×2。非B3從該任務記錄敵人生成。",
                    ADV_HAN_WEI_QUN_CYCLE,
                    3,
                    POOL_IRON_GATE_1,
                    new Wave(
                            enemy("張寶", 1500, 130, 3, GameData.QUN, 25, false, false),
                            enemy("黃巾槍兵", 520, 51, 2, GameData.QUN, 10, true, true),
                            enemy("黃巾槍兵", 520, 51, 2, GameData.QUN, 10, true, true))),

            baseStage(
                    2,
                    "黃巾本陣",
                    "曹操軍編・廣宗之戰",
                    4, 5, 470, 340,
                    "原作：5合戰；B5 周倉 TURN3 / ATK201。B1～B4從共通出現池生成。",
                    ADV_WEI_QUN_HAN,
                    5,
                    POOL_GUANGZONG,
                    new Wave(
                            enemy("周倉", 2200, 201, 3, GameData.QUN, 50, true, true)))
    };

    private StageData() {}

    public static Stage materializeRun(Stage base, long seed) {
        if (base == null) base = STAGES[0];
        if (base.isMaterialized()) return base;

        Random random = new Random(seed);
        Wave[] waves = new Wave[base.waveCount];

        for (int i = 0; i < base.waveCount - 1; i++) {
            waves[i] = base.commonPool.roll(random);
        }

        waves[base.waveCount - 1] = base.fixedBossWave;

        return new Stage(
                base.id,
                base.name,
                base.chapter,
                base.difficulty,
                base.stamina,
                base.coinReward,
                base.expReward,
                base.sourceNote,
                base.advantagePairs,
                base.waveCount,
                base.commonPool,
                base.fixedBossWave,
                waves,
                seed);
    }

    public static String unitName(int unit) {
        if (unit == 5) return "桃";
        return GameData.factionName(unit);
    }

    public static Stage get(int index) {
        if (index < 0) return STAGES[0];
        if (index >= STAGES.length) return STAGES[STAGES.length - 1];
        return STAGES[index];
    }
}
