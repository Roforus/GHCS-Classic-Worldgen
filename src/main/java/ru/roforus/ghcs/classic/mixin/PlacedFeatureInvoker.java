package ru.roforus.ghcs.classic.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(PlacedFeature.class)
public interface PlacedFeatureInvoker {
    @Invoker("placeWithContext") boolean ghcs$place(PlacementContext context,RandomSource random,BlockPos origin);
}
