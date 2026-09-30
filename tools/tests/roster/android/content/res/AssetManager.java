package android.content.res;
public class AssetManager {
 public String json;
 public java.io.InputStream open(String path) { return new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
}
