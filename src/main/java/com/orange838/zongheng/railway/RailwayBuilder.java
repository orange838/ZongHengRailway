package com.orange838.zongheng.railway;

import com.orange838.zongheng.Config;
import com.orange838.zongheng.ZongHengRailway;
import com.orange838.zongheng.util.AdaptiveHeightSampler;
import com.orange838.zongheng.util.ModSaveData;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.common.Tags;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

import static com.orange838.zongheng.ZongHengRailway.CHUNK_GROUP_SIZE;
import static com.orange838.zongheng.railway.RailwayMap.samplingNum;

public class RailwayBuilder {
    private static RailwayBuilder instance;
    private static long seed;

    private final Map<RegionPos, Future<?>> regionFutures = new ConcurrentHashMap<>();
    public final Map<RegionPos, RailwayMap> regionRailways = new ConcurrentHashMap<>();
    public final Map<RegionPos, int[][]> regionHeightMap = new ConcurrentHashMap<>();
    private final Map<net.minecraft.resources.ResourceKey<Level>, ConcurrentMap<Long, Integer>> exactHeightCache = new ConcurrentHashMap<>();
    private final Map<net.minecraft.resources.ResourceKey<Level>, ConcurrentMap<Long, Boolean>> oceanBiomeCache = new ConcurrentHashMap<>();

    private final LinkedBlockingQueue<Runnable> regionRailwayLoadQueue = new LinkedBlockingQueue<Runnable>(); //线程池
    private final ThreadPoolExecutor regionRailwayLoadPoolExecutor = new ThreadPoolExecutor(64, 1024, 1, TimeUnit.DAYS, regionRailwayLoadQueue);
    private final WorldGenRegion level;

    /** 已经做过「周边预热」的中心区域（含当时用的半径），避免同一区域每次生成区块都重复提交。 */
    private final Set<String> preheatedRegions = ConcurrentHashMap.newKeySet();
    /** 已经被预热提交过规划的目标区域，防止相邻的几个中心区域把同一个目标算好几遍。 */
    private final Set<RegionPos> preheatedTargets = ConcurrentHashMap.newKeySet();

    private RailwayBuilder(WorldGenRegion level) {
        this.level = level;
    }
    public static synchronized RailwayBuilder getInstance(long seed, WorldGenRegion level) {
        if (instance == null || RailwayBuilder.seed != seed) {
            instance = new RailwayBuilder(level);
            RailwayBuilder.seed = seed;
        }
        return instance;
    }

    public static synchronized RailwayBuilder getInstance(long seed) {
        if (instance == null || RailwayBuilder.seed != seed) {
            return null;
        }
        return instance;
    }

    // 为区块生成铁路路线。如未生成则阻塞线程开始生成。如已生成直接返回。
    // 这里只生成规划路线，不实际放置路线！
    public void generateRailway(RegionPos regionPos) {
        // 如果路线已经生成，直接返回
        if (regionRailways.containsKey(regionPos)) {
            preheatAround(regionPos);
            return;
        }

        ServerLevel overworld = Objects.requireNonNull(level.getServer()).getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) {
            return;
        }

        // 尝试从本地数据中读取
        ModSaveData data = ModSaveData.get(overworld);
        RailwayMap savedData = data.getRailwayMap(regionPos);
        if (savedData != null) {
            regionRailways.put(regionPos, savedData);
            ZongHengRailway.LOGGER.info("Region {} Done! Read From Local Data", regionPos);
            preheatAround(regionPos);
            return;
        }

        // 如果路线还未生成...
        try {
            // 如果没有线程在生成路线，添加线程开始生成
            if (!regionFutures.containsKey(regionPos)) {
                var f = regionRailwayLoadPoolExecutor.submit(() -> {
                    // 生成铁路步骤...
                    RailwayMap railwayMap = new RailwayMap(regionPos);

                    railwayMap.startPlanningRoutes(overworld);

                    // 放置路线规划结果
                    regionRailways.put(regionPos, railwayMap);

                    //将数据保存到磁盘
                    data.putRailwayMap(regionPos, railwayMap);
                });
                regionFutures.put(regionPos, f);
            }
            // 等待线程生成
            Future<?> future = regionFutures.get(regionPos);
            int timeout = Config.planTimeoutSeconds;
            if (timeout > 0) {
                // 有超时保护：算不完就先放这个区块过去，规划仍在后台继续
                try {
                    future.get(timeout, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    ZongHengRailway.LOGGER.warn(
                            "Region {} planning is taking longer than {}s, letting this chunk generate without railway for now",
                            regionPos, timeout);
                    return;
                }
            } else {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            regionFutures.remove(regionPos);
            ZongHengRailway.LOGGER.error("Region {} planning failed", regionPos, e);
        } finally {
            if (regionRailways.containsKey(regionPos)) {
                regionFutures.remove(regionPos);
            }
        }

        // 本区域规划完成后，顺手把周边区域也提前算好，走到边界时不卡顿、两边对接点一致
        preheatAround(regionPos);
    }

    /**
     * 把当前区域周围一圈区域提交到同一个线程池异步规划。
     *
     * <p>只做规划（计算），不放置任何方块：未加载的区块不会凭空出现铁轨，
     * 等玩家真正走到那里、区块开始生成时，规划结果直接可用。
     */
    private void preheatAround(RegionPos center) {
        if (!Config.preheatAroundPlayer) {
            return;
        }
        int radius = Math.max(0, Math.min(3, Config.preheatRadius));
        if (!preheatedRegions.add(center.x() + "," + center.z() + "," + radius)) {
            return;
        }
        ServerLevel overworld = level.getServer().getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) {
            return;
        }
        ModSaveData data = ModSaveData.get(overworld);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                RegionPos target = new RegionPos(center.x() + dx, center.z() + dz);
                if (regionRailways.containsKey(target) || regionFutures.containsKey(target)) {
                    continue;
                }
                // 存档里已有结果的区域不需要重算（直接读 map，避免 getRailwayMap 里的 setDirty）
                if (data.regionRailways.containsKey(target)) {
                    continue;
                }
                // 几个中心区域的预热范围会互相重叠，这里保证同一个目标区域只提交一次
                if (!preheatedTargets.add(target)) {
                    continue;
                }
                var f = regionRailwayLoadPoolExecutor.submit(() -> {
                    try {
                        RailwayMap railwayMap = new RailwayMap(target);
                        railwayMap.startPlanningRoutes(overworld);
                        regionRailways.put(target, railwayMap);
                        data.putRailwayMap(target, railwayMap);
                    } catch (Exception e) {
                        // 失败的允许下次再试
                        preheatedTargets.remove(target);
                        ZongHengRailway.LOGGER.error("Pre-planning region {} failed", target, e);
                    } finally {
                        // 规划结果已进 regionRailways，future 不再需要留着
                        regionFutures.remove(target);
                    }
                });
                regionFutures.put(target, f);
                // 任务可能在 put 之前就结束并移除了自己，这里补一次清理，避免残留已完成的 future
                if (f.isDone()) {
                    regionFutures.remove(target, f);
                }
            }
        }
    }


    /*
     * 缓存图管理
     * 旨在删除主播之前写的错乱的各种坐标系统
     * 统一使用世界坐标
     *
     * */
    /**
     * 获取指定坐标的高度
     * @param wx 世界坐标x
     * @param wz 世界坐标z
     * @return 高度
     */
    public int getHeight(int wx, int wz) {
        RegionPos regionPos = new RegionPos(Math.floorDiv(wx, 16*CHUNK_GROUP_SIZE), Math.floorDiv(wz, 16*CHUNK_GROUP_SIZE));
        int[][] heightMap = regionHeightMap
                .computeIfAbsent(regionPos, k -> getHeightMap(level.getLevel(), regionPos));
        int px = Math.floorDiv(wx - regionPos.x()*CHUNK_GROUP_SIZE*16, 16/samplingNum);
        int pz = Math.floorDiv(wz - regionPos.z()*CHUNK_GROUP_SIZE*16, 16/samplingNum);
        return heightMap[px][pz];
    }

    /**
     * Returns an exact world-surface height and caches it for every planning stage.
     * The cache is dimension-aware because planning can query more than one level.
     */
    public int getExactHeight(ServerLevel serverLevel, int wx, int wz) {
        ConcurrentMap<Long, Integer> dimensionCache = exactHeightCache.computeIfAbsent(
                serverLevel.dimension(),
                ignored -> new ConcurrentHashMap<>()
        );
        long key = ((long) wx << 32) ^ (wz & 0xffffffffL);
        return dimensionCache.computeIfAbsent(key, ignored -> {
            ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
            RandomState randomState = serverLevel.getChunkSource().randomState();
            return generator.getBaseHeight(
                    wx,
                    wz,
                    Heightmap.Types.WORLD_SURFACE,
                    serverLevel,
                    randomState
            );
        });
    }

    /** Returns whether the quart containing the position has an ocean biome. */
    public boolean isOceanBiome(ServerLevel serverLevel, int wx, int wz) {
        ConcurrentMap<Long, Boolean> dimensionCache = oceanBiomeCache.computeIfAbsent(
                serverLevel.dimension(),
                ignored -> new ConcurrentHashMap<>()
        );
        int quartX = QuartPos.fromBlock(wx);
        int quartZ = QuartPos.fromBlock(wz);
        long key = ((long) quartX << 32) ^ (quartZ & 0xffffffffL);
        return dimensionCache.computeIfAbsent(key, ignored -> {
            ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
            RandomState randomState = serverLevel.getChunkSource().randomState();
            try {
                return generator.getBiomeSource().getNoiseBiome(
                        quartX,
                        QuartPos.fromBlock(serverLevel.getSeaLevel()),
                        quartZ,
                        randomState.sampler()
                ).is(Tags.Biomes.IS_OCEAN);
            } catch (NullPointerException exception) {
                return false;
            }
        });
    }

    private int[][] getHeightMap(ServerLevel serverLevel, RegionPos regionPos) {
        // 高度自适应采样地形高度图
        // 创建采样器：阈值=10，最大层数=3，每个节点4x4采样
        AdaptiveHeightSampler sampler = new AdaptiveHeightSampler(10, 2, 4, (x, z) -> {
            int wx = (int) (x*(16.0/samplingNum) + regionPos.x()*CHUNK_GROUP_SIZE*16);
            int wz = (int) (z*(16.0/samplingNum) + regionPos.z()*CHUNK_GROUP_SIZE*16);
            return getExactHeight(serverLevel, wx, wz);
        });

        try {
//            long startTime = System.currentTimeMillis();
            // 构建四叉树，区域大小
            sampler.buildQuadTree(CHUNK_GROUP_SIZE*samplingNum);
//            long endTime = System.currentTimeMillis();
//            sampler.printStatistics();
//            ZongHengRailway.LOGGER.info(" Region {} Build HeightMap time: {}ms", regionPos, endTime - startTime);
        } catch (InterruptedException e) {
            ZongHengRailway.LOGGER.error("Build HeightMap Err", e);
        } finally {
            sampler.shutdown();
        }

        return sampler.generateImage(CHUNK_GROUP_SIZE*samplingNum, CHUNK_GROUP_SIZE*samplingNum);
    }
}
