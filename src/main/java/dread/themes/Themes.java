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
import com.megacrit.cardcrawl.monsters.exordium.GremlinWizard;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.monsters.exordium.Looter;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
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
import dread.powers.DreadBossPower;
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
    static final byte RALLY_MOVE = -79;

    /** 打晕后每回合格挡的硬上限（用户 11-14 定：绝对不能超过 20，原版守护者防御形态也是 20）。 */
    static final int STAGGER_BLOCK_CAP = 20;

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
            DreadBossPower.applyIfBoss(__instance);
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
        Tier t = Dread.tierOf(m, !battleStart);
        if (t == Tier.NONE || Fields.themed.get(m)) return;
        Fields.themed.set(m, true);
        String key = Dread.themeKey();
        if (key == null) return;
        switch (key) {
            case SLIME:
                if (!isSlime(m)) power(m, new ThemePowers.Proliferate(m, Dread.pick(t, 1, 2)));
                break;
            case GUARDIAN: {
                float thr = t == Tier.ELITE ? 0.50f : 0.40f;
                float blk = t == Tier.ELITE ? 0.25f : 0.20f;
                int block = Math.round(m.maxHealth * blk);
                if (t == Tier.NORMAL) block = Math.max(5, block);
                block = Math.min(STAGGER_BLOCK_CAP, block);
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

        // 增殖：不改意图，下回合行动前额外生成小史莱姆，然后照常出招。
        ThemePowers.Proliferate pro = (ThemePowers.Proliferate) m.getPower(ThemePowers.Proliferate.ID);
        if (pro != null && half && !pro.triggered) {
            pro.triggered = true;
            pro.pending = true;
            pro.flash();
            pro.updateDescription();
        }

        ThemePowers.Ember ember = (ThemePowers.Ember) m.getPower(ThemePowers.Ember.ID);
        if (ember != null && half && !ember.triggered) {
            ember.triggered = true;
            ember.flash();
            bottom(new MakeTempCardInDrawPileAction(new Burn(), ember.amount, false, true));
            if (ember.extraDiscard > 0) bottom(new MakeTempCardInDiscardAction(new Burn(), ember.extraDiscard));
        }

        // 背水：下回合改为「清负面 + 力量」，原本的招式顺延一回合（开场招不会被吞掉）。
        ThemePowers.Rally rally = (ThemePowers.Rally) m.getPower(ThemePowers.Rally.ID);
        if (rally != null && half && !rally.triggered) {
            rally.triggered = true;
            rally.pending = true;
            rally.flash();
            whenIntentSettled(() -> {
                if (!alive(m)) return;
                rally.savedMove = saveMove(m);
                rally.savedName = m.moveName;
                m.setMove(ThemePowers.Rally.NAME, RALLY_MOVE, AbstractMonster.Intent.BUFF);
                m.createIntent();
            });
        }

        ThemePowers.Stagger st = (ThemePowers.Stagger) m.getPower(ThemePowers.Stagger.ID);
        if (st != null && !st.triggered) {
            st.taken += lost;
            if (st.taken >= st.threshold) {
                st.triggered = true;
                st.flash();
                if (m instanceof Lagavulin && !Boolean.TRUE.equals(
                        ReflectionHacks.getPrivate(m, Lagavulin.class, "isOutTriggered"))) {
                    // 沉睡中的乐加维林：原版这一下就会把它打醒，下回合是「苏醒」（什么都不做），
                    // 这就当作眩晕；苏醒那回合不给格挡，之后 2 回合给。
                    st.blockDelay = 1;
                    st.blockTurnsLeft = 2;
                } else {
                    st.pending = true;
                    whenIntentSettled(() -> {
                        if (!alive(m)) return;
                        st.savedMove = saveMove(m);
                        m.setMove("眩晕", STUN_MOVE, AbstractMonster.Intent.STUN);
                        m.createIntent();
                    });
                }
            }
            st.updateDescription();
        }
    }

    static EnemyMoveInfo saveMove(AbstractMonster m) {
        EnemyMoveInfo mv = ReflectionHacks.getPrivate(m, AbstractMonster.class, "move");
        return mv == null ? null : new EnemyMoveInfo(mv.nextMove, mv.intent, mv.baseDamage, mv.multiplier, mv.isMultiDamage);
    }

    /** 去掉招式历史末尾的一条（我们临时插入的「眩晕」「背水」），不影响原版的连招判断。 */
    static void popHistory(AbstractMonster m) {
        if (m.moveHistory != null && !m.moveHistory.isEmpty()) m.moveHistory.remove(m.moveHistory.size() - 1);
    }

    /**
     * 改意图的时机：玩家回合里立即改；怪物回合里（例如荆棘反伤打到门槛）原版随后还会执行
     * RollMoveAction 重掷意图，所以排到队尾，等重掷之后再改，避免被覆盖。
     */
    static void whenIntentSettled(Runnable r) {
        if (!AbstractDungeon.actionManager.turnHasEnded) {
            r.run();
            return;
        }
        bottom(new AbstractGameAction() {
            @Override
            public void update() {
                r.run();
                isDone = true;
            }
        });
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

    // ===================== 敌人行动前：眩晕、背水、增殖、打晕后的格挡 =====================

    public static boolean beforeTakeTurn(AbstractMonster m) {
        ThemePowers.Stagger st = (ThemePowers.Stagger) m.getPower(ThemePowers.Stagger.ID);
        if (st != null) {
            if (st.pending) {
                // 眩晕回合：这一招被「吃掉」，像已经出过一样往下走（重掷下一招），之后 2 回合给格挡。
                st.pending = false;
                st.blockTurnsLeft = 2;
                if (m.nextMove != STUN_MOVE) {
                    // 怪物在被打晕后自己改了意图（例如大史莱姆过半血改为分裂）：不拦它，眩晕作废，只保留之后的格挡。
                    st.updateDescription();
                } else {
                    popHistory(m); // 去掉「眩晕」，历史末尾回到被吃掉的那一招
                    advanceAfterSkippedMove(m, st.savedMove == null ? (byte) -1 : st.savedMove.nextMove);
                    m.createIntent();
                    st.updateDescription();
                    return false;
                }
            }
            if (st.blockDelay > 0) {
                st.blockDelay--;
            } else if (st.blockTurnsLeft > 0) {
                st.blockTurnsLeft--;
                bottom(new GainBlockAction(m, m, st.block));
                st.updateDescription();
            }
        }

        ThemePowers.Rally rally = (ThemePowers.Rally) m.getPower(ThemePowers.Rally.ID);
        if (rally != null && rally.pending) {
            rally.pending = false;
            bottom(new RemoveDebuffsAction(m));
            bottom(new ApplyPowerAction(m, m, new StrengthPower(m, rally.amount), rally.amount));
            if (m.nextMove != RALLY_MOVE) {
                // 怪物自己改了意图（例如鸟被打落）：只给增益，照它自己的意图行动。
                return true;
            }
            EnemyMoveInfo s = rally.savedMove;
            popHistory(m); // 背水
            if (s != null) {
                popHistory(m); // 原招在历史里只算一次
                m.setMove(rally.savedName, s.nextMove, s.intent, s.baseDamage, s.multiplier, s.isMultiDamage);
            } else {
                m.rollMove();
            }
            m.createIntent();
            return false;
        }

        ThemePowers.Proliferate pro = (ThemePowers.Proliferate) m.getPower(ThemePowers.Proliferate.ID);
        if (pro != null && pro.pending) {
            pro.pending = false;
            pro.updateDescription();
            spawnSlimes(m, pro.amount);
        }
        return true;
    }

    /**
     * 被打晕吃掉一招之后，定下一招。多数怪在 getMove 里按招式历史选招，直接重掷即可；
     * 少数第一幕的怪在 takeTurn 里推进自己的状态，跳过 takeTurn 会卡住，这里替它们推进一步。
     * （守护者主题只在第一幕生效，所以只需覆盖第一幕的怪。）
     */
    static void advanceAfterSkippedMove(AbstractMonster m, byte skipped) {
        if (m instanceof Looter) {
            // 抢劫(1) ×2 → 突刺(4) 或烟雾弹(2) → 烟雾弹(2) → 逃跑(3)
            if (skipped == 4) { m.setMove((byte) 2, AbstractMonster.Intent.DEFEND); return; }
            if (skipped == 2 || skipped == 3) { m.setMove((byte) 3, AbstractMonster.Intent.ESCAPE); return; }
        } else if (m instanceof GremlinWizard) {
            if (skipped == 1) ReflectionHacks.setPrivate(m, GremlinWizard.class, "currentCharge", 0); // 终极冲击被吃掉，重新蓄力
        } else if (m instanceof Lagavulin) {
            if (skipped == 1) ReflectionHacks.setPrivate(m, Lagavulin.class, "debuffTurnCount", 0); // 灵魂虹吸被吃掉
        } else if (m instanceof SlaverRed) {
            if (skipped == 2) ReflectionHacks.setPrivate(m, SlaverRed.class, "usedEntangle", true); // 缠绕被吃掉
        }
        m.rollMove();
    }

    // ===================== 增殖：小史莱姆摆位 =====================

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
        float top = parent.hb.y + parent.hb.height;
        for (Rectangle t : taken) top = Math.max(top, t.y + t.height);
        return new float[]{parent.hb.cX, top + g};
    }

    // ===================== 共享时间扭曲 =====================

    /**
     * 由每个持有者的能力在「出牌结算后」调用（与原版时间扭曲同一时机）；只有排在最前的存活持有者负责计数，
     * 其余持有者直接返回。不能在 BaseMod 的「出牌时」事件里结束回合：那时原版还要读出牌队列，会崩溃。
     */
    public static void onAfterUseCard(ThemePowers.SharedWarp caller) {
        if (AbstractDungeon.getMonsters() == null) return;
        List<AbstractMonster> holders = new ArrayList<>();
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (alive(m) && m.hasPower(ThemePowers.SharedWarp.ID)) holders.add(m);
        }
        if (holders.isEmpty() || holders.get(0) != caller.owner) return;
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
