package ru.roforus.ghcs.classic;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.neoforged.neoforge.event.AddPackFindersEvent;
public final class ExternalPack {
    private static Path root;
    public static void setRoot(Path value) {root=value;}
    public static void find(AddPackFindersEvent event) {
        if(event.getPackType()!=PackType.SERVER_DATA || root==null)return;
        var pack=Pack.readMetaAndCreate(new PackLocationInfo("mod/ghcs_classic:rules",Component.literal("GHCS-Classic-Worldgen"),PackSource.BUILT_IN,Optional.empty()),
            BuiltInPackSource.fromName(info->new PathPackResources(info,root)),PackType.SERVER_DATA,new PackSelectionConfig(true,Pack.Position.TOP,true));
        if(pack!=null)event.addRepositorySource(consumer->consumer.accept(pack));
    }
    private ExternalPack() {}
}
