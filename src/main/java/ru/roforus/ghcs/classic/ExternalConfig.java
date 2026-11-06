package ru.roforus.ghcs.classic;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLPaths;

/** User-owned JSON files are provisioned once; generated files are disposable. */
public final class ExternalConfig {
    public static final Path ROOT=FMLPaths.CONFIGDIR.get().resolve("ghcs-classic-worldgen");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static JsonElement bundled(String name) {
        var input=ExternalConfig.class.getResourceAsStream("/defaults/"+name);
        if(input==null)throw new IllegalStateException("Missing default: "+name);
        try(var reader=new InputStreamReader(input,StandardCharsets.UTF_8)) {return JsonParser.parseReader(reader);}
        catch(IOException e) {throw new IllegalStateException(e);}
    }
    public static synchronized JsonElement read(String name) {
        var fallback=bundled(name);var file=ROOT.resolve(name);
        try {
            Files.createDirectories(file.getParent());
            if(!Files.exists(file))Files.writeString(file,GSON.toJson(fallback)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)) {
                var value=JsonParser.parseReader(reader);
                if(fallback.isJsonObject()!=value.isJsonObject() || fallback.isJsonArray()!=value.isJsonArray())throw new IllegalArgumentException("Wrong JSON root type");
                if(value.isJsonObject())for(var entry:fallback.getAsJsonObject().entrySet()) {
                    if(!value.getAsJsonObject().has(entry.getKey()))value.getAsJsonObject().add(entry.getKey(),entry.getValue().deepCopy());
                }
                return value;
            }
        }catch(Exception e) {
            warn("Не удалось прочитать "+file+"; на этот запуск применены стандартные настройки. "+e.getMessage());return fallback.deepCopy();
        }
    }
    public static void warn(String text) {
        LogUtils.getLogger().warn("[ghcs-classic-worldgen] {}",text);
        ModLoader.addLoadingIssue(ModLoadingIssue.warning(text.replace('{','(').replace('}',')')));
    }
    public static void write(Path file,JsonElement json) {
        try {Files.createDirectories(file.getParent());Files.writeString(file,GSON.toJson(json)+"\n",StandardCharsets.UTF_8);}
        catch(IOException e) {throw new IllegalStateException("Cannot write generated data: "+file,e);}
    }
    public static Path generated(Map<String,JsonElement> resources) {
        Path output=ROOT.resolve("generated").toAbsolutePath().normalize();
        Path manifest=output.resolve("generated-files.json");
        try {
            if(Files.exists(manifest))for(var entry:JsonParser.parseString(Files.readString(manifest)).getAsJsonArray()) {
                var file=output.resolve(entry.getAsString()).normalize();
                if(!file.startsWith(output))throw new IllegalArgumentException("Unsafe generated path");
                Files.deleteIfExists(file);
            }
        }catch(Exception e) {warn("Could not clean generated cache: "+e.getMessage());}
        var names=new JsonArray();
        resources.forEach((name,value)-> {var file=output.resolve(name).normalize();if(!file.startsWith(output))throw new IllegalArgumentException(name);write(file,value);names.add(name);});
        write(manifest,names);return output;
    }
    public static JsonObject metadata(String description) {
        var root=new JsonObject();var pack=new JsonObject();pack.addProperty("pack_format",48);pack.addProperty("description",description);root.add("pack",pack);return root;
    }
    private ExternalConfig() {}
}
