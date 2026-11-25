# 灵体宝匣与 Lootr（EI core 1.0.36）

## 核实依据

以测试实例 build-fc4b9a5 的实际 JAR 为准，开发依赖与实例文件 SHA-256 一致：Minecraft 1.21.1、NeoForge 21.1.265、Neo Vitae 1.1.51、Lootr 1.21.1-1.11.38.127。

- Neo Vitae：`4f7c55e2e0aef1f341ba7b79596e419d49ec34ed948b724ead4a6508037047ec`。
- Lootr：`5c74de4709d1c13b25880fdd241a89d5d9f331346d20edc76f80ccd4d99dc66c`。

灵体宝匣属于 Neo Vitae，方块、方块实体和菜单注册 ID 均为 `neovitae:spirit_cache`。实现分别是 `SpiritCacheBlock`、`SpiritCacheBlockEntity`、`SpiritCacheMenu`，位于 `com.breakinblocks.neovitae.common` 对应子包。原方块实体是 27 格 `RandomizableContainer`。原开启路径先 `unpackLootTable(player)`，再播放 `VAULT_INSERT_ITEM` 并打开原菜单；默认 unpack 清除 LootTable 并把奖励放入全局库存。未发现开启或关闭删除宝匣的逻辑。关闭音效音调为 0.8，原 `animateTick` 在有物品时发出 3 个血焰粒子。

地下城 `DungeonRoom` / `DungeonStructure` 通过 StructureTemplate 放置带 LootTable 的宝匣。扫描 Neo Vitae 结构资源发现 36 个包含宝匣的模板、133 处宝匣、25 张关联战利品表；还存在只有空 Items、没有 LootTable 的装饰宝匣。可通过 `validation/spirit-cache-tests/scan_resources.py` 重建逐项证据到 `output/mod-analysis/neovitae/spirit_cache_structures.json`。原表和结构均未改写。

检查了整包 KubeJS/AStages 相关脚本；当前 `stages.js` 中 Neo Vitae 的 chapter1 规则是配方限制，未找到宝匣专属开启门禁。本实现保留原右键事件入口和 `ServerPlayer.openMenu`，没有增加阶段豁免。完整整包阶段行为仍需游戏验收。

## 实现与状态归属

当前 Lootr 配置不能直接为此自定义菜单接入个人库存。因此在 EI core 中通过官方 `ILootrBlockEntityConverter` ServiceLoader 扩展注册 provider，保留原方块和实体。Mixin 只在 Lootr、Neo Vitae 同时存在时加载，Lootr 编译依赖不打包进 EI core。API 已按实际 JAR 核对：`LootrAPI.getData`、`ILootrSavedData.getInventory(UUID)`、`createInventory(provider, UUID, filler)`、`ILootrInventory.setMenuBuilder`。

每个宝匣保存固定随机 UUID，绑定维度和位置。每次打开优先读取该宝匣 LootrSavedData 中玩家 UUID 对应库存；只有不存在时才调用 Lootr 默认 filler，使用原 LootTable、LootTableSeed、玩家 Luck 和位置上下文生成奖励，遵循 Lootr 的种子配置。本包 `randomise_seed=true`，每位玩家独立抽取；战利品内容可能偶然相同。即使启用 Lootr 团队模式，也按个人 UUID 分开保存。

奖励、剩余物品及开启记录由 Lootr 保存至主世界 `data/lootr/` 下的标准数据文件，空库存也是有效的已生成库存。宝匣自己的 NBT 只保存身份、玩家放置标记与粒子显示快照，不重复保存奖励。复制带身份的 NBT 到另一位置会分配新身份；正常区块重载和重启继续使用原身份。备份或恢复世界时应一起保留区块及 Lootr 数据，避免人为删掉领取记录。

菜单使用原 `SpiritCacheMenu`，槽位绑定 Lootr 的个人库存。关闭仍经原 `stopOpen` 播放音效。开启保留原音效；原血焰粒子按当前玩家是否还有奖励显示，已领取玩家不影响其他人的粒子状态。未新增模型或动画。服务器拒绝旁观者、假玩家及超出原容器有效范围的开启。实体的全局 unpack 被抑制，物理库存不允许自动化抽取/插入；打破宝匣不会将个人奖励作为公共物品掉出。

禁用此 provider 的自动 refresh、decay 和标记未开启行为，避免正常重开时重刷或自动消失。管理员显式重置 Lootr 存档、命令清除库存或回滚世界属于人为改变领取状态，不作为防重复发奖承诺的一部分。

## 适用范围与旧世界

默认仅接管仍带非空 LootTable ID、未标记为玩家放置的宝匣。新放置的方块通过 `setPlacedBy` 排除并持久化标记；无表空宝匣与普通手放宝匣保留原行为。没有额外配置开关。

已有世界中仍保留 LootTable 的未领取宝匣，在首次访问时惰性接入，无需重新生成区块。历史版本没有世界生成来源标记，因此使用“保留 LootTable 且非新版本记录的玩家放置”作为识别依据；管理员用命令创建带 LootTable 的宝匣也会满足该条件。

**旧版本已被全局打开的宝匣，其 LootTable 已被删除，保持原全局剩余库存，不自动补发。** 无法可靠还原原表、种子或曾经领取的玩家。管理员若决定补偿，应先备份世界，核实结构对应的原表并制定补发名单；不要批量猜表或清空 Lootr 数据来重置奖励。

## 验证结果（2026-11-26）

静态核对：实际模组元数据、注册、结构 NBT、原方块/菜单/音效路径、Lootr API 及持久化实现；构建产物检查另见交付记录。

已执行 NeoForge 独立 GameTest 服务器，加载实际 Neo Vitae/Lootr 及必要依赖，测试隔离于用户测试实例，不加载 EI core 其他功能及整包全部模组。使用两个带测试连接的 ServerPlayer 对象；没有真实网络客户端。固定奖励表生成 5 钻石、3 绿宝石，避免随机结果掩盖重复生成。

| 检查 | 实际结果 |
| --- | --- |
| 官方 converter 与两个独立库存 | 通过，A/B 各 8 件，库存对象不同 |
| A 部分领取、再次打开 | 通过，A 剩 7，B 仍 8 |
| A 清空后再次打开 | 通过，A 保持 0，B 仍 8 |
| 原菜单及其服务器有效性 | 通过，实际 open/close 原菜单 |
| 外部调用 unpack | 通过，原表保留，全局库存不生成 |
| Lootr NBT、宝匣身份往返 | 通过，空库存和未领库存保留 |
| 不同宝匣、复制 NBT 到其他位置 | 通过，各自独立身份 |
| 玩家放置标记重载、旧全局领取宝匣排除 | 通过 |
| 同 UUID 新 ServerPlayer 模拟重登 | 通过，不重刷 |
| 服务器进程退出后重新启动 | 通过，从真实区块和 Lootr 数据文件读取；A=0、B=7、第二宝匣 A=6；读取前先断言记录已存在 |

跨进程两轮各 2/2 测试通过。日志：`logs/spirit-cache-gametest-seed.log`、`logs/spirit-cache-gametest-restart.log`；共同宝匣 UUID：`3d392889-1986-4a46-b3a2-8ddfcbc218e5`。这属于运行中的服务端自动化验证，超出静态检查，但**不等于真实双客户端多人游戏实测**。

尚待在完整整包中实际验收：两位玩家同时/先后右键、双方界面与 shift 点击同步、断线重登、客户端粒子/音效/外观、阶段限制，以及同一多人服重启后的端到端行为。不能据服务端自动化宣称这些已完成。

建议在测试世界选择两个未开启世界生成宝匣 C1/C2：A 在 C1 取部分后关闭，B 开 C1 应有自己的完整奖励；A 返回应仅剩自己的物品。A 取空重开应仍为空，B 内容不变。两人开 C2 应得到独立奖励。记录坐标/余量后两人退出再登录，再完整关闭并重启服务器重复检查；未达到阶段的账号同时核对原门禁。测试完成前保留当前世界与 Lootr 数据备份。
