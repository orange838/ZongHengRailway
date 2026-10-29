package com.orange838.zongheng.railway.planner;

import com.orange838.zongheng.Config;
import com.orange838.zongheng.railway.RailwayBuilder;
import com.orange838.zongheng.railway.RegionPos;
import com.orange838.zongheng.structure.ModStructureManager;
import com.orange838.zongheng.structure.StationTemplate;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.*;

/**
 * 路线图大致生成步骤：
 * 1. 使用低差异序列在区域内随机均匀的选择站点
 * 2. 去除不合法的选点（如海洋上的点）
 * 3. 将站点连接成为德劳内三角网
 * 4. 删边，使得圆周上的相邻边在特定角度以上
 * 5. 删边，使得节点的最大度为4。根据节点度数分配对应出口的车站
 * 6. 返回节点和它所连接的所有节点
 * 7. 通过匈牙利匹配为每个车站出口匹配一条边
 * 8. 根据匹配结果生成连接
 */

// 感谢@YiMin翊民排查的在73行的bug导致的一些不兼容!
// 站点规划 连接规划
public class StationPlanner {
    private final RegionPos regionPos;

    public StationPlanner(RegionPos regionPos) {
        this.regionPos = regionPos;
    }

    // 区域内站点生成
    public static List<Pair<StationGenInfo, List<BlockPos>>> generateStation(RegionPos regionPos, ServerLevel level, long seed) {
        return generateStation(regionPos, level, seed, RailwayBuilder.getInstance(seed));
    }

    public static List<Pair<StationGenInfo, List<BlockPos>>> generateStation(
            RegionPos regionPos,
            ServerLevel level,
            long seed,
            RailwayBuilder builder
    ) {
        return buildPlan(regionPos, level, seed, builder).stations();
    }

    /** 区域规划结果：车站列表 + 跨区对接点。 */
    public record RegionPlan(
            List<Pair<StationGenInfo, List<BlockPos>>> stations,
            List<BorderPortal> portals
    ) {
    }

    /** 跨区对接点：位于区域边界上的轨道对接点，相邻两个区域会算出同一个坐标与高度。 */
    public record BorderPortal(
            BlockPos pos,
            Vec3 inwardDir,
            int nodeIndex
    ) {
    }

    public static RegionPlan buildPlan(
            RegionPos regionPos,
            ServerLevel level,
            long seed,
            RailwayBuilder builder
    ) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState cfg = level.getChunkSource().randomState();

        long regionSeed = seed + regionPos.hashCode()*53217L + 75_512;
        List<Pair<StationGenInfo, List<BlockPos>>> result = new ArrayList<>();

        // 判定区域是否生成路网
        // 全世界生成开关：开启时跳过距离上限与概率门槛，每个区域都会生成路网
        if (!Config.generateRailwayEverywhere) {
            if (Math.abs(regionPos.x()) > Config.maxRegionDistance || Math.abs(regionPos.z()) > Config.maxRegionDistance) {
                // 过远不生成
                return new RegionPlan(result, List.of());
            }
            if (new Random(regionSeed).nextDouble() < 1 - Config.areaGenRailwayProbability) {
                return new RegionPlan(result, List.of());
            }
        }

        var nodes = RouteGraph.generate(
                regionPos.getBasePos().x + 60,
                regionPos.getBasePos().x + regionPos.getLength() - 60,
                regionPos.getBasePos().y + 60,
                regionPos.getBasePos().y + regionPos.getLength() - 60,
                new Random(regionSeed).nextInt(8, 13),
                regionSeed,
                70,
                4,
                (x, z) -> {
                    try {
                        var biome = gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock((int) x), QuartPos.fromBlock(65), QuartPos.fromBlock((int) z), cfg.sampler());
                        return !biome.is(Tags.Biomes.IS_OCEAN) && !biome.is(Tags.Biomes.IS_RIVER);
                    } catch (NullPointerException e) {
                        return true;
                    }
                }
        );

        List<StationElevationPlanner.StationElevation> elevations = builder == null
                ? null
                : StationElevationPlanner.plan(nodes, builder, level);

        // 跨区对接点：每条边界一个，相邻两区算出同一个点，两边的轨道在边界处头对头接上
        List<BorderPortal> portals = planPortals(regionPos, level, builder, nodes);
        Map<Integer, List<BlockPos>> portalByNode = new HashMap<>();
        for (BorderPortal portal : portals) {
            portalByNode.computeIfAbsent(portal.nodeIndex(), k -> new ArrayList<>()).add(portal.pos());
        }

        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            RouteGraph.NodeData node = nodes.get(nodeIndex);
            StationElevationPlanner.StationElevation elevation = elevations == null
                    ? null
                    : elevations.get(nodeIndex);
            int x = (int) node.point.x;
            int z = (int) node.point.z;
            int h = elevation == null ? 0 : elevation.height();
            node.setPointY(h);

            // 根据高度决定生成地上还是地下车站
            boolean hasPortal = portalByNode.containsKey(nodeIndex);
            int exitNum = (node.connected.size() >= 3 || hasPortal) ? 4 : 2;
            StationTemplate station;
            long stationSeed = regionSeed + x* 3L + z* 7L;
            if (elevation != null && elevation.underground()) {
                station = ModStructureManager.getRandomUnderGroundStation(stationSeed, exitNum);
            } else {
                station = ModStructureManager.getRandomNormalStation(stationSeed, exitNum);
            }

            int finalH = h;
            BlockPos placePos = new BlockPos(x, h, z);
            List<BlockPos> connectedStations = new ArrayList<>(node.connected.stream()
                    .map(p -> new BlockPos((int) p.x, finalH, (int) p.z))
                    .toList());
            connectedStations.addAll(portalByNode.getOrDefault(nodeIndex, List.of()));
            if (exitNum == 2) {
                station = selectExitRotation(station, placePos, connectedStations);
            }
            result.add(new Pair<>(
                    new StationGenInfo(station, placePos),
                    connectedStations
            ));
        }

        return new RegionPlan(result, portals);
    }

    // 跨区对接点规划
    private static List<BorderPortal> planPortals(
            RegionPos regionPos,
            ServerLevel level,
            RailwayBuilder builder,
            List<RouteGraph.NodeData> nodes
    ) {
        List<BorderPortal> portals = new ArrayList<>();
        if (!Config.connectAcrossRegions || builder == null || nodes.isEmpty()) {
            return portals;
        }
        int length = regionPos.getLength();
        int baseX = (int) regionPos.getBasePos().x;
        int baseZ = (int) regionPos.getBasePos().y;

        for (int side = 0; side < 4; side++) {
            boolean vertical = side < 2;
            int edgeIndex = vertical
                    ? (side == 0 ? regionPos.x() : regionPos.x() + 1)
                    : (side == 2 ? regionPos.z() : regionPos.z() + 1);
            int axis = vertical ? 0 : 1;
            int along = vertical ? regionPos.z() : regionPos.x();
            Random random = new Random(seedOfEdge(edgeIndex, along, axis));

            int margin = Math.min(200, Math.max(1, length / 8));
            int t = margin + random.nextInt(Math.max(1, length - margin * 2));

            int x;
            int z;
            Vec3 inward;
            if (vertical) {
                x = side == 0 ? baseX : baseX + length;
                z = baseZ + t;
                inward = new Vec3(side == 0 ? 1 : -1, 0, 0);
            } else {
                x = baseX + t;
                z = side == 2 ? baseZ : baseZ + length;
                inward = new Vec3(0, 0, side == 2 ? 1 : -1);
            }

            int y = Math.max(builder.getExactHeight(level, x, z), level.getSeaLevel() + 2);
            int nodeIndex = nearestNode(nodes, x, z);
            if (nodeIndex < 0) {
                continue;
            }
            portals.add(new BorderPortal(new BlockPos(x, y, z), inward, nodeIndex));
        }
        return portals;
    }

    private static int nearestNode(List<RouteGraph.NodeData> nodes, int x, int z) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < nodes.size(); i++) {
            RouteGraph.NodeData node = nodes.get(i);
            double dx = node.point.x - x;
            double dz = node.point.z - z;
            double dist = dx * dx + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    private static long seedOfEdge(int edgeIndex, int along, int axis) {
        long h = 1125899906842597L;
        h = h * 31 + edgeIndex;
        h = h * 31 + along;
        h = h * 31 + axis;
        return h;
    }

    // 路线连接规则生成
    public List<ConnectionGenInfo> generateConnections(ServerLevel level, long seed) {
        List<ConnectionGenInfo> result = new ArrayList<>();
        Map<Integer, List<StationTemplate.Exit>> connect = new HashMap<>();

        RegionPlan plan = buildPlan(regionPos, level, seed, RailwayBuilder.getInstance(seed));
        List<Pair<StationGenInfo, List<BlockPos>>> thisStations = plan.stations();

        for (Pair<StationGenInfo, List<BlockPos>> stationGen : thisStations) {
            var station = stationGen.getFirst();
            var connected = stationGen.getSecond();

            var matching = minDistanceMatching(station.getExits(), connected);
            for (Pair<StationTemplate.Exit, BlockPos> exitBlockPosPair : matching) {
                var exit = exitBlockPosPair.getFirst();
                var con = exitBlockPosPair.getSecond();
                int id = generateLineId(station.placePos.getX(), station.placePos.getZ(), con.getX(), con.getZ());
                connect.computeIfAbsent(id, k -> new ArrayList<>()).add(exit);
            }

//            var tpos = station.placePos;
//            int exitnum = matching.size();
//            ZongHengRailway.LOGGER.info("====> StationPlanner: {} {} {} {}, exit {}, rotation {}", tpos.getX(), tpos.getY(), tpos.getZ(), regionPos, exitnum, station.stationTemplate.rotation);
        }

        // 跨区对接点：作为虚拟出口参与配对，使相邻区域的路网在边界处连通
        for (BorderPortal portal : plan.portals()) {
            StationGenInfo station = plan.stations().get(portal.nodeIndex()).getFirst();
            int id = generateLineId(
                    station.placePos().getX(), station.placePos().getZ(),
                    portal.pos().getX(), portal.pos().getZ()
            );
            connect.computeIfAbsent(id, k -> new ArrayList<>())
                    .add(new StationTemplate.Exit(portal.pos(), portal.inwardDir()));
        }

        connect.forEach((id, con) -> {
            if (con.size() == 2) {
                var exitA = con.getFirst();
                var exitB = con.getLast();
                result.add(new ConnectionGenInfo(
                        Vec3.atLowerCornerOf(exitA.exitPos()),
                        exitA.dir(),
                        Vec3.atLowerCornerOf(exitB.exitPos()),
                        exitB.dir(),
                        exitA.pushAway(exitB.exitPos().getCenter()),
                        exitB.pushAway(exitA.exitPos().getCenter())
                ));
            }
        });

        return result;
    }

    static StationTemplate selectExitRotation(
            StationTemplate station,
            BlockPos placePos,
            List<BlockPos> connected
    ) {
        StationTemplate best = station;
        double bestScore = rotationAngleScore(station.getExitsPos(placePos), placePos, connected);

        for (StationTemplate candidate : ModStructureManager.getStationRotations(station)) {
            double score = rotationAngleScore(candidate.getExitsPos(placePos), placePos, connected);
            if (score < bestScore - 1.0e-12) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static double rotationAngleScore(
            List<StationTemplate.Exit> exits,
            BlockPos stationPos,
            List<BlockPos> connected
    ) {
        List<Pair<StationTemplate.Exit, BlockPos>> matching = minDistanceMatching(exits, connected);
        Vec3 stationCenter = Vec3.atCenterOf(stationPos);
        double score = 0.0;
        int scoredEdges = 0;

        for (Pair<StationTemplate.Exit, BlockPos> pair : matching) {
            Vec3 routeDirection = Vec3.atCenterOf(pair.getSecond())
                    .subtract(stationCenter)
                    .multiply(1.0, 0.0, 1.0);
            if (routeDirection.lengthSqr() < 1.0e-12) {
                continue;
            }

            double dot = pair.getFirst().dir().normalize().dot(routeDirection.normalize());
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
            score += angle * angle;
            scoredEdges++;
        }

        return scoredEdges == 0 ? Double.POSITIVE_INFINITY : score;
    }

    public static List<Pair<StationTemplate.Exit, BlockPos>> minDistanceMatching(
            List<StationTemplate.Exit> a,
            List<BlockPos> b
    ) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return new ArrayList<>();
        }

        boolean swapped = false;
        List<StationTemplate.Exit> exits = a;
        List<BlockPos> blocks = b;

        int n = exits.size();
        int m = blocks.size();

        if (n > m) {
            swapped = true;
        }

        int rows = Math.min(n, m);
        int cols = Math.max(n, m);

        double[][] cost = new double[rows][cols];

        if (!swapped) {
            for (int i = 0; i < rows; i++) {
                BlockPos p1 = exits.get(i).exitPos();
                for (int j = 0; j < cols; j++) {
                    cost[i][j] = p1.distSqr(blocks.get(j));
                }
            }
        } else {
            for (int i = 0; i < rows; i++) {
                BlockPos p1 = blocks.get(i);
                for (int j = 0; j < cols; j++) {
                    cost[i][j] = p1.distSqr(exits.get(j).exitPos());
                }
            }
        }

        int[] match = hungarian(cost);

        List<Pair<StationTemplate.Exit, BlockPos>> result = new ArrayList<>();

        if (!swapped) {
            for (int i = 0; i < rows; i++) {
                int j = match[i];
                if (j >= 0) {
                    result.add(new Pair<>(exits.get(i), blocks.get(j)));
                }
            }
        } else {
            for (int i = 0; i < rows; i++) {
                int j = match[i];
                if (j >= 0) {
                    result.add(new Pair<>(exits.get(j), blocks.get(i)));
                }
            }
        }

        return result;
    }

    private static int[] hungarian(double[][] cost) {
        int n = cost.length;
        int m = cost[0].length;

        double[] u = new double[n + 1];
        double[] v = new double[m + 1];
        int[] p = new int[m + 1];
        int[] way = new int[m + 1];

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;

            double[] minv = new double[m + 1];
            boolean[] used = new boolean[m + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);

            do {
                used[j0] = true;
                int i0 = p[j0];

                double delta = Double.POSITIVE_INFINITY;
                int j1 = 0;

                for (int j = 1; j <= m; j++) {
                    if (!used[j]) {
                        double cur = cost[i0 - 1][j - 1] - u[i0] - v[j];

                        if (cur < minv[j]) {
                            minv[j] = cur;
                            way[j] = j0;
                        }

                        if (minv[j] < delta) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                }

                for (int j = 0; j <= m; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }

                j0 = j1;
            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] match = new int[n];
        Arrays.fill(match, -1);

        for (int j = 1; j <= m; j++) {
            if (p[j] != 0) {
                match[p[j] - 1] = j - 1;
            }
        }

        return match;
    }

    private static int generateLineId(double x1, double y1, double x2, double y2) {
        // 无向点对 id：旧实现用两点确定的直线 (A,B,C) 作 key，
        // 三点共线时不同边会撞同一个 id，导致 con.size() != 2 而被整条丢弃。
        int ax = (int) Math.round(x1), az = (int) Math.round(y1);
        int bx = (int) Math.round(x2), bz = (int) Math.round(y2);
        if (ax > bx || (ax == bx && az > bz)) {
            int tx = ax; ax = bx; bx = tx;
            int tz = az; az = bz; bz = tz;
        }
        long h = (long) ax * 0x9E3779B1L
                ^ (long) az * 0x85EBCA77L
                ^ (long) bx * 0xC2B2AE3DL
                ^ (long) bz * 0x27D4EB2FL;
        h ^= (h >>> 29);
        h *= 0x165667B19E3779F9L;
        h ^= (h >>> 32);
        return (int) h;
    }

    // 站点放置信息(世界坐标系)
    public record StationGenInfo(
            StationTemplate stationTemplate,
            BlockPos placePos
    ) {
        public List<StationTemplate.Exit> getExits() {
            return stationTemplate.getExitsPos(placePos);
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("id", stationTemplate.getId());
            tag.putString("type", stationTemplate.getType().name());
            tag.putInt("x", placePos.getX());
            tag.putInt("y", placePos.getY());
            tag.putInt("z", placePos.getZ());
            return tag;
        }

        public static StationGenInfo fromNBT(CompoundTag tag) {
            int id = tag.getInt("id");
            int x = tag.getInt("x");
            int y = tag.getInt("y");
            int z = tag.getInt("z");
            StationTemplate.StationType type = StationTemplate.StationType.valueOf(tag.getString("type"));
            StationTemplate stationTemplate = ModStructureManager.station2.getById(id);
            if (stationTemplate == null)
                stationTemplate = ModStructureManager.station4.getById(id);

            return new StationGenInfo(stationTemplate, new BlockPos(x, y, z));
        }
    }

    /**
     * @param start        起点坐标
     * @param startDir     起点方向
     * @param end          终点坐标
     * @param endDir       终点方向
     * @param connectStart 寻路起点
     * @param connectEnd   寻路终点
     */ // 路线连接信息(世界坐标系)
    public record ConnectionGenInfo(
            Vec3 start,
            Vec3 startDir,
            Vec3 end,
            Vec3 endDir,
            int[] connectStart,
            int[] connectEnd
    ) {

    }
}
