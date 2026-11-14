package dread.powers;

import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.beyond.Deca;
import com.megacrit.cardcrawl.monsters.beyond.Donu;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;
import com.megacrit.cardcrawl.monsters.city.Champ;
import com.megacrit.cardcrawl.monsters.city.TheCollector;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import dread.Dread;

/** 放在被恐惧模式改过的 Boss 状态栏上，悬停可看本 Boss 的改动。纯说明，不参与结算。 */
public class DreadBossPower extends DreadPower {
    public static final String ID = "dread:Boss";
    private final String text;

    private DreadBossPower(AbstractMonster owner, String text) {
        super(ID, "恐惧", owner, -1, "dread:dread_boss");
        this.text = text;
        updateDescription();
    }

    @Override
    public void updateDescription() {
        description = text == null ? "" : text;
    }

    /** 直接挂到能力列表（不走施加动作），避免开场多一段动画，也不会被人工制品抵消。 */
    public static void applyIfBoss(AbstractMonster m) {
        if (!Dread.on() || m == null || m.hasPower(ID)) return;
        String t = describe(m);
        if (t != null) m.powers.add(new DreadBossPower(m, t));
    }

    static String describe(AbstractMonster m) {
        if (m instanceof SlimeBoss)
            return "#y猛撞 伤害 = #b28 + #b3 × 你抽牌堆、手牌、弃牌堆里的 #y黏液 张数。生命降到 #b65 以下时分裂。";
        if (m instanceof TheGuardian)
            return "#y蓄能 时同时攻击 #b8 ； #y猛烈痛击 伤害 #b40 ；第一次进入 #y防御模式 只需受到 #b30 点伤害。";
        if (m instanceof Hexaghost)
            return "开场 #b6 段攻击每段伤害 = 你当前生命 ÷ #b10 + #b1 。 #y灼烧 塞入的 #y灼伤 有一张放在你的抽牌堆顶。";
        if (m instanceof BronzeAutomaton)
            return "多段攻击 #b9 × #b2 ； #y超能光束 #b55 。";
        if (m instanceof Champ)
            return "生命低于 #b60% 时发怒，发怒获得 #b9 点 #y力量 。";
        if (m instanceof TheCollector)
            return "第 #b2 回合就施放强力负面（ #y虚弱 、 #y易伤 、 #y脆弱 各 #b5 层）。";
        if (m instanceof AwakenedOne)
            return "第一形态时，每回合结束，每只存活的 #y邪教徒 使其获得 #b1 点 #y力量 （带入第二形态）。";
        if (m instanceof TimeEater)
            return "生命 #b450 ； #y时间扭曲 只需 #b10 张牌。";
        if (m instanceof Donu || m instanceof Deca)
            return "生命 #b240 ；第 #b1 回合两者都攻击；一方死亡后，另一方获得 #b3 点 #y力量 且之后只攻击。";
        if (m instanceof CorruptHeart)
            return "#y血弹 发数 = #b13 + 你上回合打出的牌数 ÷ #b3 （最多 #b20 发）。";
        return null;
    }
}
