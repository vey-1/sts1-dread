package dread.bosses;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;
import com.megacrit.cardcrawl.monsters.city.Champ;
import com.megacrit.cardcrawl.monsters.city.TheCollector;
import dread.Dread;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

/** 第二幕：铜人、冠军、收藏家。 */
public final class Act2Bosses {
    private Act2Bosses() {}

    // ================= 铜人：连枷 9×2，光束 55 =================
    @SpirePatch(clz = BronzeAutomaton.class, method = SpirePatch.CONSTRUCTOR)
    public static class AutomatonCtor {
        @SpirePostfixPatch
        public static void Postfix(BronzeAutomaton __instance) {
            if (!Dread.on()) return;
            ReflectionHacks.setPrivate(__instance, BronzeAutomaton.class, "flailDmg", 9);
            ReflectionHacks.setPrivate(__instance, BronzeAutomaton.class, "beamDmg", 55);
            __instance.damage.get(0).base = 9;
            __instance.damage.get(1).base = 55;
        }
    }

    // ================= 冠军：愤怒门槛 60%，愤怒 9 力量 =================
    /** 原版判断「当前血 < 最大血 / 2」；把读到的最大血放大 1.2 倍，等价于 60%。 */
    @SpirePatch(clz = Champ.class, method = "getMove")
    public static class ChampThreshold {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    if (f.isReader() && f.getFieldName().equals("maxHealth")) {
                        f.replace("{ $_ = dread.bosses.Act2Bosses.champMaxForThreshold($proceed()); }");
                    }
                }
            };
        }
    }

    public static int champMaxForThreshold(int maxHealth) {
        return Dread.on() ? Math.round(maxHealth * 1.2f) : maxHealth;
    }

    static final byte CHAMP_ANGER = 7;

    /** 愤怒给的力量 = strAmt × 3；执行愤怒那一回合临时把 strAmt 设为 3（= 9 力量）。 */
    @SpirePatch(clz = Champ.class, method = "takeTurn")
    public static class ChampAnger {
        @SpirePrefixPatch
        public static void Prefix(Champ __instance) {
            if (!Dread.on() || __instance.nextMove != CHAMP_ANGER) return;
            savedStr = ReflectionHacks.getPrivate(__instance, Champ.class, "strAmt");
            ReflectionHacks.setPrivate(__instance, Champ.class, "strAmt", 3);
        }

        @SpirePostfixPatch
        public static void Postfix(Champ __instance) {
            if (savedStr == null) return;
            ReflectionHacks.setPrivate(__instance, Champ.class, "strAmt", savedStr);
            savedStr = null;
        }

        private static Integer savedStr = null;
    }

    // ================= 收藏家：大负面各 3 层（首回合负面在 BossHooks） =================
    @SpirePatch(clz = TheCollector.class, method = SpirePatch.CONSTRUCTOR)
    public static class CollectorCtor {
        @SpirePostfixPatch
        public static void Postfix(TheCollector __instance) {
            if (Dread.on()) ReflectionHacks.setPrivate(__instance, TheCollector.class, "megaDebuffAmt", 3);
        }
    }
}
