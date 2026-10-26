# ZongHeng Railway 纵横铁路

> **本仓库是 [TongDaRailway（通达铁路）](https://github.com/Hello-LuckyHuang/TongDaRailway) 的非官方分支。**
> 原作者：hello_luckyhuang、MarbleGate、MagicalChao。原项目以 GNU GPL-3.0 授权，本分支同样以 GNU GPL-3.0 授权，
> 并保留原作者的版权与署名。本分支的修改由 orange838 维护。

这是一个添加自动生成机械动力（Create）铁路的模组：在世界生成阶段自动选址建站、规划线路、架桥开隧道、铺设路基与曲线铁轨。

**本分支只维护 1.21.1 NeoForge 版本。**

**模组更新修改地形生成时，使用老版本模组的世界在删除已生成的非必要区块前，请勿更新新版本模组。**

## 与原版的差异

原版自 1.1.0 起加入了「区域生成概率」限制（`areaGenerationRailwayProbability`，默认 0.4），
也就是只有部分区域会生成铁路，其余区域按概率跳过。本分支把「全世界生成」做成了**开关**：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `generateRailwayEverywhere` | `true` | 总开关。`true` 时**每个铁路区域都会生成路网**，忽略下面的概率与距离上限（恢复 1.1.0 之前的行为）；`false` 时按原版规则受限生成 |
| `areaGenerationRailwayProbability` | `0.4` | 仅在总开关为 `false` 时生效。单个区域生成铁路的概率，0 表示永不生成，1 表示总是生成 |
| `maxRegionDistance` | `100` | 仅在总开关为 `false` 时生效。距世界原点多远的区域仍会生成铁路（单位：区域，1 区域 = 4096 格，100 约等于 40 万格半径） |
| `enableTrackSpawner` | `true` | 铁轨生成器方块是否能工作 |
| `generateTrackSpawner` | `true` | 是否生成铁轨生成器方块 |
| `placeTracksUsingTrackSpawner` | `true` | 用铁轨生成器（true）还是在世界生成阶段直接铺轨（false） |

配置文件位于 `.minecraft/config/zongheng_railway-common.toml`，**改完需要重开世界或重启游戏**。
注意：开关只影响**尚未生成**的区域，已生成区块里的路网结果会被存档数据固定下来。

## 路线生成算法（原文，1.1.0）

* 使用低差异序列在区域内随机均匀的选择站点
* 去除不合法的选点（如海洋上的点）
* 将站点连接成为德劳内三角网
* 删边，使得圆周上的相邻边在特定角度以上
* 删边，使得节点的最大度为 4。根据节点度数分配对应出口的车站
* 返回节点和它所连接的所有节点
* 通过匈牙利匹配为每个车站出口匹配一条边
* 根据匹配结果生成连接

## 自行构建

需要 **JDK 21**（工程用 Gradle 8.8 + `net.neoforged.moddev`）。源码中文注释使用 **GBK** 编码编译，
`build.gradle` 中 `options.encoding = 'GBK'`，用 IDEA 打开前请把文件编码设为 GBK，否则中文注释会乱码。

国内网络下 `maven.neoforged.net` 经常无法访问，`gradle/wrapper/gradle-wrapper.properties` 已指向镜像。
若依赖下载失败，可加 `--init-script gradle/china-mirror.init.gradle` 把 NeoForge 仓库重定向到 BMCLAPI 镜像：

```bash
./gradlew --init-script gradle/china-mirror.init.gradle build
```

产物在 `build/libs/zongheng_railway-<version>.jar`。

## 许可

GNU General Public License v3.0，见 [LICENSE](LICENSE)。分发本模组的修改版时必须继续使用 GPL-3.0 并提供完整对应源码，
同时保留原作者的版权声明与署名。