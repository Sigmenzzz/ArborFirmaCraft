package com.therighthon.afc;

import com.mojang.logging.LogUtils;
import com.therighthon.afc.common.AFCCreativeModeTabs;
import com.therighthon.afc.common.AFCFeatures;
import com.therighthon.afc.common.blockentities.AFCBlockEntities;
import com.therighthon.afc.common.blocks.AFCBlocks;
import com.therighthon.afc.common.blocks.AFCWood;
import com.therighthon.afc.common.blocks.FLCompatBlocks;
import com.therighthon.afc.common.commands.AFCCommands;
import com.therighthon.afc.common.entities.AFCEntities;
import com.therighthon.afc.common.fluids.AFCFluids;
import com.therighthon.afc.common.items.AFCItems;
import com.therighthon.afc.common.recipe.AFCRecipeTypes;
import com.therighthon.afc.common.recipe.AFCRecipes;
import com.therighthon.afc.event.ModEvents;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import net.minecraftforge.fml.loading.FMLEnvironment;

import net.dries007.tfc.common.recipes.TFCRecipeSerializers;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AFC.MOD_ID)
public class AFC
{
    public static final String MOD_ID = "afc";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public AFC()
    {
        // Register the setup method for modloading
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::setup);
        ModEvents.init();

        AFCBlocks.BLOCKS.register(eventBus);
        AFCItems.ITEMS.register(eventBus);
        AFCFluids.FLUIDS.register(eventBus);
        AFCCommands.ARGUMENT_TYPES.register(eventBus);
        AFCEntities.ENTITIES.register(eventBus);
        AFCFeatures.FEATURES.register(eventBus);
        AFCBlockEntities.BLOCK_ENTITIES.register(eventBus);
        AFCRecipeTypes.RECIPE_TYPES.register(eventBus);
        TFCRecipeSerializers.RECIPE_SERIALIZERS.register(eventBus);
        AFCRecipes.register(eventBus);
        AFCCreativeModeTabs.CREATIVE_TABS.register(eventBus);

        if (ModList.get().isLoaded("firmalife"))
        {
            FLCompatBlocks.BLOCKS.register(eventBus);
            ModEvents.initFLCompat();
        }
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            eventBus.addListener(com.therighthon.afc.event.ModEventClientBusEvents::clientSetup);
            eventBus.addListener(com.therighthon.afc.event.ModEventClientBusEvents::registerClientReloadListeners);
            eventBus.addListener(com.therighthon.afc.event.ModEventClientBusEvents::onEntityRenderers);
            eventBus.addListener(com.therighthon.afc.event.ModEventClientBusEvents::registerColorHandlerBlocks);
            eventBus.addListener(com.therighthon.afc.event.ModEventClientBusEvents::registerColorHandlerItems);
            eventBus.addListener(com.therighthon.afc.event.ModEventClientBusEvents::onLayers);

            if (ModList.get().isLoaded("firmalife"))
            {
                eventBus.addListener(com.therighthon.afc.event.ModEventClientBusEvents::clientFLCompatSetup);
            }
        }

        final IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(AFC::registerCommands);

        // Register ourselves for server and other game events we are interested in
        forgeBus.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("AFC COMMON SETUP");
        event.enqueueWork(AFCWood::registerBlockSetTypes);
    }

    public static ResourceLocation treeIdentifier(String path)
    {
        return new ResourceLocation("tfc", path);
    }

    public static void registerCommands(RegisterCommandsEvent event)
    {
        LOGGER.debug("Registering AFC Commands");
        AFCCommands.registerCommands(event.getDispatcher(), event.getBuildContext());
    }

}
