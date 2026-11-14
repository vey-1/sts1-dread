package dread.bosses;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.EnemyMoveInfo;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.city.TheCollector;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import dread.Dread;

/** 各 Boss 在「行动前」「刷新意图数值」「回合结束」时的统一处理。 */
public final class BossHooks {
    private BossHooks() {}

    /** 每个怪物身上的恐惧模式标记。 */
    @SpirePatch(clz = AbstractMonster.class, method = SpirePatch.CLASS)
    public static class Fields {
        public static SpireField<Boolean> collectorOpened = new SpireField<>(() -> false);
        public static SpireField<Boolean> enraged = new SpireField<>(() -> false);
    }

    static void bottom(AbstractGameAction a) {
        AbstractDungeon.actionManager.addToBottom(a);
    }

    static void setHp(AbstractMonster m, int hp) {
        m.maxHealth = hp;
        m.currentHealth = hp;
    }

    // ---------------- 史莱姆老大：重击 = 28 + 3 × 黏液数 ----------------
    static final byte SLIME_SLAM = 1;

    public static int slimedCount() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) return 0;
        int n = 0;
        for (AbstractCard c : p.drawPile.group) if (Slimed.ID.equals(c.cardID)) n++;
        for (AbstractCard c : p.hand.group) if (Slimed.ID.equals(c.cardID)) n++;
        for (AbstractCard c : p.discardPile.group) if (Slimed.ID.equals(c.cardID)) n++;
        return n;
    }

    static void refreshSlimeSlam(AbstractMonster m) {
        int base = 28 + 3 * slimedCount();
        m.damage.get(1).base = base;
        if (m.nextMove == SLIME_SLAM) {
            ReflectionHacks.setPrivate(m, AbstractMonster.class, "intentBaseDmg", base);
            EnemyMoveInfo mv = ReflectionHacks.getPrivate(m, AbstractMonster.class, "move");
            if (mv != null) mv.baseDamage = base;
        }
    }

    /** 原版每次刷新意图伤害前调用；在这里改写会随牌堆变化的伤害基数。 */
    @SpirePatch(clz = AbstractMonster.class, method = "applyPowers")
    public static class ApplyPowersPatch {
        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            if (!Dread.on()) return;
            if (__instance instanceof SlimeBoss) refreshSlimeSlam(__instance);
        }
    }

    // ---------------- 行动前 ----------------
    public static boolean beforeTakeTurn(AbstractMonster m) {
        AbstractPlayer p = AbstractDungeon.player;
        if (m instanceof SlimeBoss && m.nextMove == SLIME_SLAM) {
            refreshSlimeSlam(m);
            m.applyPowers();
        } else if (m instanceof TheGuardian && m.nextMove == Act1Bosses.GUARDIAN_CHARGE_UP) {
            bottom(new DamageAction(p, m.damage.get(Act1Bosses.GUARDIAN_CHARGE_DMG_INDEX),
                    AbstractGameAction.AttackEffect.BLUNT_LIGHT));
        } else if (m instanceof TheCollector && m.nextMove == 1 && !Fields.collectorOpened.get(m)) {
            Fields.collectorOpened.set(m, true);
            bottom(new ApplyPowerAction(p, m, new WeakPower(p, 2, true), 2));
            bottom(new ApplyPowerAction(p, m, new VulnerablePower(p, 2, true), 2));
            bottom(new ApplyPowerAction(p, m, new FrailPower(p, 2, true), 2));
        }
        return true;
    }

    // ---------------- 回合结束：觉醒者一阶段，每只活着的邪教徒 +1 力量 ----------------
    @SpirePatch(clz = MonsterGroup.class, method = "applyEndOfTurnPowers")
    public static class EndOfRoundPatch {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!Dread.on()) return;
            for (AbstractMonster m : __instance.monsters) {
                if (!(m instanceof AwakenedOne) || m.isDying || m.halfDead) continue;
                Boolean form1 = ReflectionHacks.getPrivate(m, AwakenedOne.class, "form1");
                if (form1 == null || !form1) continue;
                int cultists = 0;
                for (AbstractMonster o : __instance.monsters) {
                    if (o instanceof Cultist && !o.isDying && !o.escaped && o.currentHealth > 0) cultists++;
                }
                if (cultists > 0) {
                    bottom(new ApplyPowerAction(m, m, new StrengthPower(m, cultists), cultists));
                }
            }
        }
    }

    public static DamageInfo newDamage(AbstractMonster owner, int base) {
        return new DamageInfo(owner, base);
    }
}
