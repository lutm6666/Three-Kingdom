package com.openai.threekingdoms;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements PuzzleBoardView.BattleListener {
    private LinearLayout root;
    private TextView waveLabel;
    private TextView playerHpLabel;
    private TextView enemyHpLabel;
    private TextView enemyTurnLabel;
    private TextView resultLabel;
    private TextView damageBreakdownLabel;
    private PuzzleBoardView board;

    private final Button[] skillButtons = new Button[5];
    private final int[] teamIds = new int[5];

    private int selectedTeamSlot = 0;
    private int currentStageIndex = 0;
    private StageData.Stage currentBattleStage;
    private boolean battleRewardGranted = false;
    private String enhancementMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String masterError = ReconstructedMasterData.applyRoster(this);
        if (masterError != null) android.util.Log.e("MasterData", masterError);
        loadTeam();
        showHome();
    }

    private void loadTeam() {
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        int[] defaults = GameData.defaultTeam();

        for (int i = 0; i < teamIds.length; i++) {
            int id = prefs.getInt("team_" + i, defaults[i]);
            teamIds[i] = (id >= 0 && id < GameData.ROSTER.length)
                    ? id
                    : defaults[i];
        }

        PlayerData.migrateOwnership(this, teamIds);
    }

    private void saveTeam() {
        SharedPreferences.Editor editor =
                getSharedPreferences("game", MODE_PRIVATE).edit();

        for (int i = 0; i < teamIds.length; i++) {
            editor.putInt("team_" + i, teamIds[i]);
        }

        editor.apply();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16f);
        b.setAllCaps(false);
        b.setOnClickListener(listener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private int drawableId(String resourceName) {
        return getResources().getIdentifier(
                resourceName,
                "drawable",
                getPackageName());
    }

    private ImageView artImage(int resId, int heightDp, boolean crop) {
        ImageView image = new ImageView(this);
        image.setImageResource(resId);
        image.setAdjustViewBounds(false);
        image.setScaleType(
                crop
                        ? ImageView.ScaleType.CENTER_CROP
                        : ImageView.ScaleType.CENTER_INSIDE);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(heightDp));

        lp.setMargins(0, dp(6), 0, dp(8));
        image.setLayoutParams(lp);
        return image;
    }

    private boolean addOptionalArt(
            String resourceName,
            int heightDp,
            boolean crop) {
        int resId = drawableId(resourceName);
        if (resId == 0) return false;

        root.addView(artImage(resId, heightDp, crop));
        return true;
    }

    private void prepareRoot() {
        ScrollView scroll = new ScrollView(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(22));
        root.setBackgroundColor(Color.rgb(28, 24, 21));

        scroll.addView(root);
        setContentView(scroll);
    }

    private void addTitle(String title, String subtitle) {
        TextView h = text(title, 28f, Color.WHITE);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(h);

        TextView s = text(subtitle, 15f, Color.LTGRAY);
        s.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(s);
    }

    private String teamSummary() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < teamIds.length; i++) {
            if (i > 0) sb.append(" / ");
            sb.append(GameData.get(teamIds[i]).name);
        }

        return sb.toString();
    }

    private int[] teamLevels() {
        int[] levels = new int[teamIds.length];

        for (int i = 0; i < teamIds.length; i++) {
            levels[i] = PlayerData.getLevel(this, teamIds[i]);
        }

        return levels;
    }

    private int teamTotalHp() {
        int total = 0;
        GameData.General leader = GameData.get(teamIds[0]);

        for (int id : teamIds) {
            GameData.General member = GameData.get(id);
            total += Math.round(
                    PlayerData.effectiveHp(this, member)
                            * leader.leaderHpMultiplier(member));
        }

        return total;
    }

    private int teamTotalAtk() {
        int total = 0;

        for (int id : teamIds) {
            total += PlayerData.effectiveAtk(this, GameData.get(id));
        }

        return total;
    }

    private String factionName(int faction) {
        return GameData.factionName(faction);
    }

    private String initialEnemySummary(StageData.Wave wave) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < wave.enemies.length; i++) {
            if (i > 0) sb.append("\n");

            StageData.Enemy enemy = wave.enemies[i];

            sb.append(i == 0 ? "▶ " : "　")
                    .append(enemy.name)
                    .append(" [")
                    .append(factionName(enemy.faction))
                    .append("] HP ")
                    .append(enemy.maxHp)
                    .append(enemy.statMark())
                    .append("/")
                    .append(enemy.maxHp)
                    .append(enemy.statMark())
                    .append("　ATK ")
                    .append(enemy.attack)
                    .append(enemy.attackMark())
                    .append("　DEF ")
                    .append(enemy.defense)
                    .append(enemy.statMark())
                    .append("　TURN ")
                    .append(enemy.interval)
                    .append(enemy.turnMark());
        }

        return sb.toString();
    }

    private int teamTotalRecovery() {
        int total = 0;
        for (int id : teamIds) {
            total += PlayerData.effectiveRecovery(this, GameData.get(id));
        }
        return total;
    }

    private void showHome() {
        prepareRoot();
        addTitle("三國志拼圖大戰重建", "v2.5 重建 Master 資料核心");

        boolean hasOriginalArt = addOptionalArt(
                OriginalArtData.BACKGROUND_RESOURCE,
                150,
                true);

        root.addView(text(
                hasOriginalArt
                        ? "原作 APK 美術已在本機接入；公開 repo 不包含原作 PNG。"
                        : "原作 APK 美術尚未匯入；可執行 tools/import_original_art.ps1 從本機 APK 解包資料加入。",
                13f,
                Color.rgb(160, 220, 255)));

        int unlocked = Math.min(
                StageData.STAGES.length,
                PlayerData.getHighestUnlockedStage(this) + 1);

        root.addView(text(
                "銅錢：" + PlayerData.getCoins(this),
                16f,
                Color.rgb(255, 215, 100)));

        root.addView(text(
                "已解鎖關卡：" + unlocked + " / " + StageData.STAGES.length,
                15f,
                Color.rgb(160, 220, 255)));

        root.addView(text(
                "目前隊伍：" + teamSummary(),
                16f,
                Color.WHITE));

        root.addView(text(
                "總 HP " + teamTotalHp()
                        + "　總 ATK " + teamTotalAtk()
                        + "　總回復 " + teamTotalRecovery(),
                15f,
                Color.LTGRAY));

        GameData.General leader = GameData.get(teamIds[0]);
        root.addView(text(
                "主將：" + leader.name
                        + "｜隊長技：" + leader.leaderSkillName()
                        + "－" + leader.leaderSkillDescription(),
                15f,
                Color.rgb(255, 210, 120)));

        root.addView(button("隊伍編成", v -> showTeam()));
        root.addView(button("武將圖鑑", v -> showGeneralDex()));
        root.addView(button("武器庫", v -> showWeaponInventory()));
        root.addView(button("原作美術圖庫", v -> showOriginalArtGallery()));
        root.addView(button("Master 資料核心", v -> showMasterCore()));
        root.addView(button("戰利品倉庫", v -> showLootInventory()));
        root.addView(button("武將招募所", v -> showRecruitment()));
        root.addView(button("進入關卡", v -> showStageSelect()));
    }

    private String activeSkillDescription(GameData.General g) {
        return g.skillDescription;
    }

    private String weaponHolderName(int weaponId) {
        for (GameData.General g : GameData.ROSTER) {
            if (PlayerData.getEquippedWeapon(this, g.id) == weaponId) {
                return g.name;
            }
        }
        return "未裝備";
    }

    private void showMasterCore() {
        prepareRoot();

        ReconstructedMasterData.Summary summary =
                ReconstructedMasterData.loadSummary(this);

        addTitle(
                "Master 資料核心",
                "reconstructed_master_v1.json");

        root.addView(text(
                summary.statusText(),
                17f,
                summary.valid
                        ? Color.rgb(120, 230, 150)
                        : Color.rgb(255, 120, 120)));

        if (summary.valid) {
            root.addView(text(
                    "角色：" + summary.characters
                            + "　record VERIFIED："
                            + summary.verifiedCharacters
                            + "　PARTIAL："
                            + summary.partialCharacters,
                    16f,
                    Color.WHITE));

            root.addView(text(
                    "關卡：" + summary.stages
                            + "　遇敵規則標為 RECONSTRUCTED："
                            + summary.reconstructedEncounterStages,
                    16f,
                    Color.WHITE));

            root.addView(text(
                    "已驗證原作立繪身份："
                            + summary.verifiedArtIdentities
                            + " / "
                            + OriginalArtData.CHARACTER_ART.length,
                    16f,
                    Color.LTGRAY));

            root.addView(text(
                    "這份 Master 是目前離線重建版的 canonical data source，"
                            + "不是宣稱已找回的原伺服器 Master。"
                            + " VERIFIED／RECONSTRUCTED／PARTIAL／UNKNOWN"
                            + " 會保留到欄位層級。",
                    13f,
                    Color.rgb(255, 196, 96)));

            root.addView(text(
                    "原始 Master 的 native 流程已確認為 "
                            + "master*.bin → zlib/deflate → JSON。"
                            + " 舊伺服器與公開快照均未找到可用 payload，"
                            + "因此目前採 provenance-first 重建。",
                    13f,
                    Color.rgb(160, 220, 255)));
        } else {
            root.addView(text(
                    "Master 載入失敗。此版本不應繼續把新資料寫入硬編碼 Java；"
                            + "先修復 JSON／confidence 驗證。",
                    14f,
                    Color.rgb(255, 120, 120)));
        }

        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showOriginalArtGallery() {
        prepareRoot();
        addTitle(
                "原作美術圖庫",
                "16 張立繪／身份映射重新驗證中");

        addOptionalArt(
                OriginalArtData.BACKGROUND_RESOURCE,
                180,
                true);

        root.addView(text(
                "目前已整合："
                        + OriginalArtData.CHARACTER_ART.length
                        + " 張完整角色立繪；目前 "
                        + OriginalArtData.verifiedCount()
                        + " 張達 HIGH。先前 3 張映射已因證據循環而撤回，"
                        + "全部重新按原始資料驗證。",
                14f,
                Color.rgb(160, 220, 255)));

        root.addView(text(
                "HIGH 標準：必須來自原始遊戲資料的 serial／card 身份關聯，"
                        + "再由獨立來源交叉確認。重建／測試 Master 不可作身份證據。"
                        + " 未達此標準者不命名。",
                13f,
                Color.rgb(255, 196, 96)));

        for (OriginalArtData.Entry entry
                : OriginalArtData.CHARACTER_ART) {
            TextView label = text(
                    entry.verifiedLabel(),
                    15f,
                    entry.isHighConfidence()
                            ? Color.rgb(120, 230, 150)
                            : Color.WHITE);
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            root.addView(label);

            if (!addOptionalArt(
                    entry.resourceName,
                    220,
                    false)) {
                TextView missing = text(
                        "本機尚未匯入此原作 PNG",
                        13f,
                        Color.GRAY);
                missing.setGravity(Gravity.CENTER_HORIZONTAL);
                root.addView(missing);
            }
        }

        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showWeaponInventory() {
        prepareRoot();
        addTitle("武器庫", "舊重建版裝備；保留既有存檔相容");

        int owned = 0;
        for (EquipmentData.Weapon weapon : EquipmentData.WEAPONS) {
            if (PlayerData.ownsWeapon(this, weapon.id)) {
                owned++;
            }
        }

        root.addView(text(
                "已取得：" + owned + " / " + EquipmentData.WEAPONS.length,
                16f,
                Color.rgb(160, 220, 255)));

        for (EquipmentData.Weapon weapon : EquipmentData.WEAPONS) {
            boolean has = PlayerData.ownsWeapon(this, weapon.id);
            String holder = has ? weaponHolderName(weapon.id) : "尚未取得";

            root.addView(text(
                    (has ? "【已取得】" : "【未取得】")
                            + " " + weapon.name
                            + "　ATK +" + weapon.atkBonus,
                    18f,
                    has ? Color.WHITE : Color.GRAY));

            root.addView(text(
                    "來源：" + weapon.source
                            + "　目前：" + holder,
                    14f,
                    has ? Color.LTGRAY : Color.DKGRAY));
        }

        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showLootInventory() {
        prepareRoot();
        addTitle(
                "戰利品倉庫",
                "原作可掉落表；目前掉率為重建 "
                        + DropData.RECON_DROP_RATE_PERCENT + "%");

        root.addView(text(
                "注意：Wiki 有記錄「可掉落」與獲得率加成活動，但未提供這些普通關卡的基準百分比。",
                13f,
                Color.rgb(255, 196, 96)));

        root.addView(text(
                "已入庫種類：" + PlayerData.distinctLootCount(this)
                        + " / " + DropData.ITEMS.length
                        + "　總件數：" + PlayerData.totalLootCount(this),
                16f,
                Color.rgb(160, 220, 255)));

        boolean any = false;

        for (DropData.Item item : DropData.ITEMS) {
            int count = PlayerData.getLootCount(this, item.id);
            if (count <= 0) continue;

            any = true;

            root.addView(text(
                    item.displayName()
                            + "　×" + count
                            + "　[" + item.typeName() + "]",
                    18f,
                    Color.WHITE));

            root.addView(text(
                    "來源：" + item.source,
                    13f,
                    Color.LTGRAY));
        }

        if (!any) {
            root.addView(text(
                    "目前尚未取得戰利品。",
                    16f,
                    Color.GRAY));
        }

        root.addView(text(
                "掉落武將目前先以「卡片戰利品」獨立保存，不會與既有出戰用武將版本混併。",
                13f,
                Color.rgb(160, 220, 255)));

        root.addView(button("查看掉落卡圖鑑", v -> showDropCardDex()));
        root.addView(button("武將強化", v -> showEnhancementTargets()));
        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showDropCardDex() {
        prepareRoot();
        addTitle(
                "掉落卡圖鑑",
                "v2.2 卡片資料／武將強化");

        root.addView(text(
                "已取得的卡片會顯示持有數；未取得卡片仍可查看資料校正狀態。"
                        + " 完整三圍只在原卡頁已查證時顯示。",
                13f,
                Color.rgb(255, 196, 96)));

        int ownedKinds = 0;
        for (DropCardData.Card card : DropCardData.CARDS) {
            if (PlayerData.getLootCount(this, card.dropId) > 0) {
                ownedKinds++;
            }
        }

        root.addView(text(
                "掉落武將卡：" + ownedKinds
                        + " / " + DropCardData.CARDS.length + " 種已取得",
                16f,
                Color.rgb(160, 220, 255)));

        for (DropCardData.Card card : DropCardData.CARDS) {
            int count = PlayerData.getLootCount(this, card.dropId);
            boolean owned = count > 0;

            String status = card.statsVerified
                    ? "完整校正"
                    : (card.identityVerified ? "部分校正" : "未校正");

            Button b = button(
                    (owned ? "【持有×" + count + "】" : "【未取得】")
                            + " " + card.stars() + " " + card.name
                            + "\n"
                            + card.factionName() + "／"
                            + card.troopTypeName()
                            + "　" + status,
                    v -> showDropCardDetail(card.dropId));

            if (!owned) {
                b.setAlpha(0.72f);
            }

            root.addView(b);
        }

        root.addView(button("返回戰利品倉庫", v -> showLootInventory()));
        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showDropCardDetail(String dropId) {
        DropData.Item item = DropData.get(dropId);
        DropCardData.Card card = DropCardData.get(dropId);

        if (item == null || card == null) {
            showDropCardDex();
            return;
        }

        prepareRoot();
        addTitle(
                card.stars() + " " + card.name,
                card.sourceCardNo == null
                        ? "原作低星掉落卡"
                        : "原作圖鑑 " + card.sourceCardNo);

        int count = PlayerData.getLootCount(this, dropId);

        int currentCardLevel = EnhancementData.canEnhanceTarget(card)
                ? PlayerData.getDropCardLevel(this, dropId)
                : item.level;

        root.addView(text(
                "持有：" + count
                        + "　掉落時 Lv." + item.level
                        + (EnhancementData.canEnhanceTarget(card)
                                ? "　目前 Lv." + currentCardLevel
                                : ""),
                17f,
                count > 0 ? Color.rgb(255, 215, 100) : Color.GRAY));

        root.addView(text(
                "勢力：" + card.factionName()
                        + "　兵種：" + card.troopTypeName()
                        + "　稀有度：" + card.stars(),
                17f,
                Color.WHITE));

        root.addView(text(
                "Max Lv："
                        + (card.maxLevel > 0
                                ? String.valueOf(card.maxLevel)
                                : "未校正"),
                16f,
                Color.LTGRAY));

        root.addView(text(
                card.statSummary(),
                16f,
                card.statsVerified
                        ? Color.LTGRAY
                        : Color.rgb(255, 196, 96)));

        if (card.statsVerified) {
            root.addView(text(
                    "初始：HP " + card.hp
                            + "／ATK " + card.atk
                            + "／回復 " + card.recovery,
                    15f,
                    Color.LTGRAY));

            root.addView(text(
                    "最大：HP " + card.maxHp
                            + "／ATK " + card.maxAtk
                            + "／回復 " + card.maxRecovery,
                    15f,
                    Color.LTGRAY));
        }

        root.addView(text(
                "技能：" + card.skillName
                        + "\n" + card.skillDescription,
                16f,
                Color.rgb(160, 220, 255)));

        root.addView(text(
                "軍略：" + card.leaderName
                        + "\n" + card.leaderDescription,
                15f,
                Color.LTGRAY));

        root.addView(text(
                "資料狀態：" + card.verificationNote,
                13f,
                card.statsVerified
                        ? Color.rgb(120, 230, 150)
                        : Color.rgb(255, 196, 96)));

        root.addView(text(
                "來源關卡：" + item.source,
                13f,
                Color.GRAY));

        if (count > 0 && EnhancementData.canEnhanceTarget(card)) {
            root.addView(button(
                    "強化這張武將卡",
                    v -> showEnhancementTarget(dropId)));
        }

        root.addView(button("返回掉落卡圖鑑", v -> showDropCardDex()));
        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showEnhancementTargets() {
        prepareRoot();
        addTitle("武將強化", "v2.2 原作費用／勢力倍率");

        root.addView(text(
                "原作規則：費用＝本體目前 Lv × 100 貫 × 素材數；"
                        + "同勢力素材的成長度 ×1.5。",
                14f,
                Color.rgb(160, 220, 255)));

        root.addView(text(
                "注意：普通低星武將的 Lv.1 素材成長度尚未查到完整原表。"
                        + "目前重建基準為 ★1=100、★2=200，再乘素材 Lv；"
                        + "這一段不是原作已知值。",
                13f,
                Color.rgb(255, 196, 96)));

        root.addView(text(
                "目前銅錢：" + PlayerData.getCoins(this),
                16f,
                Color.rgb(255, 215, 100)));

        boolean any = false;

        for (DropCardData.Card card : DropCardData.CARDS) {
            int count = PlayerData.getLootCount(this, card.dropId);

            if (count <= 0 || !EnhancementData.canEnhanceTarget(card)) {
                continue;
            }

            any = true;

            int level = PlayerData.getDropCardLevel(this, card.dropId);
            int growth = PlayerData.getDropCardGrowth(this, card.dropId);
            int next = EnhancementData.nextRequirement(
                    card,
                    level,
                    growth);

            root.addView(button(
                    card.stars() + " " + card.name
                            + "　Lv." + level + "/" + card.maxLevel
                            + "　持有×" + count
                            + "\n成長度 " + growth
                            + (next > 0
                                    ? "　Next " + next
                                    : "　MAX"),
                    v -> showEnhancementTarget(card.dropId)));
        }

        if (!any) {
            root.addView(text(
                    "目前沒有可強化的完整校正本體。"
                            + "現階段第一張支援卡是 ★★張梁；"
                            + "取得後即可使用此系統。",
                    16f,
                    Color.GRAY));
        }

        root.addView(text(
                "成長曲線：目前張梁暫按 Next.5 重建；"
                        + "張寶的 Next.5 已有原作攻略例證，"
                        + "但張梁本身的 Next 類型仍待原卡確認。",
                13f,
                Color.rgb(255, 196, 96)));

        root.addView(button("返回戰利品倉庫", v -> showLootInventory()));
        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showEnhancementTarget(String targetId) {
        DropData.Item targetItem = DropData.get(targetId);
        DropCardData.Card target = DropCardData.get(targetId);

        if (targetItem == null
                || target == null
                || PlayerData.getLootCount(this, targetId) <= 0
                || !EnhancementData.canEnhanceTarget(target)) {
            showEnhancementTargets();
            return;
        }

        prepareRoot();

        int level = PlayerData.getDropCardLevel(this, targetId);
        int growth = PlayerData.getDropCardGrowth(this, targetId);
        int next = EnhancementData.nextRequirement(
                target,
                level,
                growth);

        addTitle(
                "強化：" + target.stars() + " " + target.name,
                "Lv." + level + "/" + target.maxLevel
                        + "｜" + EnhancementData.curveText(target));

        if (!enhancementMessage.isEmpty()) {
            root.addView(text(
                    enhancementMessage,
                    15f,
                    Color.rgb(120, 230, 150)));
            enhancementMessage = "";
        }

        root.addView(text(
                "目前成長度：" + growth
                        + (next > 0
                                ? "　下一級尚需 " + next
                                : "　MAX"),
                16f,
                Color.WHITE));

        root.addView(text(
                "目前銅錢：" + PlayerData.getCoins(this)
                        + "　單張素材費用："
                        + EnhancementData.enhancementCost(level, 1),
                15f,
                Color.rgb(255, 215, 100)));

        root.addView(text(
                "素材成長度重建：★1 "
                        + EnhancementData.RECON_STAR1_BASE_GROWTH
                        + "、★2 "
                        + EnhancementData.RECON_STAR2_BASE_GROWTH
                        + "；再乘素材 Lv。"
                        + "同勢力時套用原作 ×1.5。",
                13f,
                Color.rgb(255, 196, 96)));

        boolean anyMaterial = false;

        for (DropData.Item material : DropData.ITEMS) {
            if (material.type != DropData.TYPE_GENERAL) {
                continue;
            }

            DropCardData.Card materialCard =
                    DropCardData.get(material.id);

            if (materialCard == null) {
                continue;
            }

            int available = PlayerData.getLootCount(
                    this,
                    material.id);

            if (material.id.equals(targetId)) {
                // 保留一張作為本體。
                available--;
            }

            if (available <= 0) {
                continue;
            }

            anyMaterial = true;

            int gained = EnhancementData.materialGrowth(
                    target,
                    material,
                    materialCard);

            boolean sameFaction =
                    target.faction == materialCard.faction;

            String materialId = material.id;

            root.addView(button(
                    material.displayName()
                            + "　可用×" + available
                            + "\n+" + gained + " 成長度"
                            + (sameFaction
                                    ? "（同勢力 ×1.5）"
                                    : "")
                            + "　費用 "
                            + EnhancementData.enhancementCost(level, 1),
                    v -> performEnhancement(
                            targetId,
                            materialId)));
        }

        if (!anyMaterial) {
            root.addView(text(
                    "目前沒有可消耗的武將素材。"
                            + "素材類道具（秘藥／秘玉／名馬）不屬於武將強化素材。",
                    15f,
                    Color.GRAY));
        }

        root.addView(button(
                "返回強化列表",
                v -> showEnhancementTargets()));

        root.addView(button(
                "返回首頁",
                v -> showHome()));
    }

    private void performEnhancement(
            String targetId,
            String materialId) {
        DropData.Item material = DropData.get(materialId);
        DropCardData.Card materialCard =
                DropCardData.get(materialId);
        DropCardData.Card target =
                DropCardData.get(targetId);

        if (target == null
                || material == null
                || materialCard == null
                || material.type != DropData.TYPE_GENERAL) {
            enhancementMessage = "強化失敗：素材資料無效。";
            showEnhancementTarget(targetId);
            return;
        }

        int level = PlayerData.getDropCardLevel(
                this,
                targetId);

        if (level >= target.maxLevel) {
            enhancementMessage = "已達 Max Lv，無法繼續強化。";
            showEnhancementTarget(targetId);
            return;
        }

        int available = PlayerData.getLootCount(
                this,
                materialId);

        if (materialId.equals(targetId)) {
            available--;
        }

        if (available <= 0) {
            enhancementMessage = "強化失敗：沒有可消耗的副本素材。";
            showEnhancementTarget(targetId);
            return;
        }

        int cost = EnhancementData.enhancementCost(
                level,
                1);

        if (!PlayerData.spendCoins(this, cost)) {
            enhancementMessage = "強化失敗：銅錢不足，需要 " + cost + " 貫。";
            showEnhancementTarget(targetId);
            return;
        }

        if (!PlayerData.consumeLoot(
                this,
                materialId,
                1)) {
            PlayerData.addCoins(this, cost);
            enhancementMessage = "強化失敗：素材庫存異常，已退還銅錢。";
            showEnhancementTarget(targetId);
            return;
        }

        int beforeLevel = level;

        int gained = EnhancementData.materialGrowth(
                target,
                material,
                materialCard);

        int updatedGrowth = PlayerData.addDropCardGrowth(
                this,
                targetId,
                gained);

        int afterLevel = PlayerData.getDropCardLevel(
                this,
                targetId);

        enhancementMessage =
                "強化完成：消耗 "
                        + material.displayName()
                        + "，成長度 +" + gained
                        + "，銅錢 -" + cost
                        + "。"
                        + (afterLevel > beforeLevel
                                ? " Lv." + beforeLevel
                                        + " → Lv." + afterLevel
                                : " 目前 Lv." + afterLevel)
                        + "（累積 " + updatedGrowth + "）";

        showEnhancementTarget(targetId);
    }

    private void showGeneralDex() {
        prepareRoot();
        addTitle("武將圖鑑", "查看武將詳細資料與隊長技");

        int ownedCount = 0;
        for (GameData.General g : GameData.ROSTER) {
            if (PlayerData.isGeneralOwned(this, g.id)) {
                ownedCount++;
            }
        }

        root.addView(text(
                "收藏：" + ownedCount + " / " + GameData.ROSTER.length,
                16f,
                Color.rgb(160, 220, 255)));

        for (GameData.General g : GameData.ROSTER) {
            boolean owned = PlayerData.isGeneralOwned(this, g.id);
            int level = PlayerData.getLevel(this, g.id);

            Button entry = button(
                    (owned ? "【已擁有】" : "【未招募】")
                            + " " + g.name
                            + " Lv." + level
                            + " [" + g.factionName() + "・" + g.troopTypeName() + "]"
                            + "｜隊長技：" + g.leaderSkillName(),
                    v -> showGeneralDetail(g.id));

            root.addView(entry);
        }

        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showGeneralDetail(int generalId) {
        GameData.General g = GameData.get(generalId);
        boolean owned = PlayerData.isGeneralOwned(this, generalId);
        int level = PlayerData.getLevel(this, generalId);
        int exp = PlayerData.getExp(this, generalId);
        int need = PlayerData.expToNext(g, level);

        prepareRoot();
        addTitle(g.name, owned ? "已擁有武將" : "尚未招募");

        root.addView(text(
                "勢力：" + g.factionName()
                        + "　兵種：" + g.troopTypeName()
                        + "　Lv." + level,
                20f,
                Color.WHITE));

        root.addView(text(
                "資料來源【" + g.provenance.label + "】"
                        + "　卡面：" + g.sourceVariant
                        + "　" + g.rarity
                        + "　Max Lv." + g.maxLevel,
                15f,
                g.provenance.isOriginalVerified()
                        ? Color.rgb(160, 220, 255)
                        : Color.rgb(255, 196, 96)));

        root.addView(text(
                "HP：" + PlayerData.effectiveHp(this, g)
                        + "　原作 Lv.1 " + g.hp
                        + " / Lv.Max " + g.maxHp,
                17f,
                Color.LTGRAY));

        root.addView(text(
                "ATK：" + PlayerData.effectiveAtk(this, g)
                        + "　原作 Lv.1 " + g.atk
                        + " / Lv.Max " + g.maxAtk,
                17f,
                Color.LTGRAY));

        root.addView(text(
                "回復：" + PlayerData.effectiveRecovery(this, g)
                        + "　原作 Lv.1 " + g.recovery
                        + " / Lv.Max " + g.maxRecovery,
                17f,
                Color.LTGRAY));

        if (g.verifiedOriginal && level > 1 && level < g.maxLevel) {
            root.addView(text(
                    "※ 中間等級數值暫以 Lv.1～Lv.Max 線性插值；原作每級成長表尚待匯入。",
                    13f,
                    Color.rgb(255, 196, 96)));
        }

        root.addView(text(
                "EXP：" + exp + "/"
                        + (need == 0 ? "MAX" : need),
                16f,
                Color.LTGRAY));

        root.addView(text(
                "主動技能｜" + g.skillName,
                20f,
                Color.rgb(180, 205, 255)));

        root.addView(text(
                activeSkillDescription(g)
                        + "　CD " + g.skillCd
                        + (g.skillCdMin < g.skillCd
                                ? " → " + g.skillCdMin
                                : ""),
                16f,
                Color.LTGRAY));

        root.addView(text(
                "隊長技｜" + g.leaderSkillName(),
                20f,
                Color.rgb(255, 210, 120)));

        root.addView(text(
                g.leaderSkillDescription(),
                17f,
                Color.WHITE));

        if (owned) {
            int equippedId = PlayerData.getEquippedWeapon(this, generalId);
            EquipmentData.Weapon equipped = EquipmentData.get(equippedId);

            root.addView(text(
                    "武器｜"
                            + (equipped == null
                                    ? "未裝備"
                                    : equipped.name + "　ATK +" + equipped.atkBonus),
                    20f,
                    Color.rgb(190, 230, 190)));

            for (EquipmentData.Weapon weapon : EquipmentData.WEAPONS) {
                if (!PlayerData.ownsWeapon(this, weapon.id)) {
                    continue;
                }

                boolean isEquipped = equippedId == weapon.id;
                String holder = weaponHolderName(weapon.id);

                Button equip = button(
                        isEquipped
                                ? "✓ 已裝備 " + weapon.name
                                : "裝備 " + weapon.name
                                        + "　ATK +" + weapon.atkBonus
                                        + ("未裝備".equals(holder)
                                                ? ""
                                                : "（目前：" + holder + "）"),
                        v -> {
                            PlayerData.equipWeapon(this, generalId, weapon.id);
                            showGeneralDetail(generalId);
                        });

                equip.setEnabled(!isEquipped);
                root.addView(equip);
            }

            if (equipped != null) {
                root.addView(button(
                        "卸下武器",
                        v -> {
                            PlayerData.equipWeapon(this, generalId, -1);
                            showGeneralDetail(generalId);
                        }));
            }
        }

        if (!owned) {
            int price = GameData.recruitPrice(g.id);
            root.addView(text(
                    "招募價格：銅錢 " + price,
                    16f,
                    Color.rgb(255, 215, 100)));

            Button recruit = button(
                    "前往招募所",
                    v -> showRecruitment());

            root.addView(recruit);
        }

        root.addView(button("返回圖鑑", v -> showGeneralDex()));
        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showRecruitment() {
        prepareRoot();
        addTitle("武將招募所", "使用通關獲得的銅錢招募新武將");

        int coins = PlayerData.getCoins(this);
        int ownedCount = 0;

        for (GameData.General g : GameData.ROSTER) {
            if (PlayerData.isGeneralOwned(this, g.id)) {
                ownedCount++;
            }
        }

        root.addView(text(
                "銅錢：" + coins
                        + "　已擁有：" + ownedCount
                        + " / " + GameData.ROSTER.length,
                17f,
                Color.rgb(255, 215, 100)));

        root.addView(text(
                "初始五將與目前既有隊伍已自動保留。招募後可立即在隊伍編成中使用。",
                14f,
                Color.LTGRAY));

        for (GameData.General g : GameData.ROSTER) {
            boolean owned = PlayerData.isGeneralOwned(this, g.id);
            int price = GameData.recruitPrice(g.id);
            int level = PlayerData.getLevel(this, g.id);

            root.addView(text(
                    g.name
                            + " Lv." + level
                            + " [" + g.factionName() + "・" + g.troopTypeName() + "]"
                            + "　HP " + PlayerData.effectiveHp(this, g)
                            + "　ATK " + PlayerData.effectiveAtk(this, g)
                            + "　技能：" + g.skillName,
                    15f,
                    owned ? Color.WHITE : Color.LTGRAY));

            Button recruit;

            if (owned) {
                recruit = button("✓ 已擁有：" + g.name, v -> {});
                recruit.setEnabled(false);
            } else {
                recruit = button(
                        "招募 " + g.name + "　銅錢 " + price,
                        v -> recruitGeneral(g.id));

                recruit.setEnabled(coins >= price);
            }

            root.addView(recruit);
        }

        root.addView(button("返回首頁", v -> showHome()));
    }

    private void recruitGeneral(int generalId) {
        if (PlayerData.isGeneralOwned(this, generalId)) {
            showRecruitment();
            return;
        }

        int price = GameData.recruitPrice(generalId);

        if (price <= 0 || !PlayerData.spendCoins(this, price)) {
            showRecruitment();
            return;
        }

        PlayerData.setGeneralOwned(this, generalId, true);
        showRecruitment();
    }

    private void showTeam() {
        prepareRoot();
        addTitle("隊伍編成", "先選隊伍位置，再從下方武將名單替換");

        root.addView(text(
                "隊伍總 HP：" + teamTotalHp()
                        + "　總 ATK：" + teamTotalAtk(),
                16f,
                Color.rgb(160, 220, 255)));

        for (int i = 0; i < teamIds.length; i++) {
            final int slot = i;
            GameData.General g = GameData.get(teamIds[i]);

            int level = PlayerData.getLevel(this, g.id);
            int exp = PlayerData.getExp(this, g.id);
            int need = PlayerData.expToNext(g, level);

            String marker = (i == selectedTeamSlot) ? "▶ " : "　 ";
            String role = (i == 0) ? "【主將】" : "";

            root.addView(button(
                    marker + "位置 " + (i + 1) + role
                            + "　" + g.name
                            + " Lv." + level
                            + " [" + g.factionName() + "・" + g.troopTypeName() + "]"
                            + "　HP " + PlayerData.effectiveHp(this, g)
                            + "　ATK " + PlayerData.effectiveAtk(this, g)
                            + "　EXP " + exp + "/"
                            + (need == 0 ? "MAX" : need)
                            + "　" + g.skillName,
                    v -> {
                        selectedTeamSlot = slot;
                        showTeam();
                    }));
        }

        root.addView(text("可用武將", 20f, Color.WHITE));

        for (GameData.General g : GameData.ROSTER) {
            boolean selected = false;

            for (int id : teamIds) {
                if (id == g.id) {
                    selected = true;
                    break;
                }
            }

            boolean owned = PlayerData.isGeneralOwned(this, g.id);
            int level = PlayerData.getLevel(this, g.id);

            String prefix;
            if (!owned) {
                prefix = "【未招募】";
            } else if (selected) {
                prefix = "【已上陣】";
            } else {
                prefix = "【可上陣】";
            }

            Button candidate = button(
                    prefix + " " + g.name
                            + " Lv." + level
                            + " [" + g.factionName() + "・" + g.troopTypeName() + "]"
                            + " HP " + PlayerData.effectiveHp(this, g)
                            + " ATK " + PlayerData.effectiveAtk(this, g)
                            + "｜" + g.skillName,
                    v -> replaceSelectedSlot(g.id));

            candidate.setEnabled(owned);
            root.addView(candidate);
        }

        root.addView(button("恢復預設隊伍", v -> {
            int[] defaults = GameData.defaultTeam();
            System.arraycopy(defaults, 0, teamIds, 0, teamIds.length);
            selectedTeamSlot = 0;
            saveTeam();
            showTeam();
        }));

        root.addView(button("完成編隊／返回首頁", v -> showHome()));
    }

    private void replaceSelectedSlot(int newId) {
        if (!PlayerData.isGeneralOwned(this, newId)) {
            showTeam();
            return;
        }

        int existingSlot = -1;

        for (int i = 0; i < teamIds.length; i++) {
            if (teamIds[i] == newId) {
                existingSlot = i;
                break;
            }
        }

        if (existingSlot >= 0 && existingSlot != selectedTeamSlot) {
            int old = teamIds[selectedTeamSlot];
            teamIds[selectedTeamSlot] = newId;
            teamIds[existingSlot] = old;
        } else {
            teamIds[selectedTeamSlot] = newId;
        }

        saveTeam();
        selectedTeamSlot = (selectedTeamSlot + 1) % teamIds.length;
        showTeam();
    }

    private void showStageSelect() {
        prepareRoot();
        addTitle("關卡選擇", "通關後會永久解鎖下一關");

        root.addView(text(
                "出戰：" + teamSummary(),
                15f,
                Color.rgb(160, 220, 255)));

        root.addView(text(
                "※ * = Wiki 留白後的重建值；TURN／ATK 無 * 者為已校正資料。",
                13f,
                Color.rgb(255, 196, 96)));

        int highestUnlocked = PlayerData.getHighestUnlockedStage(this);

        for (int i = 0; i < StageData.STAGES.length; i++) {
            final int stageIndex = i;
            StageData.Stage stage = StageData.get(i);
            boolean unlocked = i <= highestUnlocked;

            root.addView(text(
                    stage.chapter + "　" + stage.name,
                    20f,
                    unlocked ? Color.WHITE : Color.GRAY));

            root.addView(text(
                    "難度 " + stage.difficulty
                            + "　體力 " + stage.stamina
                            + "　合戰 " + stage.waveCount
                            + "　優劣：" + stage.advantageText(),
                    14f,
                    unlocked
                            ? Color.rgb(255, 210, 120)
                            : Color.DKGRAY));

            root.addView(text(
                    stage.sourceNote,
                    13f,
                    unlocked
                            ? Color.rgb(160, 220, 255)
                            : Color.DKGRAY));

            root.addView(text(
                    "共通出現池：" + stage.commonPool.summary(),
                    14f,
                    unlocked ? Color.LTGRAY : Color.DKGRAY));

            root.addView(text(
                    "抽選：" + stage.commonPool.note,
                    13f,
                    unlocked
                            ? Color.rgb(255, 196, 96)
                            : Color.DKGRAY));

            StringBuilder bossEnemies = new StringBuilder();
            for (int e = 0; e < stage.fixedBossWave.enemies.length; e++) {
                if (e > 0) bossEnemies.append("、");
                StageData.Enemy enemy = stage.fixedBossWave.enemies[e];
                bossEnemies.append(enemy.name)
                        .append("[")
                        .append(factionName(enemy.faction))
                        .append("]")
                        .append(" ATK")
                        .append(enemy.attack)
                        .append(enemy.attackMark())
                        .append(" TURN")
                        .append(enemy.interval)
                        .append(enemy.turnMark());
            }

            root.addView(text(
                    "固定 B" + stage.waveCount + "：" + bossEnemies,
                    14f,
                    unlocked ? Color.LTGRAY : Color.DKGRAY));

            int rewardWeaponId = EquipmentData.rewardWeaponForStage(stageIndex);
            EquipmentData.Weapon rewardWeapon = EquipmentData.get(rewardWeaponId);
            boolean weaponOwned = rewardWeapon != null
                    && PlayerData.ownsWeapon(this, rewardWeaponId);

            root.addView(text(
                    "獎勵：銅錢 +" + stage.coinReward
                            + "；出戰武將 EXP +" + stage.expReward
                            + (rewardWeapon == null
                                    ? ""
                                    : "；"
                                            + (weaponOwned ? "已取得 " : "首次掉落 ")
                                            + rewardWeapon.name
                                            + " ATK +" + rewardWeapon.atkBonus),
                    14f,
                    unlocked
                            ? Color.rgb(255, 215, 100)
                            : Color.DKGRAY));

            Button stageButton = button(
                    unlocked
                            ? "開始：" + stage.name
                            : "🔒 尚未解鎖：" + stage.name,
                    v -> showBattle(stageIndex));

            stageButton.setEnabled(unlocked);
            root.addView(stageButton);

            root.addView(text("", 6f, Color.TRANSPARENT));
        }

        root.addView(button("返回首頁", v -> showHome()));
    }

    private void showBattle(int stageIndex) {
        currentStageIndex = stageIndex;

        StageData.Stage baseStage = StageData.get(stageIndex);
        long runSeed = System.nanoTime()
                ^ (((long) stageIndex + 1L) << 32);
        StageData.Stage stage =
                StageData.materializeRun(baseStage, runSeed);
        currentBattleStage = stage;
        StageData.Wave firstWave = stage.waves[0];

        prepareRoot();
        addTitle(
                stage.name,
                stage.chapter + "｜難度 " + stage.difficulty
                        + "／體力 " + stage.stamina);

        root.addView(text(
                stage.sourceNote,
                13f,
                Color.rgb(160, 220, 255)));

        root.addView(text(
                "本次遭遇種子：" + stage.runSeed
                        + "（同 seed 可重現；抽選權重為重建）",
                12f,
                Color.rgb(255, 196, 96)));

        GameData.General leader = GameData.get(teamIds[0]);
        root.addView(text(
                "主將：" + leader.name
                        + "｜" + leader.leaderSkillName()
                        + "－" + leader.leaderSkillDescription(),
                14f,
                Color.rgb(255, 210, 120)));

        int maxHp = teamTotalHp();

        waveLabel = text(
                "Wave 1 / " + stage.waveCount,
                18f,
                Color.WHITE);

        playerHpLabel = text(
                "我軍 HP：" + maxHp + " / " + maxHp,
                18f,
                Color.rgb(124, 220, 128));

        enemyHpLabel = text(
                initialEnemySummary(firstWave),
                16f,
                Color.WHITE);

        StageData.Enemy firstEnemy = firstWave.enemies[0];
        enemyTurnLabel = text(
                "選定目標 HP：" + firstEnemy.maxHp
                        + " / " + firstEnemy.maxHp
                        + "　攻擊倒數：" + firstEnemy.interval,
                16f,
                Color.rgb(255, 196, 96));

        resultLabel = text(
                "Combo：0　總傷害：0　回復：0",
                17f,
                Color.LTGRAY);

        damageBreakdownLabel = text(
                "武將傷害：尚未攻擊",
                14f,
                Color.rgb(180, 205, 255));

        root.addView(waveLabel);
        root.addView(playerHpLabel);
        root.addView(enemyHpLabel);
        root.addView(enemyTurnLabel);
        root.addView(resultLabel);
        root.addView(damageBreakdownLabel);

        battleRewardGranted = false;

        board = new PuzzleBoardView(this);
        board.setBattleListener(this);
        board.setStage(stage);
        board.setTeam(teamIds, teamLevels());

        root.addView(button("切換目標", v -> board.cycleTarget()));

        LinearLayout skillRow1 = new LinearLayout(this);
        skillRow1.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout skillRow2 = new LinearLayout(this);
        skillRow2.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < skillButtons.length; i++) {
            final int index = i;
            GameData.General g = GameData.get(teamIds[i]);

            Button b = new Button(this);
            b.setAllCaps(false);
            b.setTextSize(12f);
            b.setText(
                    g.name
                            + " Lv." + PlayerData.getLevel(this, g.id)
                            + "\n" + g.skillName + " READY");

            b.setOnClickListener(v -> board.useSkill(index));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f);

            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            b.setLayoutParams(lp);
            skillButtons[i] = b;

            if (i < 3) {
                skillRow1.addView(b);
            } else {
                skillRow2.addView(b);
            }
        }

        root.addView(skillRow1);
        root.addView(skillRow2);

        root.addView(
                board,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(text(
                "v1.9 非固定波會從原作共通出現池抽選；每波 1～2 名、等權重屬重建規則。重新開始保留同遭遇，重新抽選才換 seed。",
                14f,
                Color.LTGRAY));

        root.addView(button("重新開始本關（同遭遇）", v -> {
            battleRewardGranted = false;
            board.resetGame();
        }));

        root.addView(button("重新抽選遭遇", v -> showBattle(stageIndex)));

        root.addView(button("離開關卡", v -> showStageSelect()));

        board.resetGame();
    }

    private void updateSkillButtons(
            int[] cooldowns,
            int skillSealTurns) {
        if (cooldowns == null || cooldowns.length < skillButtons.length) {
            return;
        }

        for (int i = 0; i < skillButtons.length; i++) {
            if (skillButtons[i] == null) {
                continue;
            }

            GameData.General g = GameData.get(teamIds[i]);
            int level = PlayerData.getLevel(this, g.id);

            if (skillSealTurns > 0) {
                skillButtons[i].setText(
                        g.name + " Lv." + level
                                + "\n技能封印 " + skillSealTurns);
                skillButtons[i].setEnabled(false);
            } else {
                skillButtons[i].setEnabled(true);

                if (cooldowns[i] <= 0) {
                    skillButtons[i].setText(
                            g.name + " Lv." + level
                                    + "\n" + g.skillName + " READY");
                } else {
                    skillButtons[i].setText(
                            g.name + " Lv." + level
                                    + "\n" + g.skillName
                                    + " CD " + cooldowns[i]);
                }
            }
        }
    }

    private String formatDamageBreakdown(int[] generalDamage) {
        if (generalDamage == null || generalDamage.length < 5) {
            return "武將傷害：—";
        }

        StringBuilder sb = new StringBuilder("武將傷害：");

        for (int i = 0; i < 5; i++) {
            if (i > 0) sb.append("｜");

            sb.append(GameData.get(teamIds[i]).name)
                    .append(" ")
                    .append(generalDamage[i]);
        }

        return sb.toString();
    }

    private String formatLootDrops(DropData.Item[] drops) {
        if (drops == null || drops.length == 0) {
            return "無";
        }

        StringBuilder sb = new StringBuilder();
        boolean[] used = new boolean[drops.length];

        for (int i = 0; i < drops.length; i++) {
            if (used[i]) continue;

            int count = 1;
            for (int j = i + 1; j < drops.length; j++) {
                if (!used[j] && drops[i].id.equals(drops[j].id)) {
                    used[j] = true;
                    count++;
                }
            }

            if (sb.length() > 0) sb.append("、");
            sb.append(drops[i].displayName());
            if (count > 1) sb.append(" ×").append(count);
        }

        return sb.toString();
    }

    private String grantVictoryRewards() {
        StageData.Stage stage = currentBattleStage != null
                ? currentBattleStage
                : StageData.materializeRun(
                        StageData.get(currentStageIndex),
                        0L);

        PlayerData.addCoins(this, stage.coinReward);

        StringBuilder levelUps = new StringBuilder();

        for (int id : teamIds) {
            GameData.General g = GameData.get(id);

            PlayerData.GainResult gain =
                    PlayerData.addExp(this, id, stage.expReward);

            if (gain.levelsGained > 0) {
                if (levelUps.length() > 0) {
                    levelUps.append("、");
                }

                levelUps.append(g.name)
                        .append("→Lv.")
                        .append(gain.level);
            }
        }

        int nextStage = currentStageIndex + 1;
        boolean unlockedNewStage = nextStage < StageData.STAGES.length
                && nextStage > PlayerData.getHighestUnlockedStage(this);

        if (nextStage < StageData.STAGES.length) {
            PlayerData.unlockStage(this, nextStage);
        }

        String reward =
                "銅錢 +" + stage.coinReward
                        + "；出戰武將 EXP +" + stage.expReward;

        DropData.Item[] drops = DropData.roll(stage);

        for (DropData.Item item : drops) {
            PlayerData.addLoot(this, item.id, 1);
        }

        reward += "\n戰利品（重建掉率 "
                + DropData.RECON_DROP_RATE_PERCENT
                + "%）："
                + formatLootDrops(drops);

        if (levelUps.length() > 0) {
            reward += "\n升級：" + levelUps;
        }

        if (unlockedNewStage) {
            reward += "\n新關卡解鎖："
                    + StageData.get(nextStage).name;
        }

        return reward;
    }

    private void showVictoryResult(String rewardText) {
        StageData.Stage stage = StageData.get(currentStageIndex);

        prepareRoot();
        addTitle(stage.name + "　通關", "戰鬥結算");

        root.addView(text(
                "通關獎勵",
                21f,
                Color.rgb(255, 215, 100)));

        root.addView(text(
                rewardText,
                17f,
                Color.WHITE));

        root.addView(text(
                "目前銅錢：" + PlayerData.getCoins(this),
                16f,
                Color.rgb(255, 215, 100)));

        root.addView(text(
                "出戰武將成長",
                20f,
                Color.WHITE));

        for (int id : teamIds) {
            GameData.General g = GameData.get(id);

            int level = PlayerData.getLevel(this, id);
            int exp = PlayerData.getExp(this, id);
            int need = PlayerData.expToNext(g, level);

            root.addView(text(
                    g.name
                            + " Lv." + level
                            + "　EXP " + exp + "/"
                            + (need == 0 ? "MAX" : need)
                            + "　HP " + PlayerData.effectiveHp(this, g)
                            + "　ATK " + PlayerData.effectiveAtk(this, g),
                    16f,
                    Color.LTGRAY));
        }

        int nextStage = currentStageIndex + 1;

        if (nextStage < StageData.STAGES.length) {
            StageData.Stage next = StageData.get(nextStage);

            root.addView(button(
                    "前往下一關：" + next.name,
                    v -> showBattle(nextStage)));
        } else {
            root.addView(text(
                    "目前原型的三個關卡已全部完成。",
                    16f,
                    Color.rgb(160, 220, 255)));
        }

        root.addView(button(
                "返回關卡選擇",
                v -> showStageSelect()));

        root.addView(button(
                "返回首頁",
                v -> showHome()));
    }

    @Override
    public void onBattleResolved(
            int wave,
            String enemyName,
            int enemyHp,
            int enemyMaxHp,
            int playerHp,
            int playerMaxHp,
            int enemyTurns,
            int combos,
            int totalDamage,
            int heal,
            int[] generalDamage,
            int[] skillCooldowns,
            int skillSealTurns,
            boolean waveCleared,
            boolean victory,
            boolean gameOver,
            String message) {

        StageData.Stage stage = StageData.get(currentStageIndex);

        waveLabel.setText(
                "Wave " + wave + " / " + stage.waveCount);

        playerHpLabel.setText(
                "我軍 HP：" + playerHp + " / " + playerMaxHp);

        enemyHpLabel.setText(enemyName);

        enemyTurnLabel.setText(
                "選定目標 HP：" + enemyHp + " / " + enemyMaxHp
                        + "　攻擊倒數：" + enemyTurns);

        damageBreakdownLabel.setText(
                formatDamageBreakdown(generalDamage));

        updateSkillButtons(skillCooldowns, skillSealTurns);

        if (gameOver) {
            resultLabel.setText("敗北　" + message);
            resultLabel.setTextColor(Color.rgb(255, 110, 110));
            return;
        }

        if (victory) {
            if (!battleRewardGranted) {
                battleRewardGranted = true;
                String reward = grantVictoryRewards();

                resultLabel.setText(
                        "通關！　" + message);

                resultLabel.setTextColor(
                        Color.rgb(255, 215, 100));

                root.post(() -> showVictoryResult(reward));
            }

            return;
        }

        resultLabel.setText(
                message
                        + "　Combo：" + combos
                        + "　總傷害：" + totalDamage
                        + "　回復：" + heal);

        resultLabel.setTextColor(
                waveCleared
                        ? Color.rgb(255, 215, 100)
                        : Color.LTGRAY);
    }
}
