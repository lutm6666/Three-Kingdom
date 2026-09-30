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
  System.out.println("PASS: canonical load, changed JSON applied, enum rejection, atomic rollback, stable IDs, invalid numeric fields, corrupt JSON");
 }
}
