package com.openai.threekingdoms.test;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import com.openai.threekingdoms.*;
import org.json.JSONObject;
import java.util.*;

final class SaveRegression {
    private final Instrumentation test;
    SaveRegression(Instrumentation test){this.test=test;}
    void run() {
        Bundle result=new Bundle();
        try {
            Context context=test.getTargetContext();
            SharedPreferences game=context.getSharedPreferences("game",0),progress=context.getSharedPreferences("progress",0);
            SharedPreferences.Editor team=game.edit().clear(),save=progress.edit().clear();
            for(int i=0;i<5;i++){team.putInt("team_"+i,i+5);save.putBoolean("owned_"+(i+5),true);}
            save.putInt("coins",12345).putInt("highest_unlocked_stage",2).putInt("level_7",150).putInt("exp_7",42)
                .putBoolean("weapon_owned_2",true).putInt("equipped_weapon_7",2)
                .putInt("loot_"+DropData.ITEMS[0].id,3).putInt("dropcard_growth_"+DropCardData.CARDS[0].dropId,120);
            check(team.commit()&&save.commit(),"fixture failed");
            String backup=SaveBackup.export(context);
            Map<String,?> beforeGame=new HashMap<>(game.getAll()),beforeProgress=new HashMap<>(progress.getAll());
            check(game.edit().clear().commit()&&progress.edit().clear().commit(),"clear failed");
            SaveBackup.restore(context,SaveBackup.parse(backup));
            check(beforeGame.equals(game.getAll())&&beforeProgress.equals(progress.getAll()),"round-trip lost data");
            check(progress.getInt("level_7",0)==150 && PlayerData.getLevel(context,7)==GameData.get(7).maxLevel,"legacy level changed");
            for(Object bad:new Object[]{-1,1.5,"12",2147483648L,JSONObject.NULL,true}) {
                JSONObject row=new JSONObject(backup);row.getJSONObject("progress").put("coins",bad);
                reject(context,row.toString(),beforeGame,beforeProgress);
            }
            for(int badCase=0;badCase<8;badCase++) {
                JSONObject row=new JSONObject(backup);
                switch(badCase) {
                    case 0: row.put("version",2);break;
                    case 1: row.getJSONObject("game").put("team_0",99);break;
                    case 2: row.getJSONObject("game").put("team_1",5);break;
                    case 3: row.getJSONObject("progress").put("owned_5",false);break;
                    case 4: row.getJSONObject("progress").put("weapon_owned_2",false);break;
                    case 5: row.getJSONObject("progress").put("loot_unknown",1);break;
                    case 6: row.getJSONObject("progress").put("highest_unlocked_stage",3);break;
                    case 7: row.getJSONObject("progress").put("extra_key",1);break;
                }
                reject(context,row.toString(),beforeGame,beforeProgress);
            }
            reject(context,"{broken",beforeGame,beforeProgress);
            reject(context,String.join("",Collections.nCopies(65537,"x")),beforeGame,beforeProgress);
            // A fresh Activity consumes the restored team through its normal startup.
            MainActivity activity=(MainActivity)test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            test.waitForIdleSync();
            java.lang.reflect.Field field=MainActivity.class.getDeclaredField("teamIds");field.setAccessible(true);
            check(Arrays.equals((int[])field.get(activity),new int[]{5,6,7,8,9}),"startup did not load restored team");
            check(progress.getInt("level_7",0)==150,"startup overwrote legacy level");
            result.putString("result","PASS: save round-trip, ownership, equipment, loot, legacy level, invalid import unchanged, restored startup");
            test.finish(Activity.RESULT_OK,result);
        }catch(Throwable error){android.util.Log.e("SaveRegression","test failed",error);result.putString("result","FAIL: "+error);test.finish(Activity.RESULT_CANCELED,result);}
    }
    private static void reject(Context context,String json,Map<String,?> game,Map<String,?> progress)throws Exception {
        boolean rejected=false;
        try{SaveBackup.restore(context,SaveBackup.parse(json));}catch(Exception expected){rejected=true;}
        check(rejected,"invalid import accepted");
        check(game.equals(context.getSharedPreferences("game",0).getAll())&&progress.equals(context.getSharedPreferences("progress",0).getAll()),"invalid import changed save");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
