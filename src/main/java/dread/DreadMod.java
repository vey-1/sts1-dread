package dread;

import basemod.BaseMod;
import basemod.abstracts.CustomSavable;
import basemod.interfaces.OnCardUseSubscriber;
import basemod.interfaces.OnPlayerTurnStartSubscriber;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostDungeonInitializeSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.curses.AscendersBane;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import dread.themes.Themes;
import dread.ui.DreadToggle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@SpireInitializer
public class DreadMod implements PostInitializeSubscriber, PostDungeonInitializeSubscriber,
        PostBattleSubscriber, OnStartBattleSubscriber, OnCardUseSubscriber, OnPlayerTurnStartSubscriber {

    public static void initialize() {
        DreadMod mod = new DreadMod();
        BaseMod.subscribe(mod);
        BaseMod.addSaveField("dread:state", new CustomSavable<SaveData>() {
            @Override
            public SaveData onSave() {
                SaveData d = new SaveData();
                d.active = Dread.active;
                d.cursedActs = new ArrayList<>(Dread.cursedActs);
                d.actThemes = new HashMap<>(Dread.actThemes);
                return d;
            }

            @Override
            public void onLoad(SaveData d) {
                Dread.resetRun(d != null && d.active);
                if (d != null) {
                    if (d.cursedActs != null) Dread.cursedActs = new HashSet<>(d.cursedActs);
                    if (d.actThemes != null) Dread.actThemes = new HashMap<>(d.actThemes);
                }
            }
        });
    }

    public static class SaveData {
        public boolean active;
        public List<Integer> cursedActs;
        public Map<Integer, String> actThemes;
    }

    @Override
    public void receivePostInitialize() {
        DreadToggle.loadTextures();
    }

    /** 只在新开一局时触发（读档不触发，读档由 onLoad 恢复）。 */
    @Override
    public void receivePostDungeonInitialize() {
        if (!com.megacrit.cardcrawl.core.CardCrawlGame.loadingSave
                && AbstractDungeon.floorNum <= 1 && AbstractDungeon.actNum <= 1) {
            Dread.resetRun(Dread.selected);
        }
    }

    @Override
    public void receiveOnBattleStart(AbstractRoom room) {
        Dread.cardsThisPlayerTurn = 0;
        if (!Dread.on()) return;
        Dread.themeKey();
        Themes.onBattleStart(room);
    }

    /** 诅咒叠加：每打完一幕 Boss（1–3 幕，每幕一次）加 1 张进阶之灾。 */
    @Override
    public void receivePostBattle(AbstractRoom room) {
        if (!Dread.on()) return;
        if (!(room instanceof MonsterRoomBoss)) return;
        int act = AbstractDungeon.actNum;
        if (act < 1 || act > 3 || Dread.cursedActs.contains(act)) return;
        Dread.cursedActs.add(act);
        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new AscendersBane(),
                Settings.WIDTH / 2.0f, Settings.HEIGHT / 2.0f));
    }

    @Override
    public void receiveCardUsed(AbstractCard card) {
        Dread.cardsThisPlayerTurn++;
        if (Dread.on()) Themes.onCardUsed(card);
    }

    @Override
    public void receiveOnPlayerTurnStart() {
        Dread.cardsThisPlayerTurn = 0;
    }
}
