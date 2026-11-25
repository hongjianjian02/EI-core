# 灵体宝匣服务端回归

这是隔离 GameTest harness，只编译此次联动和测试类，不进入交付 JAR。需 Java 21、本项目 Gradle、`libs/lootr.jar`、`libs/neovitae.jar`、GeckoLib，以及 init.gradle 指向的实际测试实例中的 Modonomicon/Curios/SmartBrainLib。

从 EI core 项目根目录依次运行（同一 isolated gameDirectory）：

```powershell
gradle -I validation/spirit-cache-tests/init.gradle runSpiritCacheTest -PrestartStage=seed -g .gradle-home --no-daemon --console=plain
gradle -I validation/spirit-cache-tests/init.gradle runSpiritCacheTest -PrestartStage=verify -g .gradle-home --no-daemon --console=plain
```

本次环境已具备 Prism 资产缓存，通过 `build/moddev/minecraft_assets.properties` 指定 assets_root 与 asset_index=17，并给上述命令加 `-x downloadAssets`。没有缓存时让 Gradle 正常准备资产。

运行目录为 `run/spirit-cache-test`，仅测试世界会在 (32,100,32)、(34,100,32) 创建测试宝匣；不要将 gameDirectory 指向用户存档。seed 会重新创建这两个 fixture，verify 不重建它们，从同一世界的真实区块与 Lootr SavedData 读回身份/余量。两轮应分别出现 `EICORE_RESTART_SEED_PASS` / `EICORE_RESTART_VERIFY_PASS` 及 `All 2 required tests passed`。verify 可再次运行，仍应通过。

测试用连接只是服务端对象，不建立真实客户端连接。真实多人 GUI、音效、粒子和整包限制的验收清单见根目录 SPIRIT_CACHE_LOOTR.md。

`scan_resources.py` 是原始 Neo Vitae 结构与战利品表取证脚本；`make_template.py` 可重建测试空结构。它们不修改模组资源。
