package dev.tr7zw.graphutil;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class GraphUtilModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            return GraphUtilModBase.instance.createConfigScreen(parent);
        };
    }  
    
}
