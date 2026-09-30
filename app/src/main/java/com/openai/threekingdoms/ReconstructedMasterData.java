package com.openai.threekingdoms;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class ReconstructedMasterData {
    public static final String ASSET_PATH =
            "data/reconstructed_master_v1.json";

    public static final class Summary {
        public final boolean valid;
        public final String schema;
        public final int version;
        public final int characters;
        public final int verifiedCharacters;
        public final int stages;
        public final int reconstructedEncounterStages;
        public final int verifiedArtIdentities;
        public final String error;

        private Summary(
                boolean valid,
                String schema,
                int version,
                int characters,
                int verifiedCharacters,
                int stages,
                int reconstructedEncounterStages,
                int verifiedArtIdentities,
                String error) {
            this.valid = valid;
            this.schema = schema;
            this.version = version;
            this.characters = characters;
            this.verifiedCharacters = verifiedCharacters;
            this.stages = stages;
            this.reconstructedEncounterStages =
                    reconstructedEncounterStages;
            this.verifiedArtIdentities = verifiedArtIdentities;
            this.error = error;
        }

        public String statusText() {
            if (!valid) {
                return "INVALID："
                        + (error == null ? "未知錯誤" : error);
            }

            return "VALID"
                    + "｜schema=" + schema
                    + "｜v" + version;
        }
    }

    private ReconstructedMasterData() {}

    public static Summary loadSummary(Context context) {
        try {
            JSONObject root =
                    new JSONObject(readAsset(context, ASSET_PATH));

            JSONObject meta = root.getJSONObject("meta");
            String schema = meta.getString("schema");
            int version = meta.getInt("version");

            if (!"sgpz-reconstructed-master".equals(schema)) {
                throw new IllegalStateException(
                        "unexpected schema: " + schema);
            }

            JSONArray characters =
                    root.getJSONArray("characters");
            JSONArray stages =
                    root.getJSONArray("stages");

            int verifiedCharacters = 0;
            Set<String> characterIds = new HashSet<>();

            for (int i = 0; i < characters.length(); i++) {
                JSONObject character =
                        characters.getJSONObject(i);

                String id = character.getString("id");
                if (!characterIds.add(id)) {
                    throw new IllegalStateException(
                            "duplicate character id: " + id);
                }

                validateConfidence(
                        character.getString("confidence"));

                if ("VERIFIED".equals(
                        character.getString("confidence"))) {
                    verifiedCharacters++;
                }

                validateConfidence(
                        character.getJSONObject("stats")
                                .getString("confidence"));

                validateConfidence(
                        character.getJSONObject("active_skill")
                                .getString("confidence"));

                validateConfidence(
                        character.getString(
                                "art_identity_confidence"));
            }

            int reconstructedEncounterStages = 0;
            Set<String> stageIds = new HashSet<>();

            for (int i = 0; i < stages.length(); i++) {
                JSONObject stage = stages.getJSONObject(i);

                String id = stage.getString("id");
                if (!stageIds.add(id)) {
                    throw new IllegalStateException(
                            "duplicate stage id: " + id);
                }

                String encounterConfidence =
                        stage.getJSONObject("encounter_rule")
                                .getString("confidence");
                validateConfidence(encounterConfidence);

                if ("RECONSTRUCTED".equals(
                        encounterConfidence)) {
                    reconstructedEncounterStages++;
                }
            }

            int verifiedArt =
                    root.getJSONObject("art_assets")
                            .getInt("identities_verified");

            if (verifiedArt != 0) {
                throw new IllegalStateException(
                        "art identity gate violated: "
                                + verifiedArt);
            }

            return new Summary(
                    true,
                    schema,
                    version,
                    characters.length(),
                    verifiedCharacters,
                    stages.length(),
                    reconstructedEncounterStages,
                    verifiedArt,
                    null);

        } catch (Exception e) {
            return new Summary(
                    false,
                    "",
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    e.getClass().getSimpleName()
                            + ": " + e.getMessage());
        }
    }

    private static void validateConfidence(
            String confidence) {
        if ("VERIFIED".equals(confidence)
                || "RECONSTRUCTED".equals(confidence)
                || "PARTIAL".equals(confidence)
                || "UNKNOWN".equals(confidence)) {
            return;
        }

        throw new IllegalArgumentException(
                "invalid confidence: " + confidence);
    }

    private static String readAsset(
            Context context,
            String path) throws Exception {
        try (InputStream in =
                     context.getAssets().open(path);
             ByteArrayOutputStream out =
                     new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int read;

            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) {
                    out.write(buffer, 0, read);
                }
            }

            return new String(
                    out.toByteArray(),
                    StandardCharsets.UTF_8);
        }
    }
}
