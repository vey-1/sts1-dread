package dread.powers;

import basemod.ReflectionHacks;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import dread.Dread;

/**
 * 心脏状态栏的「血弹蓄积」：只做显示，不参与结算。
 * 数字 = 按你本回合已出牌数算出的下一次血弹发数（规则见 Act3Bosses.HeartBloodShots：
 * 心脏在怪物回合结束时用你刚结束那回合的出牌数定下一次血弹）。
 */
public class BloodGaugePower extends DreadPower {
    public static final String ID = "dread:BloodGauge";
    private static final byte DEBILITATE = 3, ECHO = 2;
    private int shownCards = -1;

    public BloodGaugePower(AbstractMonster owner) {
        super(ID, "血弹蓄积", owner, projected(), "dread:blood_gauge");
    }

    public static int projected() {
        return Math.min(20, 13 + Dread.cardsThisPlayerTurn / 3);
    }

    /** 0 = 不会，1 = 50% 可能，2 = 必定。按原版选招循环推断心脏下回合的招式。 */
    private int nextIsBloodShots() {
        if (!(owner instanceof CorruptHeart)) return 1;
        CorruptHeart h = (CorruptHeart) owner;
        if (h.nextMove == DEBILITATE) return 1;
        Integer mc = ReflectionHacks.getPrivate(h, CorruptHeart.class, "moveCount");
        if (mc == null) return 1;
        switch (mc % 3) {
            case 0: return 1;                              // 新一轮：血弹或回响随机
            case 1: return h.nextMove == ECHO ? 2 : 0;     // 本轮第二招：放过回响就轮到血弹
            default: return 0;                             // 本轮第三招是增益
        }
    }

    /** 每帧刷新：出牌时数字立刻跳，回合开始清零也立刻跟上。 */
    @Override
    public void update(int slot) {
        if (shownCards != Dread.cardsThisPlayerTurn) {
            int before = amount;
            shownCards = Dread.cardsThisPlayerTurn;
            amount = projected();
            if (amount > before) flashWithoutSound();
            updateDescription();
        }
        super.update(slot);
    }

    @Override
    public void updateDescription() {
        int cards = Dread.cardsThisPlayerTurn;
        int hits = projected();
        StringBuilder sb = new StringBuilder();
        sb.append("你本回合已打出 #b").append(cards).append(" 张牌：下一次 #y血弹 为 #b").append(hits).append(" 发");
        if (hits < 20) {
            sb.append("，再打出 #b").append(3 - cards % 3).append(" 张 +1 发（最多 #b20 发）。");
        } else {
            sb.append("（已到上限）。");
        }
        int next = owner == null ? 1 : nextIsBloodShots();
        sb.append(" NL 下回合 ").append(next == 2 ? "#r必定是 #y血弹 。" : next == 1 ? "有 #b50% 可能是 #y血弹 。" : "不是 #y血弹 。");
        description = sb.toString();
    }
}
