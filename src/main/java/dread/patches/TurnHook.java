package dread.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import dread.Dread;
import dread.bosses.BossHooks;
import dread.themes.Themes;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/** 在原版「敌人执行 takeTurn」之前插一个钩子；返回 false 表示本回合跳过原版行动。 */
@SpirePatch(clz = GameActionManager.class, method = "getNextAction")
public class TurnHook {
    @SpireInstrumentPatch
    public static ExprEditor Instrument() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (m.getClassName().equals(AbstractMonster.class.getName()) && m.getMethodName().equals("takeTurn")) {
                    m.replace("{ if (dread.patches.TurnHook.before($0)) { $_ = $proceed($$); } }");
                }
            }
        };
    }

    public static boolean before(AbstractMonster m) {
        if (!Dread.on()) return true;
        boolean proceed = BossHooks.beforeTakeTurn(m);
        if (!proceed) return false;
        return Themes.beforeTakeTurn(m);
    }
}
