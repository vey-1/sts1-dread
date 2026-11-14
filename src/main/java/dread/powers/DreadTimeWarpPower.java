package dread.powers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.TimeWarpPower;
import com.megacrit.cardcrawl.vfx.combat.TimeWarpTurnEndEffect;

/** 时间吞噬者本体用：可调张数与力量的时间扭曲（保留原版 ID，原版对 "Time Warp" 的引用照常工作）。 */
public class DreadTimeWarpPower extends TimeWarpPower {
    private final int threshold;
    private final int str;

    public DreadTimeWarpPower(AbstractCreature owner, int threshold, int str) {
        super(owner);
        this.threshold = threshold;
        this.str = str;
        updateDescription();
    }

    @Override
    public void updateDescription() {
        if (threshold <= 0) {
            super.updateDescription();
            return;
        }
        this.description = "每当你打出 #b" + threshold + " 张牌，立刻结束你的回合，并获得 #b" + str + " 点 #y力量 。";
    }

    @Override
    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
        flashWithoutSound();
        this.amount++;
        if (this.amount >= threshold) {
            this.amount = 0;
            playApplyPowerSfx();
            AbstractDungeon.actionManager.callEndTurnEarlySequence();
            CardCrawlGame.sound.play("POWER_TIME_WARP", 0.05f);
            AbstractDungeon.effectsQueue.add(new TimeWarpTurnEndEffect());
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new StrengthPower(m, str), str));
            }
        }
        updateDescription();
    }
}
