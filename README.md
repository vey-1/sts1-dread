# Dread 恐惧模式（Slay the Spire 1）

把《杀戮尖塔 2》v0.113.0 的「恐惧模式（Dread）」思路做到塔 1 的 ModTheSpire mod。
数值与机制按塔 1 重新设计，不是逐项照搬。

- 角色选择界面、进阶选择右边的红色裂瞳图标：点一下开启，进阶锁定 A20。
- 10 个 Boss 机制强化；每一幕的小怪 / 精英获得本幕 Boss 的主题能力（精英数值更高）。
- 每打完一幕 Boss，牌组多一张进阶之灾。
- 不开开关时与原版完全一致。

完整数值表见 [docs/plan.md](docs/plan.md)。

## 依赖
ModTheSpire、BaseMod。

## 构建
需要 JDK 9+（`javac --release 8`），以及本机的游戏和创意工坊依赖（路径在 `build.ps1` 顶部）：

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1 -Install
```

图标由 `tools/make_icons.py` 生成（纯标准库）。

## 状态
第一版（0.1.0）：补丁全部能织入原版代码（ModTheSpire `--out-jar` 检查通过），**尚未实际进游戏打过**。平衡性未测。
本仓库不包含任何游戏原版代码或资源。
