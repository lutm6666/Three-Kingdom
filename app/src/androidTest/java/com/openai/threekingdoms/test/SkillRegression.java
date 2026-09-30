package com.openai.threekingdoms.test;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.MotionEvent;
import com.openai.threekingdoms.*;
import java.lang.reflect.*;
import java.util.Arrays;

/** Test-only durable enemies and controlled boards isolate effects; production skill code executes. */
final class SkillRegression {
    private final Instrumentation test;
    private PuzzleBoardView board;
    private Throwable failure;
    private volatile int calls;
    private volatile String message;
    SkillRegression(Instrumentation test) { this.test=test; }
    void run() {
        Bundle result=new Bundle();
        try {
            test.getTargetContext().getSharedPreferences("game",0).edit().clear().commit();
            test.getTargetContext().getSharedPreferences("progress",0).edit().clear().commit();
            MainActivity activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            main(() -> {
                Method show=MainActivity.class.getDeclaredMethod("showBattle",int.class);
                show.setAccessible(true);show.invoke(activity,0);
                board=(PuzzleBoardView)get(activity,"board");
                board.setBattleListener((wave,name,hp,max,php,pmax,turn,combos,damage,heal,gd,cd,seal,cleared,win,loss,text)->{
                    calls++;message=text;
                });
                fixture(GameData.defaultTeam());
                int max=(Integer)get(board,"playerMaxHp");
                set(board,"playerHp",max-1000);
                check(board.useSkill(0),"heal rejected");
                check((Integer)get(board,"playerHp")==max-100,"heal amount wrong");
                int[] cooldown=(int[])get(board,"skillCooldown");
                check(cooldown[0]==4,"heal cooldown wrong");
                check(!board.useSkill(0) && (Integer)get(board,"playerHp")==max-100,"cooldown allowed reuse");
                noMatch();touch(MotionEvent.ACTION_DOWN);touch(MotionEvent.ACTION_UP);
                check(cooldown[0]==3,"turn did not decrement cooldown once");
                for(int i=0;i<3;i++) { noMatch();touch(MotionEvent.ACTION_DOWN);touch(MotionEvent.ACTION_UP); }
                check(cooldown[0]==0 && board.useSkill(0),"cooldown did not become ready");
                check((Integer)get(board,"playerHp")==max,"heal exceeded max HP");
                int[] turns=((int[])get(board,"enemyTurnsRemaining")).clone();
                check(board.useSkill(2),"delay rejected");
                int[] after=(int[])get(board,"enemyTurnsRemaining");
                for(int i=0;i<turns.length;i++)check(after[i]==turns[i]+2,"delay missed enemy");
                int[][] cells=(int[][])get(board,"board");
                for(int r=0;r<5;r++)for(int c=0;c<6;c++)cells[r][c]=(r+c)%6;
                check(board.useSkill(4),"conversion rejected");
                for(int r=0;r<5;r++)for(int c=0;c<6;c++)
                    check(cells[r][c]==((r+c)%6==GameData.WEI?GameData.SHU:(r+c)%6),"conversion changed wrong cell");
                check(board.useSkill(3),"free move rejected");
                noMatch();touch(MotionEvent.ACTION_DOWN);
                check((Long)get(board,"dragDurationMs")==10000L && (Long)get(board,"nextMoveDurationMs")==0L,"free move duration wrong");
                touch(MotionEvent.ACTION_UP); noMatch();touch(MotionEvent.ACTION_DOWN);
                check((Long)get(board,"dragDurationMs")==5000L,"free move applied twice");
                touch(MotionEvent.ACTION_UP);
                fixture(GameData.defaultTeam());
                set(board,"skillSealTurns",1);
                int before=(Integer)get(board,"playerHp");
                check(!board.useSkill(0) && (Integer)get(board,"playerHp")==before
                        && ((int[])get(board,"skillCooldown"))[0]==0,"seal allowed skill or spent cooldown");
                noMatch();touch(MotionEvent.ACTION_DOWN);touch(MotionEvent.ACTION_UP);
                check((Integer)get(board,"skillSealTurns")==0 && board.useSkill(0),"seal did not expire");
                fixture(GameData.defaultTeam());
                int[] enemy=(int[])get(board,"enemyHp");
                check(board.useSkill(1) && enemy[0]==100000-14995,"Guan Yu damage/defense wrong");
                fixture(new int[]{0,7,9,6,5});
                enemy=(int[])get(board,"enemyHp");
                check(board.useSkill(1) && enemy[0]==100000-8310,"Lu Bu did not ignore defense");
                check(board.useSkill(2),"Zhou Yu rejected");
                check(enemy[0]==100000-8310-3923 && enemy[1]==100000-3928 && enemy[2]==100000-3928,"Wu team attack missed targets");
                int beforeDamage=enemy[0];
                check(board.useSkill(4) && enemy[0]==beforeDamage-2095,"fixed damage/faction advantage wrong");
                set(board,"victory",true); check(!board.useSkill(0),"skill allowed after victory");
                set(board,"victory",false);set(board,"gameOver",true);check(!board.useSkill(0),"skill allowed after defeat");
                set(board,"gameOver",false);check(!board.useSkill(-1)&&!board.useSkill(5),"invalid slot accepted");
                fixture(GameData.defaultTeam());board.useSkill(0);noMatch();touch(MotionEvent.ACTION_DOWN);
            });
            int beforeTimeout=calls;
            long deadline=SystemClock.uptimeMillis()+8000L;
            while(calls==beforeTimeout && SystemClock.uptimeMillis()<deadline) SystemClock.sleep(50);
            check(calls==beforeTimeout+1 && message.contains("操作時間終了"),"timer did not settle exactly once");
            main(() -> {
                check(!(Boolean)get(board,"dragging"),"timeout kept drag active");
                check(((int[])get(board,"skillCooldown"))[0]==3,"timeout cooldown decremented incorrectly");
                int once=calls;touch(MotionEvent.ACTION_UP);
                check(calls==once,"release after timeout settled twice");
                board.resetGame();
                for(int cd:(int[])get(board,"skillCooldown"))check(cd==0,"reset kept cooldown");
                check((Integer)get(board,"skillSealTurns")==0 && (Long)get(board,"nextMoveDurationMs")==0L,"reset kept skill effect");
            });
            result.putString("result","PASS: heal, delay, conversion, free move, damage, ignore defense, AoE, cooldown, seal, timeout, reset");
            test.finish(Activity.RESULT_OK,result);
        } catch(Throwable error) {
            android.util.Log.e("SkillRegression","test failed",error);
            result.putString("result","FAIL: "+error);test.finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void fixture(int[] ids) throws Exception {
        board.setStage(StageData.materializeRun(StageData.get(0),0));
        board.setTeam(ids,new int[]{1,1,1,1,1});board.resetGame();set(board,"waveIndex",2);
        Method init=PuzzleBoardView.class.getDeclaredMethod("initWaveState");init.setAccessible(true);init.invoke(board);
        Arrays.fill((int[])get(board,"enemyHp"),100000);
        Arrays.fill((int[])get(board,"enemyTurnsRemaining"),100);
    }
    private void noMatch() throws Exception {
        int[][] cells=(int[][])get(board,"board");
        for(int r=0;r<5;r++)for(int c=0;c<6;c++)cells[r][c]=(r+c)%5;
    }
    private void touch(int action) {
        long now=SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,action,1,1,0);
        try{board.onTouchEvent(event);}finally{event.recycle();}
    }
    private void main(Checked action) throws Exception {
        failure=null;test.runOnMainSync(()->{try{action.run();}catch(Throwable t){failure=t;}});
        if(failure!=null)throw new Exception("main-thread skill test failed",failure);
    }
    private interface Checked{void run()throws Exception;}
    private static Object get(Object instance,String name)throws Exception{Field f=instance.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(instance);}
    private static void set(Object instance,String name,Object value)throws Exception{Field f=instance.getClass().getDeclaredField(name);f.setAccessible(true);f.set(instance,value);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
