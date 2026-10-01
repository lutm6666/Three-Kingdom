package com.openai.threekingdoms.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.openai.threekingdoms.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Test APK only. Fixes encounters/boards; never injects a victory or reward. */
public final class BattleRegression extends Instrumentation {
    private MainActivity activity;
    private Throwable failure;
    private StageData.Stage stage;

    private boolean probe;
    private boolean skills;
    private boolean saves;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); probe=arguments != null && "playability".equals(arguments.getString("suite")); skills=arguments != null && "skills".equals(arguments.getString("suite")); saves=arguments != null && "saves".equals(arguments.getString("suite")); start(); }
    @Override public void onStart() {
        if (saves) { new SaveRegression(this).run(); return; }
        if (skills) { new SkillRegression(this).run(); return; }
        if (probe) { new PlayabilityProbe(this).run(); return; }
        Bundle result = new Bundle();
        try {
            Context target = getTargetContext();
            target.getSharedPreferences("game", 0).edit().clear().commit();
            SharedPreferences progress = target.getSharedPreferences("progress", 0);
            SharedPreferences.Editor seed = progress.edit().clear();
            for (int id : GameData.defaultTeam()) seed.putInt("level_"+id, 10);
            check(seed.commit(), "failed to create test save");
            activity = (MainActivity) startActivitySync(new Intent(target, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            StageData.Stage[] fixtures = new StageData.Stage[3];
            int[] coins = {70, 340, 470}, experience = {30, 190, 340};
            for (int index=0; index<fixtures.length; index++) {
                check(StageData.get(index).coinReward == coins[index]
                        && StageData.get(index).expReward == experience[index], "Master reward mismatch");
                for (long encounterSeed=0; encounterSeed<100; encounterSeed++) {
                    StageData.Stage candidate = StageData.materializeRun(StageData.get(index), encounterSeed);
                    if (DropData.roll(candidate).length > 0) { fixtures[index]=candidate; break; }
                }
                check(fixtures[index] != null, "could not select drop fixture for stage " + index);
            }
            Map<String,Integer> drops = new HashMap<>();
            int expectedCoins=0, totalExp=0, highest=0;
            // Clear each stage in order, then replay earlier stages after all are unlocked.
            for (int run=0; run<6; run++) {
                int index=run%3;
                stage=fixtures[index];
                startBattle();
                PuzzleBoardView board = (PuzzleBoardView) get(activity, "board");
                int previousWave=0;
                for (int turn=0;turn<80 && !(Boolean)get(board,"victory");turn++) {
                    main(() -> {
                        int[][] cells=(int[][])get(board,"board");
                        for (int[] row:cells) java.util.Arrays.fill(row, GameData.SHU);
                        ((Random)get(board,"random")).setSeed(12345L);
                        touch(board);
                    });
                    waitForIdleSync();
                    check(!(Boolean)get(board,"gameOver"), "party lost at stage " + index);
                    int wave=(Integer)get(board,"waveIndex");
                    check(wave == previousWave || wave == previousWave+1, "skipped a wave at stage " + index);
                    previousWave=wave;
                }
                check((Boolean)get(board,"victory"), "did not clear stage " + index);
                check(previousWave == stage.waveCount-1, "not all waves cleared");
                main(() -> check(hasText(activity.getWindow().getDecorView(), "通關獎勵"), "result screen missing"));
                expectedCoins += coins[index];
                totalExp += experience[index];
                check(PlayerData.getCoins(activity)==expectedCoins,"coin amount mismatch at stage " + index);
                int level=10, exp=totalExp;
                // Independent expected progression for the reconstructed EXP curve.
                while (exp >= 100+(level-1)*40) { exp -= 100+(level-1)*40; level++; }
                for (int id:GameData.defaultTeam()) {
                    check(PlayerData.getLevel(activity,id)==level,"level amount mismatch");
                    check(PlayerData.getExp(activity,id)==exp,"EXP remainder mismatch");
                }
                highest=Math.max(highest, Math.min(index+1, fixtures.length-1));
                check(PlayerData.getHighestUnlockedStage(activity)==highest,"unlock regressed or exceeded final stage");
                for (DropData.Item item : DropData.roll(stage))
                    drops.put(item.id, drops.getOrDefault(item.id,0)+1);
                for (DropData.Item item:DropData.ITEMS)
                    check(PlayerData.getLootCount(activity,item.id)==drops.getOrDefault(item.id,0),"drop mismatch: "+item.id);
                Map<String,?> once=new HashMap<>(progress.getAll());
                main(() -> activity.onBattleResolved(stage.waveCount,"duplicate",0,1,1,1,0,0,0,0,
                        new int[5],new int[5],0,true,true,false,"duplicate"));
                waitForIdleSync();
                check(once.equals(progress.getAll()),"duplicate callback granted rewards twice");
                android.util.Log.i("BattleRegression", "PASS: stage " + index + " run " + (run/3+1)
                        + " waves=" + stage.waveCount + " coins=" + expectedCoins + " level=" + level + " exp=" + exp);
            }
            for (StageData.Stage fixture : fixtures) {
                stage=fixture;
                Map<String,?> beforeLoss=new HashMap<>(progress.getAll());
                startBattle();
                PuzzleBoardView board=(PuzzleBoardView)get(activity,"board");
                main(() -> {
                    set(board,"playerHp",1);
                    java.util.Arrays.fill((int[])get(board,"enemyTurnsRemaining"),1);
                    int[][] cells=(int[][])get(board,"board");
                    for (int r=0;r<cells.length;r++) for (int c=0;c<cells[r].length;c++) cells[r][c]=(r+c)%5;
                    touch(board);
                });
                waitForIdleSync();
                check((Boolean)get(board,"gameOver"),"loss fixture did not lose at stage " + stage.id);
                check(beforeLoss.equals(progress.getAll()),"defeat changed rewards at stage " + stage.id);
                main(() -> check(hasText(activity.getWindow().getDecorView(),"敗北"),"defeat screen missing"));
            }
            check(expectedCoins==1760 && totalExp==1120, "unexpected final reward totals");
            result.putString("result", "PASS: three stages, all waves, coins, EXP level-up, unlock, drops, duplicate guard, replay, defeat");
            result.putInt("expectedCoins",expectedCoins);
            result.putInt("expectedLevel",12);
            result.putInt("expectedExp",160);
            Map<String,Integer> expectedLoot=new HashMap<>();
            for (DropData.Item item:DropData.ITEMS)
                expectedLoot.put("loot_"+item.id,drops.getOrDefault(item.id,0));
            result.putString("expectedLoot",new org.json.JSONObject(expectedLoot).toString());
            finish(Activity.RESULT_OK,result);
        } catch (Throwable t) {
            result.putString("result","FAIL: "+t.toString());
            android.util.Log.e("BattleRegression","test failed",t);
            finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void startBattle() throws Exception {
        main(() -> {
            Method method=MainActivity.class.getDeclaredMethod("showBattle",int.class);
            method.setAccessible(true); method.invoke(activity,stage.id);
            set(activity,"currentBattleStage",stage);
            PuzzleBoardView board=(PuzzleBoardView)get(activity,"board");
            board.setStage(stage); board.resetGame();
        });
        waitForIdleSync();
    }
    private void main(Checked action) throws Exception {
        failure=null;
        runOnMainSync(() -> { try { action.run(); } catch(Throwable t) { failure=t; } });
        if(failure!=null) throw new Exception("main-thread test failed",failure);
    }
    private interface Checked { void run() throws Exception; }
    private static Object get(Object instance,String name) throws Exception {
        Field f=instance.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(instance);
    }
    private static void set(Object instance,String name,Object value) throws Exception {
        Field f=instance.getClass().getDeclaredField(name); f.setAccessible(true); f.set(instance,value);
    }
    private static void touch(PuzzleBoardView board) {
        long now=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,1,1,0);
        MotionEvent up=MotionEvent.obtain(now,now+1,MotionEvent.ACTION_UP,1,1,0);
        try { board.onTouchEvent(down); board.onTouchEvent(up); }
        finally { down.recycle(); up.recycle(); }
    }
    private static boolean hasText(View view,String fragment) {
        if(view instanceof TextView && ((TextView)view).getText().toString().contains(fragment)) return true;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++)
            if(hasText(((ViewGroup)view).getChildAt(i),fragment)) return true;
        return false;
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
