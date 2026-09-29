package com.openai.threekingdoms;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.os.SystemClock;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

public class PuzzleBoardView extends View {
    public interface BattleListener {
        void onBattleResolved(
                int wave,
                String enemyName,
                int enemyHp,
                int enemyMaxHp,
                int playerHp,
                int playerMaxHp,
                int enemyTurns,
                int combos,
                int totalDamage,
                int heal,
                int[] generalDamage,
                int[] skillCooldowns,
                int skillSealTurns,
                boolean waveCleared,
                boolean victory,
                boolean gameOver,
                String message);
    }

    private static final int ROWS = 5;
    private static final int COLS = 6;
    private static final int TYPES = 6;
    private static final int PEACH = 5;
    private static final long BASE_MOVE_MS = 5000L;
    private static final long ZHAO_YUN_MOVE_MS = 10000L;

    private final int[][] board = new int[ROWS][COLS];
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int[] teamIds = GameData.defaultTeam();
    private final GameData.General[] team = new GameData.General[5];
    private final int[] teamAtk = new int[5];
    private final int[] skillCooldown = new int[5];
    private int teamRecovery;

    private boolean dragging;
    private long dragStartAt;
    private long dragEndAt;
    private long dragDurationMs = BASE_MOVE_MS;
    private long nextMoveDurationMs;
    private final Runnable dragTimeoutRunnable = () -> {
        if (dragging) {
            finishDrag(true);
        }
    };

    private BattleListener listener;
    private StageData.Stage stage = StageData.get(0);

    private int selectedRow = -1;
    private int selectedCol = -1;

    private int playerMaxHp;
    private int playerHp;
    private int waveIndex;
    private int[] enemyHp = new int[0];
    private int[] enemyTurnsRemaining = new int[0];
    private int[] enemyActionIndex = new int[0];
    private int[] enemyPowerAttacksRemaining = new int[0];
    private float[] enemyPowerMultiplier = new float[0];
    private int selectedEnemyIndex;
    private int skillSealTurns;
    private String waveStartMessage = "";
    private boolean victory;
    private boolean gameOver;

    private final int[] colors = {
            Color.rgb(65, 128, 208),   // 魏：藍
            Color.rgb(205, 76, 70),    // 吳：紅
            Color.rgb(72, 164, 95),    // 蜀：綠
            Color.rgb(137, 86, 184),   // 漢：紫
            Color.rgb(105, 105, 105),  // 群：灰
            Color.rgb(214, 104, 151)   // 桃：回復
    };

    private final String[] symbols = {"魏", "吳", "蜀", "漢", "群", "桃"};

    public PuzzleBoardView(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(18, 16, 15));
        setTeam(GameData.defaultTeam(), new int[]{1, 1, 1, 1, 1});
        fillRandom();
    }

    public void setBattleListener(BattleListener listener) {
        this.listener = listener;
    }

    public void setStage(StageData.Stage stage) {
        this.stage = stage == null ? StageData.get(0) : stage;
    }

    public void setTeam(int[] ids, int[] levels) {
        playerMaxHp = 0;
        teamRecovery = 0;

        for (int i = 0; i < team.length; i++) {
            int id = (ids != null && i < ids.length)
                    ? ids[i]
                    : GameData.defaultTeam()[i];
            int level = (levels != null && i < levels.length)
                    ? Math.max(1, levels[i])
                    : 1;

            teamIds[i] = id;
            team[i] = GameData.get(id);
            teamAtk[i] = PlayerData.effectiveAtkAtLevel(team[i], level)
                    + PlayerData.weaponAtkBonus(getContext(), id);
            teamRecovery += PlayerData.effectiveRecoveryAtLevel(team[i], level);
        }

        if (team[0] != null) {
            for (int i = 0; i < team.length; i++) {
                int level = (levels != null && i < levels.length)
                        ? Math.max(1, levels[i])
                        : 1;
                playerMaxHp += Math.round(
                        PlayerData.effectiveHpAtLevel(team[i], level)
                                * team[0].leaderHpMultiplier(team[i]));
            }
        }
    }

    private StageData.Wave currentWave() {
        return stage.waves[waveIndex];
    }

    private void initWaveState() {
        StageData.Enemy[] enemies = currentWave().enemies;
        enemyHp = new int[enemies.length];
        enemyTurnsRemaining = new int[enemies.length];
        enemyActionIndex = new int[enemies.length];
        enemyPowerAttacksRemaining = new int[enemies.length];
        enemyPowerMultiplier = new float[enemies.length];

        StringBuilder preemptiveLog = new StringBuilder();

        for (int i = 0; i < enemies.length; i++) {
            enemyHp[i] = enemies[i].maxHp;
            enemyTurnsRemaining[i] = enemies[i].interval;
            enemyActionIndex[i] = 0;
            enemyPowerAttacksRemaining[i] = 0;
            enemyPowerMultiplier[i] = 1.0f;
        }

        selectedEnemyIndex = 0;
        ensureLivingTarget();

        for (int i = 0; i < enemies.length; i++) {
            StageData.EnemyAction preemptive = enemies[i].preemptive;
            if (preemptive == null || !isEnemyAlive(i)) continue;

            String log = executeEnemyAction(i, preemptive, 0, true);
            if (!log.isEmpty()) {
                if (preemptiveLog.length() > 0) preemptiveLog.append("｜");
                preemptiveLog.append(log);
            }

            if (playerHp <= 0) {
                gameOver = true;
                break;
            }
        }

        waveStartMessage = preemptiveLog.toString();
    }

    private boolean isEnemyAlive(int index) {
        return index >= 0
                && index < enemyHp.length
                && enemyHp[index] > 0;
    }

    private boolean allEnemiesDefeated() {
        for (int hp : enemyHp) {
            if (hp > 0) return false;
        }
        return true;
    }

    private int firstLivingEnemy() {
        for (int i = 0; i < enemyHp.length; i++) {
            if (enemyHp[i] > 0) return i;
        }
        return -1;
    }

    private void ensureLivingTarget() {
        if (isEnemyAlive(selectedEnemyIndex)) return;
        int living = firstLivingEnemy();
        selectedEnemyIndex = living >= 0 ? living : 0;
    }

    private StageData.Enemy selectedEnemy() {
        ensureLivingTarget();
        return currentWave().enemies[selectedEnemyIndex];
    }

    public void cycleTarget() {
        if (victory || gameOver || enemyHp.length <= 1) {
            return;
        }

        int start = selectedEnemyIndex;
        int index = start;

        do {
            index = (index + 1) % enemyHp.length;
            if (isEnemyAlive(index)) {
                selectedEnemyIndex = index;
                notifyState(0, 0, 0, new int[5], false, "切換目標");
                invalidate();
                return;
            }
        } while (index != start);
    }

    public void resetGame() {
        playerHp = playerMaxHp;
        waveIndex = 0;
        victory = false;
        gameOver = false;
        skillSealTurns = 0;
        waveStartMessage = "";
        dragging = false;
        nextMoveDurationMs = 0L;
        removeCallbacks(dragTimeoutRunnable);
        selectedRow = -1;
        selectedCol = -1;

        for (int i = 0; i < skillCooldown.length; i++) {
            skillCooldown[i] = 0;
        }

        do {
            fillRandom();
        } while (hasAnyMatch());

        initWaveState();

        String message = "戰鬥開始";
        if (!waveStartMessage.isEmpty()) {
            message += "｜" + waveStartMessage;
        }

        notifyState(0, 0, 0, new int[5], false, message);
        invalidate();
    }

    private int dealDamageToEnemy(
            int enemyIndex,
            int rawDamage,
            int defenseReductionPercent,
            boolean ignoreDefense) {

        if (!isEnemyAlive(enemyIndex)) return 0;

        StageData.Enemy enemy = currentWave().enemies[enemyIndex];

        int defense = ignoreDefense
                ? 0
                : Math.max(
                        0,
                        Math.round(
                                enemy.defense
                                        * (100 - defenseReductionPercent)
                                        / 100f));

        int dealt = Math.max(1, rawDamage - defense);
        enemyHp[enemyIndex] = Math.max(0, enemyHp[enemyIndex] - dealt);

        if (enemyHp[enemyIndex] <= 0
                && enemyIndex == selectedEnemyIndex) {
            ensureLivingTarget();
        }

        return dealt;
    }

    private int bowDefenseReductionPercent(int stage) {
        switch (stage) {
            case 1: return 20;
            case 2: return 25;
            case 3: return 50;
            default: return 0;
        }
    }

    private int barbarianAttackReductionPercent(int stage) {
        switch (stage) {
            case 1: return 10;
            case 2: return 20;
            case 3: return 30;
            default: return 0;
        }
    }

    private String executeEnemyAction(
            int enemyIndex,
            StageData.EnemyAction action,
            int barbarianReduction,
            boolean preemptive) {

        if (!isEnemyAlive(enemyIndex) || action == null) return "";

        StageData.Enemy enemy = currentWave().enemies[enemyIndex];
        String prefix = enemy.name + "「" + action.name + "」";

        switch (action.type) {
            case StageData.AI_SCOUT:
                return prefix + "：沒有攻擊";

            case StageData.AI_POWER_UP:
                enemyPowerMultiplier[enemyIndex] =
                        Math.max(1.0f, action.value / 100f);
                enemyPowerAttacksRemaining[enemyIndex] =
                        Math.max(1, action.aux);
                return prefix + "：接下來"
                        + enemyPowerAttacksRemaining[enemyIndex]
                        + "次攻擊 ×"
                        + enemyPowerMultiplier[enemyIndex];

            case StageData.AI_CONVERT:
                int converted = convertBoard(action.value, action.aux);
                return prefix + "："
                        + StageData.unitName(action.value)
                        + "→"
                        + StageData.unitName(action.aux)
                        + "，轉換 " + converted + " 個";

            case StageData.AI_SKILL_SEAL:
                skillSealTurns = Math.max(skillSealTurns, action.value);
                return prefix + "：全員技能封印 "
                        + action.value + " 回合";

            case StageData.AI_ATTACK:
            default:
                float powerMultiplier =
                        enemyPowerAttacksRemaining[enemyIndex] > 0
                                ? enemyPowerMultiplier[enemyIndex]
                                : 1.0f;

                float leaderIncoming =
                        team[0].leaderIncomingMultiplier(enemy.faction);

                int incoming = Math.round(
                        enemy.attack
                                * (Math.max(1, action.value) / 100f)
                                * powerMultiplier
                                * leaderIncoming
                                * (100 - barbarianReduction)
                                / 100f);

                playerHp = Math.max(0, playerHp - incoming);

                if (enemyPowerAttacksRemaining[enemyIndex] > 0) {
                    enemyPowerAttacksRemaining[enemyIndex]--;
                    if (enemyPowerAttacksRemaining[enemyIndex] <= 0) {
                        enemyPowerMultiplier[enemyIndex] = 1.0f;
                    }
                }

                if (playerHp <= 0) {
                    gameOver = true;
                }

                return prefix
                        + (preemptive ? "（先制）" : "")
                        + "：" + incoming + " 傷害";
        }
    }

    private String executeNextEnemyAction(
            int enemyIndex,
            int barbarianReduction) {

        StageData.Enemy enemy = currentWave().enemies[enemyIndex];
        StageData.EnemyAction[] actions = enemy.actions;

        if (actions == null || actions.length == 0) {
            return "";
        }

        int index = enemyActionIndex[enemyIndex] % actions.length;
        StageData.EnemyAction action = actions[index];
        enemyActionIndex[enemyIndex] =
                (enemyActionIndex[enemyIndex] + 1) % actions.length;

        return executeEnemyAction(
                enemyIndex,
                action,
                barbarianReduction,
                false);
    }

    public boolean useSkill(int index) {
        if (index < 0 || index >= team.length || victory || gameOver) {
            return false;
        }

        GameData.General g = team[index];

        if (skillSealTurns > 0) {
            notifyState(
                    0,
                    0,
                    0,
                    new int[5],
                    false,
                    "全員技能封印中，尚餘 " + skillSealTurns + " 回合");
            return false;
        }

        if (skillCooldown[index] > 0) {
            notifyState(
                    0,
                    0,
                    0,
                    new int[5],
                    false,
                    g.skillName + "尚需 " + skillCooldown[index] + " 回合");
            return false;
        }

        int damage = 0;
        int heal = 0;
        String message;

        ensureLivingTarget();

        if (g.skillType == GameData.SKILL_HEAL) {
            float leaderHeal = team[0].leaderHealMultiplier();
            heal = Math.round(g.skillValue * leaderHeal);
            playerHp = Math.min(playerMaxHp, playerHp + heal);
            message = g.name + "施放" + g.skillName + "：回復 " + heal + " HP";
        } else if (g.skillType == GameData.SKILL_DAMAGE) {
            StageData.Enemy enemy = selectedEnemy();
            float multiplier = factionMultiplier(g.faction, enemy.faction);
            float leaderDamage = team[0].leaderDamageMultiplier(
                    g, 0, playerHp, playerMaxHp);
            int raw = Math.round(g.skillValue * multiplier * leaderDamage);
            damage = dealDamageToEnemy(selectedEnemyIndex, raw, 0, false);
            message = g.name + "施放" + g.skillName
                    + "：" + multiplierText(multiplier)
                    + "造成 " + damage + " 傷害";
        } else if (g.skillType == GameData.SKILL_SELF_ATK) {
            StageData.Enemy enemy = selectedEnemy();
            float multiplier = factionMultiplier(g.faction, enemy.faction);
            float leaderDamage = team[0].leaderDamageMultiplier(
                    g, 0, playerHp, playerMaxHp);
            int raw = Math.round(
                    teamAtk[index] * g.skillValue * multiplier * leaderDamage);
            boolean ignoreDefense = g.id == 7;
            damage = dealDamageToEnemy(
                    selectedEnemyIndex,
                    raw,
                    ignoreDefense ? 100 : 0,
                    ignoreDefense);
            message = g.name + "施放" + g.skillName
                    + "：" + g.skillValue + "倍自身攻擊，"
                    + multiplierText(multiplier)
                    + "造成 " + damage + " 傷害";
        } else if (g.skillType == GameData.SKILL_TEAM_FACTION_ATK) {
            int factionAtk = 0;
            for (int i = 0; i < team.length; i++) {
                if (team[i].faction == g.skillAux) {
                    float leaderDamage = team[0].leaderDamageMultiplier(
                            team[i], 0, playerHp, playerMaxHp);
                    factionAtk += Math.round(teamAtk[i] * leaderDamage);
                }
            }

            int hitCount = 0;
            for (int e = 0; e < currentWave().enemies.length; e++) {
                if (!isEnemyAlive(e)) continue;
                StageData.Enemy enemy = currentWave().enemies[e];
                float multiplier = factionMultiplier(g.skillAux, enemy.faction);
                int raw = Math.round(factionAtk * g.skillValue * multiplier);
                damage += dealDamageToEnemy(e, raw, 0, false);
                hitCount++;
            }

            message = g.name + "施放" + g.skillName
                    + "：全隊" + factionName(g.skillAux)
                    + "攻擊合計×" + g.skillValue
                    + "，對 " + hitCount + " 名敵人造成合計 "
                    + damage + " 傷害";
        } else if (g.skillType == GameData.SKILL_CONVERT) {
            int converted = convertBoard(g.skillValue, g.skillAux);
            message = g.name + "施放" + g.skillName
                    + "：" + factionName(g.skillValue)
                    + "→" + factionName(g.skillAux)
                    + "，轉換 " + converted + " 個單位";
        } else if (g.skillType == GameData.SKILL_FREE_MOVE) {
            nextMoveDurationMs = ZHAO_YUN_MOVE_MS;
            message = g.name + "施放" + g.skillName
                    + "：下一次拖珠可自由移動 " + g.skillValue + " 秒";
        } else {
            for (int e = 0; e < enemyTurnsRemaining.length; e++) {
                if (isEnemyAlive(e)) {
                    enemyTurnsRemaining[e] += g.skillValue;
                }
            }
            message = g.name + "施放" + g.skillName
                    + "：全體敵軍攻擊延後 " + g.skillValue + " 回合";
        }

        skillCooldown[index] = g.skillCd;

        boolean waveCleared = advanceIfEnemyDefeated();
        notifyState(0, damage, heal, new int[5], waveCleared, message);
        invalidate();
        return true;
    }

    private boolean advanceIfEnemyDefeated() {
        if (!allEnemiesDefeated()) {
            ensureLivingTarget();
            return false;
        }

        if (waveIndex >= stage.waves.length - 1) {
            victory = true;
            return false;
        }

        waveIndex++;
        initWaveState();
        return true;
    }

    private int convertBoard(int from, int to) {
        int converted = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] == from) {
                    board[r][c] = to;
                    converted++;
                }
            }
        }
        invalidate();
        return converted;
    }

    private void fillRandom() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = random.nextInt(TYPES);
            }
        }
    }

    private String factionName(int faction) {
        return GameData.factionName(faction);
    }

    private float factionMultiplier(int attacker, int defender) {
        if (stage.isAdvantaged(attacker, defender)) {
            return 2.0f;
        }

        if (stage.isAdvantaged(defender, attacker)) {
            return 0.5f;
        }

        return 1.0f;
    }

    private String multiplierText(float multiplier) {
        if (multiplier > 1.0f) return "勢力有利 ×2，";
        if (multiplier < 1.0f) return "勢力不利 ×0.5，";
        return "";
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = Math.round(w * (ROWS / (float) COLS));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cw = getWidth() / (float) COLS;
        float ch = getHeight() / (float) ROWS;
        float gap = Math.max(3f, cw * 0.035f);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                RectF rect = new RectF(
                        c * cw + gap,
                        r * ch + gap,
                        (c + 1) * cw - gap,
                        (r + 1) * ch - gap);

                paint.setColor(colors[board[r][c]]);
                canvas.drawRoundRect(rect, cw * 0.18f, cw * 0.18f, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(cw * 0.32f);
                paint.setTextAlign(Paint.Align.CENTER);
                Paint.FontMetrics fm = paint.getFontMetrics();
                float y = rect.centerY() - (fm.ascent + fm.descent) / 2f;
                canvas.drawText(symbols[board[r][c]], rect.centerX(), y, paint);

                if (r == selectedRow && c == selectedCol) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(4f, cw * 0.06f));
                    paint.setColor(Color.WHITE);
                    canvas.drawRoundRect(rect, cw * 0.18f, cw * 0.18f, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }
        }

        if (dragging) {
            long remaining = Math.max(0L, dragEndAt - SystemClock.uptimeMillis());
            float fraction = Math.max(0f, Math.min(1f,
                    remaining / (float) Math.max(1L, dragDurationMs)));

            float barHeight = Math.max(10f, getHeight() * 0.025f);
            paint.setColor(Color.argb(180, 0, 0, 0));
            canvas.drawRect(0, 0, getWidth(), barHeight, paint);
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, getWidth() * fraction, barHeight, paint);

            paint.setTextSize(Math.max(22f, cw * 0.18f));
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(
                    String.format(java.util.Locale.US, "%.1f s", remaining / 1000f),
                    getWidth() - gap,
                    barHeight + Math.max(24f, cw * 0.22f),
                    paint);

            if (remaining > 0) {
                postInvalidateDelayed(50L);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (victory || gameOver) {
            return true;
        }

        int c = clamp(
                (int) (event.getX() / (getWidth() / (float) COLS)),
                0,
                COLS - 1);
        int r = clamp(
                (int) (event.getY() / (getHeight() / (float) ROWS)),
                0,
                ROWS - 1);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startDrag(r, c);
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!dragging) {
                return true;
            }

            if (SystemClock.uptimeMillis() >= dragEndAt) {
                finishDrag(true);
                return true;
            }

            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }

            moveAlongPath(r, c);
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (dragging) {
                finishDrag(false);
            }
            return true;
        }

        return true;
    }

    private void startDrag(int row, int col) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        removeCallbacks(dragTimeoutRunnable);
        dragging = true;
        selectedRow = row;
        selectedCol = col;
        dragDurationMs = nextMoveDurationMs > 0L
                ? nextMoveDurationMs
                : BASE_MOVE_MS;
        nextMoveDurationMs = 0L;
        dragStartAt = SystemClock.uptimeMillis();
        dragEndAt = dragStartAt + dragDurationMs;
        postDelayed(dragTimeoutRunnable, dragDurationMs);
        invalidate();
    }

    private void moveAlongPath(int targetRow, int targetCol) {
        while (dragging
                && (selectedRow != targetRow || selectedCol != targetCol)) {

            int dr = targetRow - selectedRow;
            int dc = targetCol - selectedCol;

            int nextRow = selectedRow;
            int nextCol = selectedCol;

            if (Math.abs(dc) >= Math.abs(dr) && dc != 0) {
                nextCol += dc > 0 ? 1 : -1;
            } else if (dr != 0) {
                nextRow += dr > 0 ? 1 : -1;
            }

            int tmp = board[selectedRow][selectedCol];
            board[selectedRow][selectedCol] = board[nextRow][nextCol];
            board[nextRow][nextCol] = tmp;
            selectedRow = nextRow;
            selectedCol = nextCol;
        }

        invalidate();
    }

    private void finishDrag(boolean timedOut) {
        if (!dragging) {
            return;
        }

        dragging = false;
        removeCallbacks(dragTimeoutRunnable);

        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }

        selectedRow = -1;
        selectedCol = -1;
        resolveBoard(timedOut);
        invalidate();
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void resolveBoard(boolean timedOut) {
        if (skillSealTurns > 0) {
            skillSealTurns--;
        }

        int totalCombos = 0;
        int[] totalMatchedByType = new int[TYPES];
        int[] totalGroupsByType = new int[TYPES];
        int guard = 0;

        while (guard++ < 12) {
            MatchResult result = findMatches();
            if (result.matchedCount == 0) {
                break;
            }

            totalCombos += result.combos;
            for (int i = 0; i < TYPES; i++) {
                totalMatchedByType[i] += result.matchedByType[i];
                totalGroupsByType[i] += result.groupsByType[i];
            }

            clearMatches(result.matched);
            collapseAndRefill();
        }

        float comboMultiplier =
                1f + Math.max(0, totalCombos - 1) * 0.25f;

        FinisherRules.Result finisher =
                FinisherRules.compute(team, totalGroupsByType);

        int bowReduction =
                bowDefenseReductionPercent(
                        finisher.stage[GameData.BOW]);

        int barbarianReduction =
                barbarianAttackReductionPercent(
                        finisher.stage[GameData.BARBARIAN]);

        int[] generalDamage = new int[5];
        int totalDamage = 0;

        for (int i = 0; i < team.length; i++) {
            int faction = team[i].faction;
            float unitFactor = unitMatchFactor(
                    totalMatchedByType[faction],
                    totalGroupsByType[faction]);

            if (unitFactor <= 0f) {
                continue;
            }

            float leaderDamage =
                    team[0].leaderDamageMultiplier(
                            team[i],
                            totalCombos,
                            playerHp,
                            playerMaxHp);

            float finisherMultiplier =
                    finisher.damageMultiplier[team[i].troopType];

            float baseRaw =
                    teamAtk[i]
                            * leaderDamage
                            * comboMultiplier
                            * unitFactor
                            * finisherMultiplier;

            boolean cavalryAllTarget =
                    team[i].troopType == GameData.CAVALRY
                            && finisher.stage[GameData.CAVALRY] > 0;

            int dealtByGeneral = 0;

            if (cavalryAllTarget) {
                for (int e = 0; e < enemyHp.length; e++) {
                    if (!isEnemyAlive(e)) continue;

                    StageData.Enemy enemy = currentWave().enemies[e];
                    float multiplier =
                            factionMultiplier(faction, enemy.faction);

                    int raw = Math.round(baseRaw * multiplier);
                    dealtByGeneral += dealDamageToEnemy(
                            e,
                            raw,
                            bowReduction,
                            false);
                }
            } else {
                ensureLivingTarget();

                if (firstLivingEnemy() >= 0) {
                    StageData.Enemy enemy = selectedEnemy();
                    float multiplier =
                            factionMultiplier(faction, enemy.faction);

                    int raw = Math.round(baseRaw * multiplier);
                    dealtByGeneral = dealDamageToEnemy(
                            selectedEnemyIndex,
                            raw,
                            bowReduction,
                            false);
                }
            }

            generalDamage[i] = dealtByGeneral;
            totalDamage += dealtByGeneral;
        }

        float peachFactor = unitMatchFactor(
                totalMatchedByType[PEACH],
                totalGroupsByType[PEACH]);

        int heal = peachFactor <= 0f
                ? 0
                : Math.round(
                        teamRecovery
                                * comboMultiplier
                                * peachFactor
                                * team[0].leaderHealMultiplier());

        if (heal > 0) {
            playerHp = Math.min(playerMaxHp, playerHp + heal);
        }

        boolean waveCleared = advanceIfEnemyDefeated();

        StringBuilder enemyActionLog = new StringBuilder();

        if (!victory && !waveCleared) {
            StageData.Enemy[] enemies = currentWave().enemies;

            for (int e = 0; e < enemies.length; e++) {
                if (!isEnemyAlive(e)) continue;

                enemyTurnsRemaining[e]--;

                if (enemyTurnsRemaining[e] <= 0) {
                    String actionLog =
                            executeNextEnemyAction(e, barbarianReduction);

                    if (!actionLog.isEmpty()) {
                        if (enemyActionLog.length() > 0) {
                            enemyActionLog.append("｜");
                        }
                        enemyActionLog.append(actionLog);
                    }

                    enemyTurnsRemaining[e] = enemies[e].interval;

                    if (gameOver) {
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < skillCooldown.length; i++) {
            if (skillCooldown[i] > 0) {
                skillCooldown[i]--;
            }
        }

        String baseMessage = waveCleared
                ? "敵軍擊破，進入下一波"
                : (timedOut ? "操作時間終了" : "回合結算");

        String finisherSummary = finisher.summary();
        if (!finisherSummary.isEmpty()) {
            baseMessage += "｜必殺：" + finisherSummary;
        }

        if (bowReduction > 0) {
            baseMessage += "｜弓兵降防 " + bowReduction + "%";
        }

        if (barbarianReduction > 0) {
            baseMessage += "｜蠻兵降攻 " + barbarianReduction + "%";
        }

        if (enemyActionLog.length() > 0) {
            baseMessage += "｜AI：" + enemyActionLog;
        }

        if (waveCleared && !waveStartMessage.isEmpty()) {
            baseMessage += "｜" + waveStartMessage;
        }

        notifyState(
                totalCombos,
                totalDamage,
                heal,
                generalDamage,
                waveCleared,
                baseMessage);
    }

    private float unitMatchFactor(int matchedUnits, int groups) {
        if (matchedUnits < 3 || groups <= 0) {
            return 0f;
        }

        int excess = Math.max(0, matchedUnits - groups * 3);
        return groups + excess / 4f;
    }

    private String enemySummary() {
        StringBuilder sb = new StringBuilder();
        StageData.Enemy[] enemies = currentWave().enemies;

        for (int i = 0; i < enemies.length; i++) {
            if (i > 0) sb.append("\n");

            StageData.Enemy enemy = enemies[i];
            boolean selected = i == selectedEnemyIndex && enemyHp[i] > 0;

            sb.append(selected ? "▶ " : "　")
                    .append(enemy.name)
                    .append(" [")
                    .append(factionName(enemy.faction))
                    .append("] ");

            if (enemyHp[i] <= 0) {
                sb.append("擊破");
            } else {
                sb.append("HP ")
                        .append(enemyHp[i])
                        .append(enemy.statMark())
                        .append("/")
                        .append(enemy.maxHp)
                        .append(enemy.statMark())
                        .append("　ATK ")
                        .append(enemy.attack)
                        .append(enemy.attackMark())
                        .append("　DEF ")
                        .append(enemy.defense)
                        .append(enemy.statMark())
                        .append("　TURN ")
                        .append(enemyTurnsRemaining[i]);
            }
        }

        return sb.toString();
    }

    private void notifyState(
            int combos,
            int damage,
            int heal,
            int[] generalDamage,
            boolean waveCleared,
            String message) {

        if (listener == null) {
            return;
        }

        ensureLivingTarget();

        StageData.Enemy selected = currentWave().enemies[selectedEnemyIndex];
        int selectedHp = enemyHp.length == 0
                ? 0
                : enemyHp[selectedEnemyIndex];
        int selectedTurn = enemyTurnsRemaining.length == 0
                ? 0
                : enemyTurnsRemaining[selectedEnemyIndex];

        listener.onBattleResolved(
                waveIndex + 1,
                enemySummary(),
                selectedHp,
                selected.maxHp,
                playerHp,
                playerMaxHp,
                selectedTurn,
                combos,
                damage,
                heal,
                generalDamage.clone(),
                skillCooldown.clone(),
                skillSealTurns,
                waveCleared,
                victory,
                gameOver,
                message);
    }

    private boolean hasAnyMatch() {
        return findMatches().matchedCount > 0;
    }

    private MatchResult findMatches() {
        boolean[][] matched = new boolean[ROWS][COLS];

        for (int r = 0; r < ROWS; r++) {
            int start = 0;

            for (int c = 1; c <= COLS; c++) {
                if (c == COLS
                        || board[r][c] != board[r][start]) {

                    if (c - start >= 3) {
                        for (int k = start; k < c; k++) {
                            matched[r][k] = true;
                        }
                    }

                    start = c;
                }
            }
        }

        for (int c = 0; c < COLS; c++) {
            int start = 0;

            for (int r = 1; r <= ROWS; r++) {
                if (r == ROWS
                        || board[r][c] != board[start][c]) {

                    if (r - start >= 3) {
                        for (int k = start; k < r; k++) {
                            matched[k][c] = true;
                        }
                    }

                    start = r;
                }
            }
        }

        int count = 0;
        int[] matchedByType = new int[TYPES];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (matched[r][c]) {
                    count++;
                    matchedByType[board[r][c]]++;
                }
            }
        }

        int[] groupsByType = new int[TYPES];
        int combos = countConnectedGroups(matched, groupsByType);

        return new MatchResult(
                matched,
                count,
                combos,
                matchedByType,
                groupsByType);
    }

    private int countConnectedGroups(
            boolean[][] matched,
            int[] groupsByType) {
        boolean[][] seen = new boolean[ROWS][COLS];
        int groups = 0;

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!matched[r][c] || seen[r][c]) {
                    continue;
                }

                groups++;
                int type = board[r][c];
                groupsByType[type]++;

                Queue<int[]> q = new ArrayDeque<>();
                q.add(new int[]{r, c});
                seen[r][c] = true;

                while (!q.isEmpty()) {
                    int[] point = q.remove();

                    for (int i = 0; i < 4; i++) {
                        int nr = point[0] + dr[i];
                        int nc = point[1] + dc[i];

                        if (nr < 0
                                || nr >= ROWS
                                || nc < 0
                                || nc >= COLS) {
                            continue;
                        }

                        if (!seen[nr][nc]
                                && matched[nr][nc]
                                && board[nr][nc] == type) {

                            seen[nr][nc] = true;
                            q.add(new int[]{nr, nc});
                        }
                    }
                }
            }
        }

        return groups;
    }

    private void clearMatches(boolean[][] matched) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (matched[r][c]) {
                    board[r][c] = -1;
                }
            }
        }
    }

    private void collapseAndRefill() {
        for (int c = 0; c < COLS; c++) {
            int write = ROWS - 1;

            for (int r = ROWS - 1; r >= 0; r--) {
                if (board[r][c] >= 0) {
                    board[write--][c] = board[r][c];
                }
            }

            while (write >= 0) {
                board[write--][c] = random.nextInt(TYPES);
            }
        }
    }

    private static final class MatchResult {
        final boolean[][] matched;
        final int matchedCount;
        final int combos;
        final int[] matchedByType;
        final int[] groupsByType;

        MatchResult(
                boolean[][] matched,
                int matchedCount,
                int combos,
                int[] matchedByType,
                int[] groupsByType) {

            this.matched = matched;
            this.matchedCount = matchedCount;
            this.combos = combos;
            this.matchedByType = matchedByType;
            this.groupsByType = groupsByType;
        }
    }
}
