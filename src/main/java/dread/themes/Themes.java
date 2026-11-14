package dread.themes;

import basemod.ReflectionHacks;
import com.badlogic.gdx.math.Rectangle;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.SpawnMonsterAction;
import com.megacrit.cardcrawl.actions.unique.RemoveDebuffsAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.EnemyMoveInfo;
import com.megacrit.cardcrawl.monsters.ending.SpireSpear;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_M;
import com.megacrit.cardcrawl.monsters.exordium.AcidSlime_S;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_L;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_M;
import com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_S;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.BeatOfDeathPower;
import com.megacrit.cardcrawl.powers.CuriosityPower;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import dread.Dread;
import dread.Dread.Tier;
import dread.powers.ThemePowers;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.ArrayList;
import java.util.List;

/** 每一幕的小怪/精英主题能力（数值见 docs/plan.md）。 */
public final class Themes {
    private Themes() {}

    // 原版 bossKey
    static final String SLIME = "Slime Boss", GUARDIAN = "The Guardian", HEXAGHOST = "Hexaghost";
    static final String AUTOMATON = "Automaton", CHAMP = "Champ", COLLECTOR = "Collector";
    static final String AWAKENED = "Awakened One", TIME_EATER = "Time Eater", DONU_DECA = "Donu and Deca";
    static final String HEART = "The Heart";

    static final byte STUN_MOVE = -77;
    static final byte SPLIT_MOVE = -78;

    /** 共用的时间扭曲计数（每场战斗清零）。 */
    static int sharedWarpCount = 0;

    @SpirePatch(clz = AbstractMonster.class, method = SpirePatch.CLASS)
    public static class Fields {
        public static SpireField<Integer> hpBefore = new SpireField<>(() -> 0);
        public static SpireField<Boolean> themed = new SpireField<>(() -> false);
    }

    static void bottom(AbstractGameAction a) {
        AbstractDungeon.actionManager.addToBottom(a);
    }

    static boolean isSlime(AbstractMonster m) {
        return m instanceof SlimeBoss || m instanceof AcidSlime_L || m instanceof AcidSlime_M || m instanceof AcidSlime_S
                || m instanceof SpikeSlime_L || m instanceof SpikeSlime_M || m instanceof SpikeSlime_S;
    }

    static boolean alive(AbstractMonster m) {
        return !m.isDying && !m.escaped && !m.halfDead && m.currentHealth > 0;
    }

    // ===================== 战斗开始 / 生成时挂能力 =====================

    public static void onBattleStart(AbstractRoom room) {
        sharedWarpCount = 0;
        if (room instanceof MonsterRoomBoss) return;
        if (COLLECTOR.equals(Dread.themeKey())) {
            boolean elite = room instanceof MonsterRoomElite || room instanceof EventRoom;
            int n = elite ? 2 : 1;
            AbstractPlayer p = AbstractDungeon.player;
            bottom(new ApplyPowerAction(p, null, new WeakPower(p, n, true), n));
            bottom(new ApplyPowerAction(p, null, new FrailPower(p, n, true), n));
        }
    }

    /** 战斗开始时每个敌人都会调用（原版 useUniversalPreBattleAction）。 */
    @SpirePatch(clz = AbstractMonster.class, method = "useUniversalPreBattleAction")
    public static class PreBattle {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            applyTo(__instance, true);
        }
    }

    /** 战斗中途生成的敌人（召唤、分裂）。 */
    @SpirePatch(clz = SpawnMonsterAction.class, method = "update")
    public static class OnSpawn {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("init") && m.getClassName().equals(AbstractMonster.class.getName())) {
                        m.replace("{ $proceed($$); dread.themes.Themes.applyTo($0, false); }");
                    }
                }
            };
        }
    }

    static void power(AbstractMonster m, AbstractPower p) {
        bottom(new ApplyPowerAction(m, m, p, p.amount));
    }

    public static void applyTo(AbstractMonster m, boolean battleStart) {
        Tier t = Dread.tierOf(m);
        if (t == Tier.NONE || Fields.themed.get(m)) return;
        Fields.themed.set(m, true);
        String key = Dread.themeKey();
        if (key == null) return;
        switch (key) {
            case SLIME:
                if (!isSlime(m)) power(m, new ThemePowers.Split(m, Dread.pick(t, 1, 2)));
                break;
            case GUARDIAN: {
                float thr = t == Tier.ELITE ? 0.50f : 0.40f;
                float blk = t == Tier.ELITE ? 0.35f : 0.25f;
                int block = Math.round(m.maxHealth * blk);
                if (t == Tier.NORMAL) block = Math.max(5, block);
                power(m, new ThemePowers.Stagger(m, Math.max(1, Math.round(m.maxHealth * thr)), block));
                break;
            }
            case HEXAGHOST:
                power(m, new ThemePowers.Ember(m, 1, Dread.pick(t, 0, 1)));
                break;
            case AUTOMATON:
                power(m, new ArtifactPower(m, Dread.pick(t, 1, 2)));
                break;
            case CHAMP:
                power(m, new ThemePowers.Rally(m, Dread.pick(t, 2, 3)));
                break;
            case AWAKENED:
                power(m, new CuriosityPower(m, Dread.pick(t, 1, 2)));
                break;
            case TIME_EATER:
                power(m, new ThemePowers.SharedWarp(m, Dread.pick(t, 1, 2)));
                break;
            case DONU_DECA: {
                int str = Dread.pick(t, 2, 3);
                power(m, new ThemePowers.Vengeance(m, str));
                if (battleStart && AbstractDungeon.getMonsters() != null
                        && AbstractDungeon.getMonsters().monsters.size() == 1) {
                    power(m, new StrengthPower(m, str));
                }
                break;
            }
            case HEART:
                if (m instanceof SpireSpear) power(m, new BeatOfDeathPower(m, 1));
                break;
            default:
                break;
        }
    }

    // ===================== 受伤后：过半血触发、打晕计数、被打死 =====================

    @SpirePatch(clz = AbstractMonster.class, method = "damage")
    public static class AfterDamage {
        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance, DamageInfo info) {
            Fields.hpBefore.set(__instance, __instance.currentHealth);
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance, DamageInfo info) {
            if (!Dread.on()) return;
            afterDamage(__instance, info, Fields.hpBefore.get(__instance) - __instance.currentHealth);
        }
    }

    static void afterDamage(AbstractMonster m, DamageInfo info, int lost) {
        if (lost <= 0) return;
        if (m.isDying || m.currentHealth <= 0) {
            boolean selfInflicted = info != null && info.owner == m;
            boolean byOtherMonster = info != null && info.owner instanceof AbstractMonster && info.owner != m;
            if (m.isDying && !selfInflicted && !byOtherMonster) onKilledByPlayer(m);
            return;
        }
        boolean half = m.currentHealth <= m.maxHealth / 2;

        ThemePowers.Split split = (ThemePowers.Split) m.getPower(ThemePowers.Split.ID);
        if (split != null && half && !split.triggered) {
            split.triggered = true;
            split.pending = true;
            split.flash();
            m.setMove("分裂", SPLIT_MOVE, AbstractMonster.Intent.UNKNOWN);
            m.createIntent();
        }

        ThemePowers.Ember ember = (ThemePowers.Ember) m.getPower(ThemePowers.Ember.ID);
        if (ember != null && half && !ember.triggered) {
            ember.triggered = true;
            ember.flash();
            bottom(new MakeTempCardInDrawPileAction(new Burn(), ember.amount, false, true));
            if (ember.extraDiscard > 0) bottom(new MakeTempCardInDiscardAction(new Burn(), ember.extraDiscard));
        }

        ThemePowers.Rally rally = (ThemePowers.Rally) m.getPower(ThemePowers.Rally.ID);
        if (rally != null && half && !rally.triggered) {
            rally.triggered = true;
            rally.flash();
            bottom(new RemoveDebuffsAction(m));
            bottom(new ApplyPowerAction(m, m, new StrengthPower(m, rally.amount), rally.amount));
        }

        ThemePowers.Stagger st = (ThemePowers.Stagger) m.getPower(ThemePowers.Stagger.ID);
        if (st != null && !st.triggered) {
            st.taken += lost;
            if (st.taken >= st.threshold) {
                st.triggered = true;
                st.pending = true;
                st.flash();
                EnemyMoveInfo mv = ReflectionHacks.getPrivate(m, AbstractMonster.class, "move");
                st.savedMove = mv == null ? null
                        : new EnemyMoveInfo(mv.nextMove, mv.intent, mv.baseDamage, mv.multiplier, mv.isMultiDamage);
                st.savedName = m.moveName;
                m.setMove("眩晕", STUN_MOVE, AbstractMonster.Intent.STUN);
                m.createIntent();
            }
            st.updateDescription();
        }
    }

    static void onKilledByPlayer(AbstractMonster dead) {
        if (AbstractDungeon.getMonsters() == null) return;
        for (AbstractMonster o : AbstractDungeon.getMonsters().monsters) {
            if (o == dead || !alive(o)) continue;
            AbstractPower v = o.getPower(ThemePowers.Vengeance.ID);
            if (v != null) {
                v.flash();
                bottom(new ApplyPowerAction(o, o, new StrengthPower(o, v.amount), v.amount));
            }
        }
    }

    // ===================== 敌人行动前：眩晕、分裂、打晕后的格挡 =====================

    public static boolean beforeTakeTurn(AbstractMonster m) {
        ThemePowers.Stagger st = (ThemePowers.Stagger) m.getPower(ThemePowers.Stagger.ID);
        if (st != null) {
            if (st.pending) {
                st.pending = false;
                st.blockTurnsLeft = 2;
                if (st.savedMove != null) {
                    m.setMove(st.savedName, st.savedMove.nextMove, st.savedMove.intent, st.savedMove.baseDamage,
                            st.savedMove.multiplier, st.savedMove.isMultiDamage);
                    m.createIntent();
                } else {
                    m.rollMove();
                    m.createIntent();
                }
                st.updateDescription();
                return false;
            }
            if (st.blockTurnsLeft > 0) {
                st.blockTurnsLeft--;
                bottom(new GainBlockAction(m, m, st.block));
                st.updateDescription();
            }
        }

        ThemePowers.Split split = (ThemePowers.Split) m.getPower(ThemePowers.Split.ID);
        if (split != null && split.pending) {
            split.pending = false;
            spawnSlimes(m, split.amount);
            m.rollMove();
            m.createIntent();
            return false;
        }
        return true;
    }

    // ===================== 史莱姆分裂与摆位 =====================

    static final float SLIME_W = 130.0f, SLIME_H = 100.0f, GAP = 10.0f;

    static void spawnSlimes(AbstractMonster parent, int count) {
        List<Rectangle> taken = new ArrayList<>();
        for (AbstractMonster o : AbstractDungeon.getMonsters().monsters) {
            if (!o.isDying && !o.escaped) taken.add(new Rectangle(o.hb.x, o.hb.y, o.hb.width, o.hb.height));
        }
        for (int i = 0; i < count; i++) {
            float[] pos = findSpot(parent, taken);
            float w = SLIME_W * Settings.scale, h = SLIME_H * Settings.scale;
            taken.add(new Rectangle(pos[0] - w / 2f, pos[1], w, h));
            float offX = (pos[0] - Settings.WIDTH * 0.75f) / Settings.xScale;
            float offY = (pos[1] - AbstractDungeon.floorY) / Settings.yScale;
            AbstractMonster s = AbstractDungeon.miscRng.randomBoolean()
                    ? new AcidSlime_S(offX, offY, 0)
                    : new SpikeSlime_S(offX, offY, 0);
            bottom(new SpawnMonsterAction(s, false));
        }
    }

    /** 返回 {中心 x, 脚底 y}。先找两侧，再往上叠。 */
    static float[] findSpot(AbstractMonster parent, List<Rectangle> taken) {
        float w = SLIME_W * Settings.scale, h = SLIME_H * Settings.scale, g = GAP * Settings.scale;
        float baseY = AbstractDungeon.floorY;
        float minX = Settings.WIDTH * 0.42f, maxX = Settings.WIDTH * 0.98f;
        float[][] candidates = {
                {parent.hb.x + parent.hb.width + g + w / 2f, baseY},
                {parent.hb.x - g - w / 2f, baseY},
                {parent.hb.cX, parent.hb.y + parent.hb.height + g},
                {parent.hb.cX, parent.hb.y + parent.hb.height + 2 * g + h},
                {parent.hb.cX, parent.hb.y - g - h},
        };
        for (float[] c : candidates) {
            if (c[0] - w / 2f < minX || c[0] + w / 2f > maxX) continue;
            if (c[1] < 0 || c[1] + h > Settings.HEIGHT * 0.95f) continue;
            Rectangle r = new Rectangle(c[0] - w / 2f, c[1], w, h);
            boolean overlap = false;
            for (Rectangle t : taken) {
                if (t.overlaps(r)) { overlap = true; break; }
            }
            if (!overlap) return c;
        }
        // 实在放不下：叠在最上方
        float top = parent.hb.y + parent.hb.height;
        for (Rectangle t : taken) top = Math.max(top, t.y + t.height);
        return new float[]{parent.hb.cX, top + g};
    }

    // ===================== 共享时间扭曲 =====================

    public static void onCardUsed(AbstractCard card) {
        if (AbstractDungeon.getMonsters() == null) return;
        List<AbstractMonster> holders = new ArrayList<>();
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (alive(m) && m.hasPower(ThemePowers.SharedWarp.ID)) holders.add(m);
        }
        if (holders.isEmpty()) return;
        sharedWarpCount++;
        boolean fire = sharedWarpCount >= ThemePowers.SharedWarp.THRESHOLD;
        if (fire) sharedWarpCount = 0;
        for (AbstractMonster m : holders) {
            ThemePowers.SharedWarp p = (ThemePowers.SharedWarp) m.getPower(ThemePowers.SharedWarp.ID);
            p.amount = sharedWarpCount;
            if (fire) {
                p.flash();
                bottom(new ApplyPowerAction(m, m, new StrengthPower(m, p.str), p.str));
            }
        }
        if (fire) AbstractDungeon.actionManager.callEndTurnEarlySequence();
    }
}
