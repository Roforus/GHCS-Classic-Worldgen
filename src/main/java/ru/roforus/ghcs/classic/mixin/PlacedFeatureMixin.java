package ru.roforus.ghcs.classic.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.roforus.ghcs.classic.ClassicWorldgen;
@Mixin(PlacedFeature.class)
public abstract class PlacedFeatureMixin {
    @Inject(method="placeWithContext",at=@At("HEAD"),cancellable=true)
    private void ghcs$classic(PlacementContext context,RandomSource random,BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        var original=(PlacedFeature)(Object)this;
        var alternate=ClassicWorldgen.replacement(original,context.getLevel().registryAccess(),context.generator());
        // Retain the original top feature, so vanilla biome filters still recognize it.
        if(alternate!=original)cir.setReturnValue(((PlacedFeatureInvoker)(Object)alternate).ghcs$place(context,random,pos));
    }
}
