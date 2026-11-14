package dread.ui;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import dread.Dread;

/** 角色选择界面：进阶选择右边的恐惧模式开关。开启时把进阶锁在 A20。 */
public final class DreadToggle {
    private DreadToggle() {}

    private static final float SIZE = 64.0f;
    private static Texture iconOn;
    private static Texture iconOff;
    private static Hitbox hb;
    private static float pulse = 0f;

    public static void loadTextures() {
        iconOn = new Texture(Gdx.files.internal("dreadResources/images/dread_on.png"));
        iconOff = new Texture(Gdx.files.internal("dreadResources/images/dread_off.png"));
        iconOn.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        iconOff.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private static boolean anySelected(CharacterSelectScreen s) {
        for (CharacterOption o : s.options) {
            if (o.selected) return true;
        }
        return false;
    }

    private static Hitbox position(CharacterSelectScreen s) {
        Hitbox right = ReflectionHacks.getPrivate(s, CharacterSelectScreen.class, "ascRightHb");
        if (hb == null) hb = new Hitbox(SIZE * Settings.scale, SIZE * Settings.scale);
        if (right != null) hb.move(right.cX + 75.0f * Settings.scale, right.cY);
        return hb;
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "update")
    public static class UpdatePatch {
        @SpirePostfixPatch
        public static void Postfix(CharacterSelectScreen __instance) {
            if (!anySelected(__instance)) return;
            Hitbox h = position(__instance);
            h.update();
            if (h.justHovered) CardCrawlGame.sound.playA("UI_HOVER", -0.3f);
            if (h.hovered && InputHelper.justClickedLeft) {
                h.clickStarted = true;
            }
            if (h.clicked) {
                h.clicked = false;
                Dread.selected = !Dread.selected;
                CardCrawlGame.sound.play("UI_CLICK_1");
            }
            if (Dread.selected) {
                __instance.isAscensionMode = true;
                __instance.ascensionLevel = 20;
                __instance.ascLevelInfoString = CharacterSelectScreen.A_TEXT[19];
                pulse += Gdx.graphics.getDeltaTime();
            }
        }
    }

    @SpirePatch(clz = CharacterSelectScreen.class, method = "render")
    public static class RenderPatch {
        @SpirePostfixPatch
        public static void Postfix(CharacterSelectScreen __instance, SpriteBatch sb) {
            if (!anySelected(__instance) || iconOn == null) return;
            Hitbox h = position(__instance);
            float s = Settings.scale * (h.hovered ? 1.08f : 1.0f);
            if (Dread.selected) s *= 1.0f + 0.04f * (float) Math.sin(pulse * 3.0f);
            Texture t = Dread.selected ? iconOn : iconOff;
            sb.setColor(Color.WHITE);
            float w = SIZE;
            sb.draw(t, h.cX - w / 2f, h.cY - w / 2f, w / 2f, w / 2f, w, w, s, s, 0f,
                    0, 0, t.getWidth(), t.getHeight(), false, false);
            // 用户 11-14 定：选角界面只保留按钮，规则说明放在 mod 描述里。
            h.render(sb);
        }
    }
}
