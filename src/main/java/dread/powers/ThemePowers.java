package dread.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.EnemyMoveInfo;

/** 十条主题里需要自定义能力的六条。触发逻辑统一在 {@link dread.themes.Themes}，这里只放状态与说明。 */
public final class ThemePowers {
    private ThemePowers() {}

    /** 史莱姆老大：首次过半血，下回合改为分裂。amount = 分出几只。 */
    public static class Split extends DreadPower {
        public static final String ID = "dread:Split";
        public boolean triggered = false;
        public boolean pending = false;

        public Split(AbstractCreature owner, int count) {
            super(ID, "分裂", owner, count, "split");
        }

        @Override
        public void updateDescription() {
            description = "第一次生命值降到一半以下时，下回合改为分裂出 #b" + amount + " 只小史莱姆。";
        }
    }

    /** 守护者：累计受伤达到门槛被打晕，之后 2 回合每回合加格挡。 */
    public static class Stagger extends DreadPower {
        public static final String ID = "dread:Stagger";
        public final int threshold;
        public final int block;
        public int taken = 0;
        public boolean triggered = false;
        public boolean pending = false;
        public int blockTurnsLeft = 0;
        public EnemyMoveInfo savedMove;
        public String savedName;

        public Stagger(AbstractCreature owner, int threshold, int block) {
            super(ID, "可被打晕", owner, threshold, "modeShift");
            this.threshold = threshold;
            this.block = block;
            updateDescription();
        }

        @Override
        public void updateDescription() {
            if (block == 0 && threshold == 0) return;
            if (triggered) {
                description = "已被打晕过。剩余 #b" + blockTurnsLeft + " 回合每回合获得 #b" + block + " 点 #y格挡 。";
            } else {
                description = "再受到 #b" + Math.max(0, threshold - taken) + " 点伤害会被 #y眩晕 ，跳过本回合意图；之后 #b2 回合每回合获得 #b"
                        + block + " 点 #y格挡 。";
            }
        }
    }

    /** 六火：首次过半血时塞灼伤。amount = 放抽牌堆顶的张数；extraDiscard = 额外进弃牌堆的张数。 */
    public static class Ember extends DreadPower {
        public static final String ID = "dread:Ember";
        public final int extraDiscard;
        public boolean triggered = false;

        public Ember(AbstractCreature owner, int top, int extraDiscard) {
            super(ID, "余烬", owner, top, "explosive");
            this.extraDiscard = extraDiscard;
            updateDescription();
        }

        @Override
        public void updateDescription() {
            description = "第一次生命值降到一半以下时，在你的抽牌堆顶放入 #b" + amount + " 张 #y灼伤"
                    + (extraDiscard > 0 ? "，弃牌堆放入 #b" + extraDiscard + " 张" : "") + "。";
        }
    }

    /** 冠军：首次过半血时清除自身负面并获得力量。 */
    public static class Rally extends DreadPower {
        public static final String ID = "dread:Rally";
        public boolean triggered = false;

        public Rally(AbstractCreature owner, int str) {
            super(ID, "背水", owner, str, "anger");
        }

        @Override
        public void updateDescription() {
            description = "第一次生命值降到一半以下时，清除自身所有负面状态并获得 #b" + amount + " 点 #y力量 。";
        }
    }

    /** 时间吞噬者：全场共用计数的时间扭曲。amount 显示当前计数，str 为该敌人每次获得的力量。 */
    public static class SharedWarp extends DreadPower {
        public static final String ID = "dread:SharedWarp";
        public static final int THRESHOLD = 12;
        public final int str;

        public SharedWarp(AbstractCreature owner, int str) {
            super(ID, "时间扭曲（共享）", owner, 0, "time");
            this.str = str;
            updateDescription();
        }

        @Override
        public void updateDescription() {
            description = "本场战斗你每累计打出 #b" + THRESHOLD + " 张牌（所有敌人共用计数），立刻结束你的回合，该敌人获得 #b"
                    + str + " 点 #y力量 。";
        }
    }

    /** 甜甜圈与十面体：有敌人被你打死时获得力量。 */
    public static class Vengeance extends DreadPower {
        public static final String ID = "dread:Vengeance";

        public Vengeance(AbstractCreature owner, int str) {
            super(ID, "同仇", owner, str, "vigor");
        }

        @Override
        public void updateDescription() {
            description = "每当有其他敌人被你打死，获得 #b" + amount + " 点 #y力量 。";
        }
    }
}
