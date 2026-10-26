package com.orange838.zongheng.mixin;

import com.orange838.zongheng.railway.RailwayBuilder;
import com.orange838.zongheng.railway.RailwayMap;
import com.orange838.zongheng.railway.RegionPos;
import com.orange838.zongheng.util.CurveRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 取消路线沿线结构生成
 * 结构搜索太慢了，索性直接取消距离路线过近的结构生成吧
 */

@Mixin(Structure.class)
public class StructureMixin {
    @Inject(
            method = "findValidGenerationPoint",
            at = @At("RETURN"),
            cancellable = true
    )
    private void zongheng_railway$cancelStructureByPos(
            Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir
    ) {
        Optional<Structure.GenerationStub> ret = cir.getReturnValue();
        if (ret.isEmpty()) {
            return;
        }

        Structure.GenerationStub stub = ret.get();
        BlockPos pos = stub.position();
        Structure self = (Structure) (Object) this;

        if (tongDaRailway211$shouldCancel(self, pos, context)) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Unique
    private static boolean tongDaRailway211$shouldCancel(Structure structure, BlockPos pos, Structure.GenerationContext context) {
        RailwayBuilder builder = RailwayBuilder.getInstance(context.seed());
        if (builder == null) return false;

        RailwayMap railwayMap = builder.regionRailways.get(RegionPos.regionPosFromWorldPos(pos.getX(), pos.getZ()));
        if (railwayMap == null) return false;

        Set<CurveRoute> routes = new HashSet<>();
        railwayMap.routeMap.forEach(((chunkPos, curveRoutes) -> {
            routes.addAll(curveRoutes);
        }));
        for (CurveRoute route : routes) {
            var frame = route.getFrame(pos.getCenter());
            if (frame.nearestPoint.distanceTo(pos.getCenter()) < 100) {
                return true;
            }
        }
        return false;
    }
}
