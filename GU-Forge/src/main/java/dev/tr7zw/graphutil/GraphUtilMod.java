package dev.tr7zw.graphutil;

import net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("graphutil")
public class GraphUtilMod extends GraphUtilModBase {

    //Forge only
    private boolean onServer = false;
    
    public GraphUtilMod() {
        try {
            Class clientClass = net.minecraft.client.Minecraft.class;
        }catch(Throwable ex) {
            System.out.println("GraphUtil Mod installed on a Server. Going to sleep.");
            onServer = true;
            return;
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiFactory.class,
                () -> new ConfigGuiFactory((mc, screen) -> {
                    return createConfigScreen(screen);
                }));
    }

    private void setup(final FMLCommonSetupEvent event) {
        if(onServer)return;
        onInitialize();
    }


    @Override
    public void initModloader() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString(),
                        (remote, isServer) -> true));
    }

}
