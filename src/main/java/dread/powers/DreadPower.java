package dread.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.AbstractPower;

/** 恐惧模式主题能力的公共基类：中文名、描述、借用原版图标。 */
public abstract class DreadPower extends AbstractPower {
    protected DreadPower(String id, String name, AbstractCreature owner, int amount, String iconRegion) {
        this.ID = id;
        this.name = name;
        this.owner = owner;
        this.amount = amount;
        this.type = PowerType.BUFF;
        loadRegion(iconRegion);
        updateDescription();
    }
}
