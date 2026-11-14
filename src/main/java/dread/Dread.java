package dread;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.SlaverBlue;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 恐惧模式的全局状态。所有补丁只通过 {@link #on()} 判断是否生效。 */
public final class Dread {
    private Dread() {}

    /** 角色选择界面上的开关（还没开局）。 */
    public static boolean selected = false;

    /** 本局是否为恐惧模式（随存档保存）。 */
    public static boolean active = false;

    /** 已经发过进阶之灾的幕。 */
    public static Set<Integer> cursedActs = new HashSet<>();

    /** 每一幕的主题 Boss（第一次在该幕战斗时记录 bossKey；第三幕双 Boss 取地图显示的第一个）。 */
    public static Map<Integer, String> actThemes = new HashMap<>();

    public static boolean on() {
        return active;
    }

    /** 本场战斗里玩家本回合已出牌数（心脏用：在怪物回合读取的是刚结束的玩家回合）。 */
    public static int cardsThisPlayerTurn = 0;

    public static void resetRun(boolean isDread) {
        active = isDread;
        cursedActs = new HashSet<>();
        actThemes = new HashMap<>();
    }

    /** 当前幕的主题 Boss key；没有记录时用当前 bossKey 并记下。 */
    public static String themeKey() {
        int act = AbstractDungeon.actNum;
        String k = actThemes.get(act);
        if (k == null && AbstractDungeon.bossKey != null) {
            k = AbstractDungeon.bossKey;
            actThemes.put(act, k);
        }
        return k;
    }

    public enum Tier { NONE, NORMAL, ELITE }

    /**
     * 这个敌人吃主题时用哪一档数值。
     * Boss 房间（本体与随从）不吃；战斗中途生成的（召唤、分裂）一律小怪值；
     * 事件战开场就在的敌人按精英值；精英房里精英本体、以及奴隶头目带的蓝/红奴隶贩子（用户 11-14 定：按精英）按精英值。
     */
    public static Tier tierOf(AbstractMonster m, boolean spawnedMidFight) {
        if (!on() || m == null) return Tier.NONE;
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || room instanceof MonsterRoomBoss) return Tier.NONE;
        if (spawnedMidFight) return Tier.NORMAL;
        if (room instanceof EventRoom) return Tier.ELITE;
        if (room instanceof MonsterRoomElite && (m.type == AbstractMonster.EnemyType.ELITE
                || m instanceof SlaverBlue || m instanceof SlaverRed)) return Tier.ELITE;
        return Tier.NORMAL;
    }

    public static int pick(Tier t, int normal, int elite) {
        return t == Tier.ELITE ? elite : normal;
    }
}
