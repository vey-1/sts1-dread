package dread.bosses;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import dread.Dread;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

/** 第一幕：史莱姆老大、守护者、六火。 */
public final class Act1Bosses {
    private Act1Bosses() {}

    // ================= 史莱姆老大 =================
    @SpirePatch(clz = SlimeBoss.class, method = SpirePatch.CONSTRUCTOR)
    public static class SlimeCtor {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (Dread.on()) BossHooks.setHp(__instance, 160);
        }
    }

    /** 原版判断「当前血/最大血 ≤ 0.5」；恐惧模式把分母换成 130，等价于血 ≤ 65 分裂。 */
    @SpirePatch(clz = SlimeBoss.class, method = "damage")
    public static class SlimeSplitThreshold {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (f.isReader() && f.getFieldName().equals("maxHealth")) {
                        f.replace("{ $_ = dread.bosses.Act1Bosses.slimeSplitDenominator($proceed()); }");
                    }
                }
            };
        }
    }

    public static int slimeSplitDenominator(int maxHealth) {
        return Dread.on() ? 130 : maxHealth;
    }

    // ================= 守护者 =================
    public static final byte GUARDIAN_CHARGE_UP = 6;
    public static final int GUARDIAN_CHARGE_DMG_INDEX = 4;

    @SpirePatch(clz = TheGuardian.class, method = SpirePatch.CONSTRUCTOR)
    public static class GuardianCtor {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (!Dread.on()) return;
            ReflectionHacks.setPrivate(__instance, TheGuardian.class, "fierceBashDamage", 40);
            __instance.damage.get(0).base = 40;
            ReflectionHacks.setPrivate(__instance, TheGuardian.class, "dmgThreshold", 30);
            while (__instance.damage.size() < GUARDIAN_CHARGE_DMG_INDEX) {
                __instance.damage.add(BossHooks.newDamage(__instance, 0));
            }
            __instance.damage.add(BossHooks.newDamage(__instance, 8));
        }
    }

    /** 守护者的所有「蓄力」意图改成「攻击 8 + 格挡」。 */
    @SpirePatch(clz = TheGuardian.class, method = "getMove")
    public static class GuardianChargeIntentGetMove {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return chargeEditor();
        }
    }

    @SpirePatch(clz = TheGuardian.class, method = "changeState")
    public static class GuardianChargeIntentChangeState {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return chargeEditor();
        }
    }

    /** 旋风斩之后接「蓄力」的意图在原版私有方法 useWhirlwind 里设置。 */
    @SpirePatch(clz = TheGuardian.class, method = "useWhirlwind")
    public static class GuardianChargeIntentAfterWhirlwind {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return chargeEditor();
        }
    }

    static ExprEditor chargeEditor() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getMethodName().equals("setMove")
                        && m.getSignature().equals("(Ljava/lang/String;BLcom/megacrit/cardcrawl/monsters/AbstractMonster$Intent;)V")) {
                    m.replace("{ if (!dread.bosses.Act1Bosses.guardianSetMove($0, $1, $2)) { $proceed($$); } }");
                }
            }
        };
    }

    public static boolean guardianSetMove(AbstractMonster m, String name, byte move) {
        if (!Dread.on() || move != GUARDIAN_CHARGE_UP) return false;
        m.setMove(name, move, AbstractMonster.Intent.ATTACK_DEFEND, 8);
        return true;
    }

    // ================= 六火 =================
    /** 分割：每下 = 玩家当前血 / 10 + 1（原版 / 12 + 1）。 */
    @SpirePatch(clz = Hexaghost.class, method = "takeTurn")
    public static class HexaghostTurn {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (f.isWriter() && f.getFieldName().equals("base")
                            && f.getClassName().equals("com.megacrit.cardcrawl.cards.DamageInfo")) {
                        f.replace("{ $proceed(dread.bosses.Act1Bosses.dividerPerHit($1)); }");
                    }
                }

                @Override
                public void edit(NewExpr e) throws CannotCompileException {
                    if (e.getClassName().equals(MakeTempCardInDiscardAction.class.getName())
                            && e.getSignature().equals("(Lcom/megacrit/cardcrawl/cards/AbstractCard;I)V")) {
                        e.replace("{ $_ = dread.bosses.Act1Bosses.sear($1, $2); }");
                    }
                }
            };
        }
    }

    public static int dividerPerHit(int vanilla) {
        if (!Dread.on()) return vanilla;
        return AbstractDungeon.player.currentHealth / 10 + 1;
    }

    /** 灼烧：一张放抽牌堆顶，其余进弃牌堆。 */
    public static MakeTempCardInDiscardAction sear(AbstractCard burn, int amount) {
        if (!Dread.on() || amount < 1) return new MakeTempCardInDiscardAction(burn, amount);
        AbstractDungeon.actionManager.addToBottom(
                new MakeTempCardInDrawPileAction(burn.makeStatEquivalentCopy(), 1, false, true));
        return new MakeTempCardInDiscardAction(burn, amount - 1);
    }
}
