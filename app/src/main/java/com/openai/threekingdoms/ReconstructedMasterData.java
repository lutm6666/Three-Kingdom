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
        public final int partialCharacters;
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
                int partialCharacters,
                int stages,
                int reconstructedEncounterStages,
                int verifiedArtIdentities,
                String error) {
            this.valid = valid;
            this.schema = schema;
            this.version = version;
            this.characters = characters;
            this.verifiedCharacters = verifiedCharacters;
            this.partialCharacters = partialCharacters;
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
            int partialCharacters = 0;
            int mappedArt = 0;
            Set<String> characterIds = new HashSet<>();

            for (int i = 0; i < characters.length(); i++) {
                JSONObject character =
                        characters.getJSONObject(i);

                String id = character.getString("id");
                if (!characterIds.add(id)) {
                    throw new IllegalStateException(
                            "duplicate character id: " + id);
                }

                String recordConfidence =
                        character.getString("confidence");
                validateConfidence(recordConfidence);

                if ("VERIFIED".equals(recordConfidence)) {
                    verifiedCharacters++;

                    if (character.isNull("card_no")) {
                        throw new IllegalStateException(
                                "VERIFIED character lacks card_no: "
                                        + id);
                    }
                } else if ("PARTIAL".equals(recordConfidence)) {
                    partialCharacters++;
                }

                validateConfidence(
                        character.getJSONObject("stats")
                                .getString("confidence"));

                validateConfidence(
                        character.getJSONObject("active_skill")
                                .getString("confidence"));

                if (!character.isNull("leader_skill")) {
                    validateConfidence(
                            character.getJSONObject("leader_skill")
                                    .getString("confidence"));
                }

                String artConfidence =
                        character.getString(
                                "art_identity_confidence");
                validateConfidence(artConfidence);

                if (character.isNull("art_asset_id")) {
                    if ("VERIFIED".equals(artConfidence)) {
                        throw new IllegalStateException(
                                "VERIFIED art without asset: " + id);
                    }
                } else {
                    if (!"VERIFIED".equals(artConfidence)) {
                        throw new IllegalStateException(
                                "mapped art is not VERIFIED: " + id);
                    }
                    mappedArt++;
                }
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

                JSONObject fieldConfidence =
                        stage.getJSONObject("field_confidence");
                validateConfidence(
                        fieldConfidence.getString("difficulty"));
                validateConfidence(
                        fieldConfidence.getString("stamina"));
                validateConfidence(
                        fieldConfidence.getString("wave_count"));

                validateConfidence(
                        stage.getJSONObject("rewards")
                                .getString("confidence"));
                validateConfidence(
                        stage.getJSONObject("composition")
                                .getString("confidence"));

                String encounterConfidence =
                        stage.getJSONObject("encounter_rule")
                                .getString("confidence");
                validateConfidence(encounterConfidence);

                if ("RECONSTRUCTED".equals(
                        encounterConfidence)) {
                    reconstructedEncounterStages++;
                }
            }

            JSONObject mechanics =
                    root.getJSONObject("mechanics");
            validateConfidence(
                    root.getJSONObject("enums")
                            .getJSONObject("unit_advantage")
                            .getString("confidence"));
            validateConfidence(
                    mechanics.getJSONObject("faction_advantage")
                            .getString("confidence"));
            validateConfidence(
                    mechanics.getJSONObject("encounter_seed")
                            .getString("confidence"));
            validateConfidence(
                    mechanics.getJSONObject("drop_rate_percent")
                            .getString("confidence"));

            JSONObject enhancement =
                    mechanics.getJSONObject("enhancement");
            String enhancementConfidence =
                    enhancement.getString("confidence");
            validateConfidence(enhancementConfidence);

            String baseGrowthConfidence =
                    enhancement
                            .getJSONObject(
                                    "low_rarity_material_base_growth")
                            .getString("confidence");
            validateConfidence(baseGrowthConfidence);

            if ("VERIFIED".equals(enhancementConfidence)
                    && !"VERIFIED".equals(baseGrowthConfidence)) {
                throw new IllegalStateException(
                        "VERIFIED enhancement contains "
                                + "non-VERIFIED child");
            }

            int verifiedArt =
                    root.getJSONObject("art_assets")
                            .getInt("identities_verified");

            if (verifiedArt != mappedArt) {
                throw new IllegalStateException(
                        "art identity count mismatch: declared="
                                + verifiedArt
                                + " actual=" + mappedArt);
            }

            return new Summary(
                    true,
                    schema,
                    version,
                    characters.length(),
                    verifiedCharacters,
                    partialCharacters,
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
                    0,
                    e.getClass().getSimpleName()
                            + ": " + e.getMessage());
        }
    }

    /** Apply numeric endpoints atomically; legacy IDs and skill execution stay stable. */
    public static String applyRoster(Context context) {
        try {
            Summary summary = loadSummary(context);
            if (!summary.valid) throw new IllegalStateException(summary.error);
            JSONObject root = new JSONObject(readAsset(context, ASSET_PATH));
            JSONArray rows = root.getJSONArray("characters");
            GameData.General[] next = GameData.ROSTER.clone();
            Set<Integer> matched = new HashSet<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                if (!row.has("runtime_roster_id"))
                    throw new IllegalStateException("missing runtime binding: " + row.getString("id"));
                // Research-only rows explicitly have no offline save ID.
                if (row.isNull("runtime_roster_id")) continue;
                int index = nonnegative(row, "runtime_roster_id");
                if (index >= next.length || !GameData.usesMasterData(index))
                    throw new IllegalStateException("unsupported roster binding: " + index);
                if (!matched.add(index)) throw new IllegalStateException("duplicate roster mapping");
                GameData.General old = next[index];
                if (!old.name.equals(row.getString("name"))
                        || !old.sourceVariant.equals(row.getString("variant")))
                    throw new IllegalStateException("roster identity mismatch: " + old.id);
                if (!GameData.factionKey(old.faction).equals(row.getString("faction"))
                        || !GameData.troopTypeKey(old.troopType).equals(row.getString("troop_type")))
                    throw new IllegalStateException("roster enum mismatch: " + old.id);
                JSONObject stats = row.getJSONObject("stats");
                JSONObject low = stats.getJSONObject("lv1");
                JSONObject high = stats.getJSONObject("max");
                JSONObject skill = row.getJSONObject("active_skill");
                if (!old.skillName.equals(skill.getString("name")))
                    throw new IllegalStateException("unsupported skill change: " + old.id);
                JSONObject cd = skill.getJSONObject("cooldown");
                int base = positive(cd, "base");
                int min = positive(cd, "min");
                if (min > base) throw new IllegalStateException("invalid cooldown range");
                next[index] = new GameData.General(old.id, old.name, old.faction, old.troopType,
                        old.sourceVariant, old.provenance, old.rarity, positive(row, "max_level"),
                        positive(low, "hp"), positive(low, "atk"), nonnegative(low, "recovery"),
                        positive(high, "hp"), positive(high, "atk"), nonnegative(high, "recovery"),
                        old.skillName, old.skillDescription, old.skillType, old.skillValue,
                        old.skillAux, base, min, old.leaderName, old.leaderDescription);
            }
            for (int id = 0; id < next.length; id++) {
                if (GameData.usesMasterData(id) && !matched.contains(id))
                    throw new IllegalStateException("missing playable roster row: " + id);
            }
            System.arraycopy(next, 0, GameData.ROSTER, 0, next.length);
            return null;
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** Stage bindings are offline save IDs. Keep combat AI and compatibility flags stable. */
    public static String applyStages(Context context) {
        try {
            Summary summary = loadSummary(context);
            if (!summary.valid) throw new IllegalStateException(summary.error);
            JSONArray rows = new JSONObject(readAsset(context, ASSET_PATH)).getJSONArray("stages");
            StageData.Stage[] next = StageData.STAGES.clone();
            Set<Integer> matched = new HashSet<>();
            String[] keys = {"cao_yellow_turban_01", "cao_iron_gate_01", "cao_guangzong_01"};
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                int id = nonnegative(row, "runtime_stage_id");
                if (id >= next.length || !matched.add(id))
                    throw new IllegalStateException("invalid stage binding: " + id);
                StageData.Stage old = next[id];
                if (!keys[id].equals(row.getString("id")) || !old.name.equals(row.getString("name"))
                        || !old.chapter.equals(row.getString("chapter")))
                    throw new IllegalStateException("stage identity mismatch: " + id);
                JSONObject rule = row.getJSONObject("encounter_rule");
                if (!"equal".equals(rule.getString("weighting")))
                    throw new IllegalStateException("unsupported encounter weighting");
                StageData.Enemy[] pool = stageEnemies(row.getJSONArray("common_pool"), old.commonPool.enemies);
                StageData.Enemy[] boss = stageEnemies(row.getJSONArray("boss_wave"), old.fixedBossWave.enemies);
                int min = positive(rule, "min_enemies"), max = positive(rule, "max_enemies");
                if (min > max || max > pool.length) throw new IllegalStateException("invalid encounter range");
                JSONArray composition = row.getJSONObject("composition").getJSONArray("fixed_boss_wave");
                if (composition.length() != boss.length) throw new IllegalStateException("boss composition mismatch");
                for (int j = 0; j < boss.length; j++)
                    if (!boss[j].name.equals(composition.getString(j)))
                        throw new IllegalStateException("boss composition mismatch");
                int waves = positive(row, "wave_count");
                if (waves > 100) throw new IllegalStateException("too many waves");
                JSONObject rewards = row.getJSONObject("rewards");
                next[id] = StageData.baseStage(id, old.name, old.chapter,
                        positive(row, "difficulty"), positive(row, "stamina"),
                        nonnegative(rewards, "coin"), nonnegative(rewards, "exp"),
                        old.sourceNote, old.advantagePairs, waves,
                        new StageData.EncounterPool(pool, min, max, old.commonPool.note),
                        new StageData.Wave(boss));
            }
            if (matched.size() != next.length) throw new IllegalStateException("missing stage row");
            System.arraycopy(next, 0, StageData.STAGES, 0, next.length);
            return null;
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static StageData.Enemy[] stageEnemies(JSONArray rows, StageData.Enemy[] old) throws Exception {
        if (rows.length() != old.length) throw new IllegalStateException("enemy count mismatch");
        StageData.Enemy[] next = new StageData.Enemy[old.length];
        for (int i = 0; i < old.length; i++) {
            JSONObject row = rows.getJSONObject(i);
            StageData.Enemy prior = old[i];
            if (!prior.name.equals(row.getString("name"))
                    || !GameData.factionKey(prior.faction).equals(row.getString("faction")))
                throw new IllegalStateException("enemy identity mismatch: " + prior.name);
            // Confidence metadata is research provenance, not permission to change combat semantics.
            next[i] = new StageData.Enemy(prior.name, positive(row, "hp"),
                    nonnegative(row, "atk"), positive(row, "turn"), prior.faction,
                    nonnegative(row, "def"), prior.attackVerified, prior.turnVerified,
                    prior.hpDefenseVerified, prior.preemptive, prior.actions);
        }
        return next;
    }

    private static int nonnegative(JSONObject row, String key) throws Exception {
        Object raw = row.get(key);
        if (!(raw instanceof Number)) throw new IllegalStateException("non-numeric " + key);
        double value = ((Number) raw).doubleValue();
        if (Double.isNaN(value) || Double.isInfinite(value)
                || value < 0 || value > Integer.MAX_VALUE || value != Math.rint(value))
            throw new IllegalStateException("invalid integer " + key);
        return (int) value;
    }

    private static int positive(JSONObject row, String key) throws Exception {
        int value = nonnegative(row, key);
        if (value == 0) throw new IllegalStateException("zero " + key);
        return value;
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
