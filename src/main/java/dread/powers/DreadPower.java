package dread.powers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.HashMap;
import java.util.Map;

/** 恐惧模式能力的公共基类：中文名、描述；图标可借原版（region 名）或用本 mod 的图片（"dread:" 前缀）。 */
public abstract class DreadPower extends AbstractPower {
    private static final Map<String, Texture> TEXTURES = new HashMap<>();

    protected DreadPower(String id, String name, AbstractCreature owner, int amount, String icon) {
        this.ID = id;
        this.name = name;
        this.owner = owner;
        this.amount = amount;
        this.type = PowerType.BUFF;
        if (icon.startsWith("dread:")) {
            String base = "dreadResources/images/powers/" + icon.substring(6);
            Texture t48 = texture(base + "_48.png");
            Texture t128 = texture(base + "_128.png");
            this.region48 = new TextureAtlas.AtlasRegion(t48, 0, 0, t48.getWidth(), t48.getHeight());
            this.region128 = new TextureAtlas.AtlasRegion(t128, 0, 0, t128.getWidth(), t128.getHeight());
        } else {
            loadRegion(icon);
        }
        updateDescription();
    }

    private static Texture texture(String path) {
        return TEXTURES.computeIfAbsent(path, p -> {
            Texture t = new Texture(Gdx.files.internal(p));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return t;
        });
    }
}
