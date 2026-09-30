import com.openai.threekingdoms.*;
import org.json.*;
public class Check {
 private static JSONObject find(JSONObject root, String id) {
  JSONArray rows = root.getJSONArray("characters");
  for (int i=0;i<rows.length();i++) if (id.equals(rows.getJSONObject(i).getString("id"))) return rows.getJSONObject(i);
  throw new AssertionError("missing row: " + id);
 }
 public static void main(String[] args) throws Exception {
  android.content.Context context = new android.content.Context();
  JSONObject root = new JSONObject(java.nio.file.Files.readString(java.nio.file.Path.of(args[0])));
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) != null) throw new AssertionError("valid master rejected");
  JSONObject first = find(root, "guan_yu_meiran");
  first.getJSONObject("stats").getJSONObject("lv1").put("hp", 777);
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) != null || GameData.get(1).hp != 777) throw new AssertionError("not data driven");
  GameData.General before = GameData.get(1);
  first.getJSONObject("stats").getJSONObject("lv1").put("hp", 888);
  find(root, "zhao_yun_yijinfeng").put("troop_type", "BOW");
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) == null || GameData.get(1) != before) throw new AssertionError("partial mutation");
  if (GameData.get(3).troopType != GameData.CAVALRY || GameData.get(0).id != 0 || GameData.ROSTER.length != 10) throw new AssertionError("save IDs changed");
  find(root, "zhao_yun_yijinfeng").put("troop_type", "CAVALRY");
  for (Object bad : new Object[]{-1, 12.5, "777", 2147483648L, JSONObject.NULL}) {
   first.getJSONObject("stats").getJSONObject("lv1").put("hp", bad);
   context.assets.json = root.toString();
   if (ReconstructedMasterData.applyRoster(context) == null || GameData.get(1) != before) throw new AssertionError("invalid integer accepted: " + bad);
  }
  first.getJSONObject("stats").getJSONObject("lv1").put("hp", 777);
  find(root, "lu_bu_zhangui").put("variant", "戰鬼");
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) == null || GameData.get(1) != before) throw new AssertionError("partial match accepted");
  find(root, "lu_bu_zhangui").put("variant", "戦鬼");
  find(root, "lu_bu_zhangui").put("runtime_roster_id", JSONObject.NULL);
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) == null || GameData.get(1) != before) throw new AssertionError("missing playable row accepted");
  find(root, "lu_bu_zhangui").put("runtime_roster_id", 7);
  JSONArray rows = root.getJSONArray("characters"), reverse = new JSONArray();
  for (int i=rows.length()-1;i>=0;i--) reverse.put(rows.get(i));
  root.put("characters", reverse);
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) != null || GameData.get(1).hp != 777) throw new AssertionError("row order changes result");
  before = GameData.get(1);
  context.assets.json = "{broken";
  if (ReconstructedMasterData.applyRoster(context) == null || GameData.get(1) != before) throw new AssertionError("corrupt JSON mutated roster");
  root = new JSONObject(java.nio.file.Files.readString(java.nio.file.Path.of(args[0])));
  context.assets.json = root.toString();
  StageData.Stage original = StageData.get(0);
  if (ReconstructedMasterData.applyStages(context) != null) throw new AssertionError("stage load rejected");
  for (int id=0;id<3;id++) {
   StageData.Stage stage = StageData.get(id);
   StageData.Stage run = StageData.materializeRun(stage, 42);
   if (run.waves.length != stage.waveCount || run.waves[stage.waveCount-1] != stage.fixedBossWave)
    throw new AssertionError("wave materialization");
  }
  JSONObject stage = root.getJSONArray("stages").getJSONObject(0);
  stage.getJSONObject("rewards").put("coin", 123);
  stage.getJSONArray("boss_wave").getJSONObject(0).put("hp", 777);
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyStages(context) != null || StageData.get(0).coinReward != 123
      || StageData.materializeRun(StageData.get(0),42).waves[2].maxHp != 777)
   throw new AssertionError("stage not data driven");
  if (StageData.get(0).advantagePairs != original.advantagePairs
      || !StageData.get(2).commonPool.enemies[0].attackVerified)
   throw new AssertionError("compatibility semantics changed");
  StageData.Stage[] saved = StageData.STAGES.clone();
  JSONObject last = root.getJSONArray("stages").getJSONObject(2);
  for (Object bad : new Object[]{-1, 0, 1.5, "3", 2147483648L, JSONObject.NULL}) {
   last.put("wave_count", bad);
   context.assets.json = root.toString();
   if (ReconstructedMasterData.applyStages(context) == null) throw new AssertionError("invalid stage number");
   for (int i=0;i<3;i++) if (StageData.get(i) != saved[i]) throw new AssertionError("partial stage mutation");
  }
  last.put("wave_count", 5);
  last.put("runtime_stage_id", 0);
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyStages(context) == null) throw new AssertionError("duplicate stage binding");
  last.put("runtime_stage_id", 2);
  JSONArray stages = root.getJSONArray("stages"), reversed = new JSONArray();
  for (int i=2;i>=0;i--) reversed.put(stages.get(i));
  root.put("stages", reversed);
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyStages(context) != null || StageData.get(0).coinReward != 123)
   throw new AssertionError("stage order changed binding");
  System.out.println("PASS: stage load, numeric propagation, materialization, compatibility, atomic rollback, strict numbers, stable bindings");
  System.out.println("PASS: canonical load, changed JSON applied, enum rejection, atomic rollback, stable IDs, invalid numeric fields, corrupt JSON");
 }
}
