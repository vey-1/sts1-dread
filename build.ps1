# 编译并打包 Dread.jar；加 -Install 会复制到游戏 mods 目录。
# 用法：powershell -ExecutionPolicy Bypass -File build.ps1 [-Install]
param([switch]$Install)
$ErrorActionPreference = 'Stop'

$Root = $PSScriptRoot
$Game = 'D:\Steam\steamapps\common\SlayTheSpire'
$Workshop = 'D:\Steam\steamapps\workshop\content\646570'
$Libs = @(
    "$Game\desktop-1.0.jar",
    "$Workshop\1605060445\ModTheSpire.jar",
    "$Workshop\1605833019\BaseMod.jar"
)
foreach ($l in $Libs) { if (-not (Test-Path $l)) { throw "找不到依赖：$l" } }

$Out = Join-Path $Root 'build'
$Classes = Join-Path $Out 'classes'
if (Test-Path $Classes) { Remove-Item -Recurse -Force $Classes }
New-Item -ItemType Directory -Force -Path $Classes | Out-Null

$Sources = Get-ChildItem -Recurse -Filter *.java (Join-Path $Root 'src\main\java') | ForEach-Object { $_.FullName }
$ListFile = Join-Path $Out 'sources.txt'
[System.IO.File]::WriteAllLines($ListFile, $Sources)

& javac --release 8 -encoding UTF-8 -nowarn -cp ($Libs -join ';') -d $Classes "@$ListFile"
if ($LASTEXITCODE -ne 0) { throw "javac 失败" }

Copy-Item -Recurse -Force (Join-Path $Root 'src\main\resources\*') $Classes
Copy-Item -Force (Join-Path $Root 'ModTheSpire.json') $Classes

$Jar = Join-Path $Out 'Dread.jar'
if (Test-Path $Jar) { Remove-Item -Force $Jar }
& jar cf $Jar -C $Classes .
if ($LASTEXITCODE -ne 0) { throw "jar 失败" }
Write-Host "已生成 $Jar"

if ($Install) {
    Copy-Item -Force $Jar (Join-Path $Game 'mods\Dread.jar')
    Write-Host "已复制到 $Game\mods\Dread.jar"
}
