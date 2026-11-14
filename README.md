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

## 安装
从 [Releases](https://github.com/vey-1/sts1-dread/releases) 下载 `Dread.jar`，放进游戏目录的 `mods\` 文件夹，
用 ModTheSpire 启动并勾选 BaseMod 和 Dread。对应游戏版本 `12-18-2022`（Steam 正式版）。

**游戏语言请设为简体中文。** mod 里的文字只有中文，其他语言的游戏字体可能显示不出中文（空白或方块）。

## 状态
当前版本 0.2.0：补丁全部能织入原版代码（ModTheSpire `--out-jar` 检查通过），已修复第三幕崩溃。
**平衡性尚未系统测试**，遇到崩溃或机制不对请开 issue，附上 `ModTheSpire` 日志和种子。
本仓库不包含任何游戏原版代码或资源。
