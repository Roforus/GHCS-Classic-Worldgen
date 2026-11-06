package ru.roforus.ghcs.classic.mixin;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import ru.roforus.ghcs.classic.ClassicWorldgen;
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class CarverSelectionMixin {
    @Redirect(method="applyCarvers",at=@At(value="INVOKE",target="Lnet/minecraft/core/Holder;value()Ljava/lang/Object;"))
    private Object ghcs$carver(Holder<?> holder,WorldGenRegion region,long seed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step) {
        var value=holder.value();
        return value instanceof ConfiguredWorldCarver<?> carver?ClassicWorldgen.replacement(carver,region.registryAccess(),(NoiseBasedChunkGenerator)(Object)this):value;
    }
}
