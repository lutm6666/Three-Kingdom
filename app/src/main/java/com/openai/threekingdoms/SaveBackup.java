package com.openai.threekingdoms;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.*;

/** Portable offline save. Research Master/art assets are deliberately not included. */
public final class SaveBackup {
    public static final int MAX_BYTES=65536;
    private SaveBackup() {}
    public static final class Snapshot {
        private final Map<String,Object> game, progress;
        private Snapshot(Map<String,Object> game,Map<String,Object> progress){this.game=game;this.progress=progress;}
        public String summary(){return "銅錢："+progress.getOrDefault("coins",0)+"\n已解鎖關卡："
                +(((Integer)progress.getOrDefault("highest_unlocked_stage",0))+1)+"\n將取代目前的隊伍與養成存檔。";}
    }
    public static String export(Context context) throws Exception {
        JSONObject root=new JSONObject();
        root.put("schema","three-kingdom-save");root.put("version",1);
        root.put("game",new JSONObject(context.getSharedPreferences("game",0).getAll()));
        root.put("progress",new JSONObject(context.getSharedPreferences("progress",0).getAll()));
        String json=root.toString(2); parse(json);return json;
    }
    public static Snapshot parse(String json) throws Exception {
        if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("備份檔過大");
        JSONObject root=new JSONObject(json);
        if(root.length()!=4 || !"three-kingdom-save".equals(root.get("schema")) || integer(root.get("version"))!=1)
            throw new IllegalArgumentException("不支援的備份格式或版本");
        Map<String,Object> game=read(root.getJSONObject("game")),progress=read(root.getJSONObject("progress"));
        for(Map.Entry<String,Object> item:game.entrySet()) {
            int slot=id(item.getKey(),"team_",5);int general=integer(item.getValue());
            if(general>=GameData.ROSTER.length)throw new IllegalArgumentException("未知隊伍武將");
        }
        for(Map.Entry<String,Object> item:progress.entrySet())validateProgress(item.getKey(),item.getValue());
        Set<Integer> team=new HashSet<>();int[] defaults=GameData.defaultTeam();
        for(int slot=0;slot<5;slot++) {
            int general=(Integer)game.getOrDefault("team_"+slot,defaults[slot]);
            if(!team.add(general) || !Boolean.TRUE.equals(progress.getOrDefault("owned_"+general,general<=4)))
                throw new IllegalArgumentException("隊伍重複或包含未擁有武將");
        }
        Set<Integer> weapons=new HashSet<>();
        for(Map.Entry<String,Object> item:progress.entrySet())if(item.getKey().startsWith("equipped_weapon_")) {
            int weapon=(Integer)item.getValue();
            if(weapon>=0 && (!weapons.add(weapon) || !Boolean.TRUE.equals(progress.get("weapon_owned_"+weapon))))
                throw new IllegalArgumentException("裝備重複或尚未擁有");
        }
        return new Snapshot(game,progress);
    }
    public static void restore(Context context,Snapshot save) throws Exception {
        SharedPreferences game=context.getSharedPreferences("game",0),progress=context.getSharedPreferences("progress",0);
        Map<String,?> oldGame=game.getAll(),oldProgress=progress.getAll();
        if(!write(game,save.game) || !write(progress,save.progress)) {
            boolean recoveredGame=write(game,oldGame),recoveredProgress=write(progress,oldProgress);
            throw new IllegalStateException(recoveredGame&&recoveredProgress?"寫入失敗，已保留原存檔":"存檔寫入失敗，請保留備份檔");
        }
    }
    private static Map<String,Object> read(JSONObject row) throws Exception {
        Map<String,Object> values=new HashMap<>();Iterator<String> keys=row.keys();
        while(keys.hasNext()) {String key=keys.next();Object raw=row.get(key);
            values.put(key,raw instanceof Boolean?raw:integer(raw));}
        return values;
    }
    private static void validateProgress(String key,Object raw) throws Exception {
        if(key.startsWith("owned_")){id(key,"owned_",GameData.ROSTER.length);bool(raw);return;}
        if(key.startsWith("weapon_owned_")){id(key,"weapon_owned_",EquipmentData.WEAPONS.length);bool(raw);return;}
        int value=integer(raw);
        if(key.equals("coins"))return;
        if(key.equals("highest_unlocked_stage")){if(value>=StageData.STAGES.length)throw new IllegalArgumentException("未知關卡");return;}
        for(String prefix:new String[]{"level_","exp_","equipped_weapon_"})if(key.startsWith(prefix)) {
            id(key,prefix,GameData.ROSTER.length);
            if(prefix.equals("level_")&&value==0)throw new IllegalArgumentException("等級不可為零");
            if(prefix.equals("equipped_weapon_")&&EquipmentData.get(value)==null)throw new IllegalArgumentException("未知裝備");
            return;
        }
        for(String prefix:new String[]{"loot_","dropcard_growth_"})if(key.startsWith(prefix)) {
            String drop=key.substring(prefix.length());
            if(DropData.get(drop)==null || (prefix.equals("dropcard_growth_")&&DropCardData.get(drop)==null))
                throw new IllegalArgumentException("未知掉落物");return;
        }
        throw new IllegalArgumentException("未知存檔欄位："+key);
    }
    private static void bool(Object raw){if(!(raw instanceof Boolean))throw new IllegalArgumentException("所有權格式錯誤");}
    private static int id(String key,String prefix,int limit) {
        if(!key.startsWith(prefix))throw new IllegalArgumentException("未知隊伍欄位");
        String suffix=key.substring(prefix.length());
        if(!suffix.matches("0|[1-9][0-9]*"))throw new IllegalArgumentException("ID 格式錯誤");
        int id=Integer.parseInt(suffix);if(id>=limit)throw new IllegalArgumentException("未知 ID");return id;
    }
    private static int integer(Object raw) {
        if(!(raw instanceof Number))throw new IllegalArgumentException("數值格式錯誤");
        double n=((Number)raw).doubleValue();
        if(Double.isNaN(n)||Double.isInfinite(n)||n<0||n>Integer.MAX_VALUE||n!=Math.rint(n))throw new IllegalArgumentException("數值範圍錯誤");
        return (int)n;
    }
    private static boolean write(SharedPreferences prefs,Map<String,?> values) {
        SharedPreferences.Editor edit=prefs.edit().clear();
        for(Map.Entry<String,?> entry:values.entrySet())if(entry.getValue() instanceof Boolean)
            edit.putBoolean(entry.getKey(),(Boolean)entry.getValue());
        else edit.putInt(entry.getKey(),(Integer)entry.getValue());
        return edit.commit();
    }
}
