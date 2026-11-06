package ru.roforus.ghcs.classic;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

@Mod("ghcs_classic")
public final class ClassicWorldgen {
    public static final String ID="ghcs_classic";
    public ClassicWorldgen(IEventBus bus) {
        prepare();bus.addListener(EventPriority.LOWEST,ExternalPack::find);
    }
    public static boolean isClassic(ChunkGenerator generator) {
        return generator instanceof NoiseBasedChunkGenerator noise && noise.generatorSettings().unwrapKey().map(k->k.location().getNamespace().equals(ID)).orElse(false);
    }
    public static PlacedFeature replacement(PlacedFeature original,RegistryAccess access,ChunkGenerator generator) {
        if(!isClassic(generator))return original;
        var registry=access.registryOrThrow(Registries.PLACED_FEATURE);var key=registry.getKey(original);
        if(key==null || key.getNamespace().equals(ID))return original;
        if(!key.getNamespace().equals("minecraft") && !key.equals(ResourceLocation.parse("create:zinc_ore")))return original;
        var alternate=registry.get(ResourceLocation.fromNamespaceAndPath(ID,key.getPath()));return alternate==null?original:alternate;
    }
    public static ConfiguredWorldCarver<?> replacement(ConfiguredWorldCarver<?> original,RegistryAccess access,ChunkGenerator generator) {
        if(!isClassic(generator))return original;
        var registry=access.registryOrThrow(Registries.CONFIGURED_CARVER);var key=registry.getKey(original);
        if(key==null || !key.getNamespace().equals("minecraft"))return original;
        var alternate=registry.get(ResourceLocation.fromNamespaceAndPath(ID,key.getPath()));return alternate==null?original:alternate;
    }
    private static void prepare() {
        var settings=ExternalConfig.read("worldgen.json").getAsJsonObject();
        var templates=ExternalConfig.read("templates.json").getAsJsonObject();
        var files=new LinkedHashMap<String,JsonElement>();
        int min=settings.get("minimum_y").getAsInt(), max=settings.get("maximum_y_exclusive").getAsInt();
        if(min%16!=0 || max%16!=0 || min < -2032 || max>2032 || max<=min || max-min>4064) {
            ExternalConfig.warn("Invalid world height; using Y=0..319 for this launch");min=0;max=320;
        }
        for(var entry:templates.entrySet()) {
            String path=entry.getKey();if(path.endsWith("/zinc_ore.json") && !ModList.get().isLoaded("create"))continue;
            var value=entry.getValue().deepCopy();
            if(path.contains("/dimension_type/")) {
                var object=value.getAsJsonObject();object.addProperty("min_y",min);object.addProperty("height",max-min);object.addProperty("logical_height",max-min);
            }
            if(path.contains("/noise_settings/")) {
                var noise=value.getAsJsonObject().getAsJsonObject("noise");noise.addProperty("min_y",min);noise.addProperty("height",max-min);
            }
            if(path.contains("/configured_carver/") && !path.endsWith("cave_extra_underground.json")) {
                var options=settings.getAsJsonObject(path.endsWith("canyon.json")?"canyons":"caves");
                var config=value.getAsJsonObject().getAsJsonObject("config");config.addProperty("probability",Math.clamp(options.get("probability").getAsFloat(),0F,1F));
                var y=config.getAsJsonObject("y");y.getAsJsonObject("min_inclusive").addProperty("absolute",options.get("minimum_origin").getAsInt());
                y.getAsJsonObject("max_inclusive").addProperty("absolute",options.get("maximum_origin").getAsInt());y.addProperty("inner",options.get("inner").getAsInt());
                config.getAsJsonObject("lava_level").addProperty("absolute",options.get("lava_level").getAsInt());
            }
            if(path.contains("/placed_feature/")) {
                String name=path.substring(path.lastIndexOf('/')+1,path.length()-5);
                var ore=settings.getAsJsonObject("ores").getAsJsonObject("minecraft:"+name);
                if(ore!=null) {
                    var modifiers=value.getAsJsonObject().getAsJsonArray("placement");modifiers.get(0).getAsJsonObject().add("count",ore.get("count"));
                    modifiers.get(2).getAsJsonObject().add("height",ore.get("height"));
                    var feature=value.getAsJsonObject().get("feature").getAsString();
                    String configPath="data/ghcs_classic/worldgen/configured_feature/"+feature.split(":",2)[1]+".json";
                    if(templates.has(configPath)) {
                        var configured=templates.getAsJsonObject(configPath).deepCopy();
                        if(configured.getAsJsonObject("config").has("size"))configured.getAsJsonObject("config").addProperty("size",Math.clamp(ore.get("size").getAsInt(),0,64));
                        // Separate configured features let regular and badlands gold have independent sizes.
                        files.put("data/ghcs_classic/worldgen/configured_feature/"+name+".json",configured);
                        value.getAsJsonObject().addProperty("feature",ID+":"+name);
                    }
                }
            }
            files.putIfAbsent(path,value);
        }
        files.put("pack.mcmeta",ExternalConfig.metadata("GHCS Classic World preset; vanilla generation remains unchanged"));
        ExternalPack.setRoot(ExternalConfig.generated(files));
    }
}
