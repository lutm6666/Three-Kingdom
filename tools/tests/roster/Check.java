import com.openai.threekingdoms.*;
import org.json.*;
public class Check {
 public static void main(String[] args) throws Exception {
  android.content.Context context = new android.content.Context();
  JSONObject root = new JSONObject(java.nio.file.Files.readString(java.nio.file.Path.of(args[0])));
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) != null) throw new AssertionError("valid master rejected");
  JSONObject first = root.getJSONArray("characters").getJSONObject(0);
  first.getJSONObject("stats").getJSONObject("lv1").put("hp", 777);
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) != null || GameData.get(1).hp != 777) throw new AssertionError("not data driven");
  GameData.General before = GameData.get(1);
  first.getJSONObject("stats").getJSONObject("lv1").put("hp", 888);
  root.getJSONArray("characters").getJSONObject(1).put("troop_type", "BOW");
  context.assets.json = root.toString();
  if (ReconstructedMasterData.applyRoster(context) == null || GameData.get(1) != before) throw new AssertionError("partial mutation");
  if (GameData.get(3).troopType != GameData.CAVALRY || GameData.get(0).id != 0 || GameData.ROSTER.length != 10) throw new AssertionError("save IDs changed");
  root.getJSONArray("characters").getJSONObject(1).put("troop_type", "CAVALRY");
  for (Object bad : new Object[]{-1, 12.5, "777", 2147483648L, JSONObject.NULL}) {
   first.getJSONObject("stats").getJSONObject("lv1").put("hp", bad);
   context.assets.json = root.toString();
   if (ReconstructedMasterData.applyRoster(context) == null || GameData.get(1) != before) throw new AssertionError("invalid integer accepted: " + bad);
  }
  context.assets.json = "{broken";
  if (ReconstructedMasterData.applyRoster(context) == null || GameData.get(1) != before) throw new AssertionError("corrupt JSON mutated roster");
  System.out.println("PASS: canonical load, changed JSON applied, enum rejection, atomic rollback, stable IDs, invalid numeric fields, corrupt JSON");
 }
}
