package dread.bosses;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.Deca;
import com.megacrit.cardcrawl.monsters.beyond.Donu;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.powers.StrengthPower;
import dread.Dread;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.NewExpr;

/** 第三幕：时间吞噬者、甜甜圈与十面体（觉醒者在 BossHooks）；第四幕：心脏。 */
public final class Act3Bosses {
    private Act3Bosses() {}

    // ================= 时间吞噬者：450 血，时间扭曲 10 张 =================
    @SpirePatch(clz = TimeEater.class, method = SpirePatch.CONSTRUCTOR)
    public static class TimeEaterCtor {
        @SpirePostfixPatch
        public static void Postfix(TimeEater __instance) {
            if (Dread.on()) BossHooks.setHp(__instance, 450);
        }
    }

    @SpirePatch(clz = TimeEater.class, method = "usePreBattleAction")
    public static class TimeEaterWarp {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(NewExpr e) throws CannotCompileException {
                    if (e.getClassName().equals("com.megacrit.cardcrawl.powers.TimeWarpPower")) {
                        e.replace("{ $_ = dread.bosses.Act3Bosses.timeWarp($1); }");
                    }
                }
            };
        }
    }

    public static com.megacrit.cardcrawl.powers.TimeWarpPower timeWarp(com.megacrit.cardcrawl.core.AbstractCreature owner) {
        if (!Dread.on()) return new com.megacrit.cardcrawl.powers.TimeWarpPower(owner);
        return new dread.powers.DreadTimeWarpPower(owner, 10, 2);
    }

    // ================= 甜甜圈与十面体 =================
    static final byte BEAM = 0;

    @SpirePatch(clz = Donu.class, method = SpirePatch.CONSTRUCTOR)
    public static class DonuCtor {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (!Dread.on()) return;
            BossHooks.setHp(__instance, 240);
            // 原版甜甜圈第一回合放增益；恐惧模式第一回合也攻击，之后照原版交替。
            ReflectionHacks.setPrivate(__instance, Donu.class, "isAttacking", true);
        }
    }

    @SpirePatch(clz = Deca.class, method = SpirePatch.CONSTRUCTOR)
    public static class DecaCtor {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            if (Dread.on()) BossHooks.setHp(__instance, 240);
        }
    }

    @SpirePatch(clz = Donu.class, method = "getMove")
    public static class DonuEnragedMove {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Donu __instance, int num) {
            return enragedBeam(__instance);
        }
    }

    @SpirePatch(clz = Deca.class, method = "getMove")
    public static class DecaEnragedMove {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Deca __instance, int num) {
            return enragedBeam(__instance);
        }
    }

    static SpireReturn<Void> enragedBeam(AbstractMonster m) {
        if (!Dread.on() || !BossHooks.Fields.enraged.get(m)) return SpireReturn.Continue();
        // 十面体的光束会塞眩晕，原版用「攻击+负面」图标；甜甜圈是纯攻击。
        AbstractMonster.Intent intent = m instanceof Deca ? AbstractMonster.Intent.ATTACK_DEBUFF : AbstractMonster.Intent.ATTACK;
        m.setMove(BEAM, intent, m.damage.get(0).base, 2, true);
        return SpireReturn.Return();
    }

    @SpirePatch(clz = Donu.class, method = "die")
    public static class DonuDie {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            enragePartner(__instance);
        }
    }

    @SpirePatch(clz = Deca.class, method = "die")
    public static class DecaDie {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            enragePartner(__instance);
        }
    }

    static void enragePartner(AbstractMonster dead) {
        if (!Dread.on() || AbstractDungeon.getMonsters() == null) return;
        for (AbstractMonster o : AbstractDungeon.getMonsters().monsters) {
            if (o == dead || o.isDying || o.currentHealth <= 0) continue;
            if (!(o instanceof Donu) && !(o instanceof Deca)) continue;
            if (BossHooks.Fields.enraged.get(o)) continue;
            BossHooks.Fields.enraged.set(o, true);
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(o, o, new StrengthPower(o, 3), 3));
            o.rollMove();
            o.createIntent();
        }
    }

    // ================= 心脏：血弹发数 = min(20, 13 + 上回合出牌数 / 3) =================
    @SpirePatch(clz = CorruptHeart.class, method = "getMove")
    public static class HeartBloodShots {
        @SpirePrefixPatch
        public static void Prefix(CorruptHeart __instance, int num) {
            if (!Dread.on()) return;
            int hits = Math.min(20, 13 + Dread.cardsThisPlayerTurn / 3);
            ReflectionHacks.setPrivate(__instance, CorruptHeart.class, "bloodHitCount", hits);
        }
    }
}
