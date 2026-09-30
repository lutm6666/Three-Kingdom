package com.openai.threekingdoms.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import com.openai.threekingdoms.*;
import java.lang.reflect.*;
import java.util.*;

/** Diagnostic bot: reads natural boards, chooses adjacent drags, never edits cells/HP/enemies. */
final class PlayabilityProbe {
    private final Instrumentation test;
    private MainActivity activity;
    private Throwable failure;
    PlayabilityProbe(Instrumentation test) { this.test=test; }
    void run() {
        Bundle result=new Bundle();
        try {
            for (int level : new int[]{1,10}) for (int index=0;index<3;index++) {
                int wins=0, losses=0, limited=0, turns=0;
                for (int seed=0;seed<5;seed++) {
                    test.getTargetContext().getSharedPreferences("game",0).edit().clear().commit();
                    android.content.SharedPreferences.Editor save=test.getTargetContext().getSharedPreferences("progress",0).edit().clear();
                    for (int id:GameData.defaultTeam()) save.putInt("level_"+id,level);
                    if (!save.commit()) throw new AssertionError("save setup failed");
                    activity=(MainActivity)test.startActivitySync(new Intent(test.getTargetContext(),MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    final int stageIndex=index, randomSeed=seed;
                    main(() -> {
                        Method show=MainActivity.class.getDeclaredMethod("showBattle",int.class);
                        show.setAccessible(true); show.invoke(activity,stageIndex);
                        PuzzleBoardView board=(PuzzleBoardView)get(activity,"board");
                        StageData.Stage stage=StageData.materializeRun(StageData.get(stageIndex),randomSeed);
                        set(activity,"currentBattleStage",stage); board.setStage(stage);
                        ((Random)get(board,"random")).setSeed(10000L+randomSeed);
                        board.resetGame();
                    });
                    test.waitForIdleSync();
                    PuzzleBoardView board=(PuzzleBoardView)get(activity,"board");
                    int turn=0;
                    while (!(Boolean)get(board,"victory") && !(Boolean)get(board,"gameOver") && turn<80) {
                        int[] cells=new int[30];
                        int[][] live=(int[][])get(board,"board");
                        for(int r=0;r<5;r++) for(int c=0;c<6;c++) cells[r*6+c]=live[r][c];
                        int[] weights=new int[6];
                        int[] attack=(int[])get(board,"teamAtk");
                        GameData.General[] team=(GameData.General[])get(board,"team");
                        for(int i=0;i<team.length;i++) weights[team[i].faction]+=attack[i];
                        int hp=(Integer)get(board,"playerHp"), max=(Integer)get(board,"playerMaxHp");
                        weights[5]=hp<max/2 ? Arrays.stream(weights).max().orElse(1) : 1;
                        int[] path=choose(cells,weights);
                        main(() -> drag(board,path)); test.waitForIdleSync(); turn++;
                    }
                    turns+=turn;
                    if ((Boolean)get(board,"victory")) wins++;
                    else if ((Boolean)get(board,"gameOver")) losses++;
                    else limited++;
                    main(() -> activity.finish()); test.waitForIdleSync();
                }
                String line="stage="+index+" level="+level+" wins="+wins+" losses="+losses
                        +" turnLimit="+limited+" totalTurns="+turns+" samples=5";
                result.putString("stage"+index+"_level"+level,line);
            }
            result.putString("result","PASS: natural-board diagnostic completed; fixed seeds, no skills, eight-step beam bot");
            test.finish(Activity.RESULT_OK,result);
        } catch(Throwable error) {
            android.util.Log.e("PlayabilityProbe","probe failed",error);
            result.putString("result","FAIL: "+error); test.finish(Activity.RESULT_CANCELED,result);
        }
    }
    private static final class Move {
        int[] cells,path; int score;
        Move(int[] cells,int[] path,int score) { this.cells=cells;this.path=path;this.score=score; }
    }
    private static int[] choose(int[] cells,int[] weights) {
        List<Move> beam=new ArrayList<>();
        Move best=new Move(cells,new int[]{0},-1);
        for(int start=0;start<30;start++) beam.add(new Move(cells,new int[]{start},0));
        for(int depth=0;depth<8;depth++) {
            List<Move> next=new ArrayList<>();
            for(Move old:beam) {
                int at=old.path[old.path.length-1];
                for(int to:new int[]{at-6,at+6,at-1,at+1}) {
                    if(to<0||to>=30 || (Math.abs(to-at)==1 && at/6!=to/6)) continue;
                    if(old.path.length>1 && to==old.path[old.path.length-2]) continue;
                    int[] copy=old.cells.clone(); int tmp=copy[at];copy[at]=copy[to];copy[to]=tmp;
                    int[] path=Arrays.copyOf(old.path,old.path.length+1);path[path.length-1]=to;
                    Move candidate=new Move(copy,path,score(copy,weights));next.add(candidate);
                    if(candidate.score>best.score) best=candidate;
                }
            }
            next.sort((a,b)->Integer.compare(b.score,a.score));
            beam=next.subList(0,Math.min(40,next.size()));
        }
        return best.path;
    }
    private static int score(int[] cells,int[] weights) {
        boolean[] matched=new boolean[30];
        for(int at=0;at<30;at++) {
            if(at%6<=3 && cells[at]==cells[at+1] && cells[at]==cells[at+2])
                matched[at]=matched[at+1]=matched[at+2]=true;
            if(at<18 && cells[at]==cells[at+6] && cells[at]==cells[at+12])
                matched[at]=matched[at+6]=matched[at+12]=true;
        }
        int score=0;
        for(int at=0;at<30;at++) if(matched[at]) score+=10+weights[cells[at]];
        return score;
    }
    private static void drag(PuzzleBoardView board,int[] path) {
        if(board.getWidth()<=0 || board.getHeight()<=0) throw new AssertionError("board not laid out");
        long now=SystemClock.uptimeMillis();
        for(int i=0;i<=path.length;i++) {
            int cell=path[Math.min(i,path.length-1)];
            int action=i==0?MotionEvent.ACTION_DOWN:i==path.length?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE;
            MotionEvent event=MotionEvent.obtain(now,now+i,action,(cell%6+0.5f)*board.getWidth()/6,
                    (cell/6+0.5f)*board.getHeight()/5,0);
            try { board.onTouchEvent(event); } finally { event.recycle(); }
        }
    }
    private void main(Checked action) throws Exception {
        failure=null;
        test.runOnMainSync(() -> { try { action.run(); } catch(Throwable t) { failure=t; } });
        if(failure!=null) throw new Exception("main-thread probe failed",failure);
    }
    private interface Checked { void run() throws Exception; }
    private static Object get(Object value,String name) throws Exception {
        Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);
    }
    private static void set(Object value,String name,Object next) throws Exception {
        Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);f.set(value,next);
    }
}
