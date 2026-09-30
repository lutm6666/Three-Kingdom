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

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
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
            for (long encounterSeed=0; encounterSeed<100; encounterSeed++) {
                StageData.Stage candidate = StageData.materializeRun(StageData.get(0), encounterSeed);
                if (DropData.roll(candidate).length > 0) { stage=candidate; break; }
            }
            check(stage != null, "could not select a nonempty deterministic drop fixture");
            Map<String,Integer> drops = new HashMap<>();
            for (DropData.Item item : DropData.roll(stage)) drops.put(item.id, drops.getOrDefault(item.id,0)+1);
            for (int run=1; run<=2; run++) {
                startBattle();
                PuzzleBoardView board = (PuzzleBoardView) get(activity, "board");
                for (int turn=0;turn<40 && !(Boolean)get(board,"victory");turn++) {
                    main(() -> {
                        int[][] cells=(int[][])get(board,"board");
                        for (int[] row:cells) java.util.Arrays.fill(row, GameData.SHU);
                        ((Random)get(board,"random")).setSeed(12345L);
                        touch(board);
                    });
                    waitForIdleSync();
                    check(!(Boolean)get(board,"gameOver"), "party lost during seeded victory fixture");
                }
                check((Boolean)get(board,"victory"), "did not clear all waves");
                check((Integer)get(board,"waveIndex") == stage.waveCount-1, "skipped a wave");
                main(() -> check(hasText(activity.getWindow().getDecorView(), "通關獎勵"), "result screen missing"));
                check(PlayerData.getCoins(activity)==run*stage.coinReward,"coin amount mismatch");
                for (int id:GameData.defaultTeam()) {
                    check(PlayerData.getLevel(activity,id)==10,"unexpected level change");
                    check(PlayerData.getExp(activity,id)==run*stage.expReward,"EXP amount mismatch");
                }
                check(PlayerData.getHighestUnlockedStage(activity)==1,"next stage not unlocked");
                for (DropData.Item item:DropData.ITEMS)
                    check(PlayerData.getLootCount(activity,item.id)==run*drops.getOrDefault(item.id,0),"drop mismatch: "+item.id);
                Map<String,?> once=new HashMap<>(progress.getAll());
                // Duplicate completion notification after an actual win must not pay twice.
                main(() -> activity.onBattleResolved(stage.waveCount,"duplicate",0,1,1,1,0,0,0,0,
                        new int[5],new int[5],0,true,true,false,"duplicate"));
                waitForIdleSync();
                check(once.equals(progress.getAll()),"duplicate callback granted rewards twice");
            }
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
            check((Boolean)get(board,"gameOver"),"loss fixture did not lose");
            check(beforeLoss.equals(progress.getAll()),"defeat granted or changed rewards");
            main(() -> check(hasText(activity.getWindow().getDecorView(),"敗北"),"defeat screen missing"));
            result.putString("result", "PASS: all waves, coins, EXP, unlock, drops, duplicate guard, replay, defeat");
            result.putInt("expectedCoins",2*stage.coinReward);
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
            method.setAccessible(true); method.invoke(activity,0);
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
