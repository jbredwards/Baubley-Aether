/*
 * Copyright (c) 2023. jbredwards
 * All rights reserved.
 */

package git.jbredwards.baubleya;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.cap.BaublesCapabilities;
import baubles.client.gui.GuiPlayerExpanded;
import baubles.common.network.PacketHandler;
import baubles.common.network.PacketOpenBaublesInventory;
import com.gildedgames.the_aether.client.AetherClientEvents;
import com.gildedgames.the_aether.client.AetherKeybinds;
import com.gildedgames.the_aether.client.gui.button.GuiAccessoryButton;
import com.gildedgames.the_aether.items.ItemsAether;
import com.gildedgames.the_aether.items.accessories.ItemAccessory;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author jbred
 *
 */
@Mod.EventBusSubscriber
@Mod(modid = "baubleya", name = "Baubley Aether", version = "1.0.0", dependencies = "required-after:baubles;required-after:aether_legacy")
public final class BaubleyAether
{
    // =============
    // COMMON EVENTS
    // =============

    @Nonnull
    static final ResourceLocation CAPABILITY_ID = new ResourceLocation("baubles", "bauble_cap");
    public static boolean playEquipSound = true; // public so modpack devs can use GroovyScript to disable, this is too niche for a config option imo

    @SubscribeEvent(priority = EventPriority.LOW)
    static void createAetherBaublesCapabilities(@Nonnull final AttachCapabilitiesEvent<ItemStack> event) {
        if(event.getObject().getItem() instanceof ItemAccessory && !event.getObject().hasCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null)) {
            event.addCapability(CAPABILITY_ID, new ICapabilityProvider() {
                @Override
                public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
                    return capability == BaublesCapabilities.CAPABILITY_ITEM_BAUBLE;
                }

                @Nullable
                @Override
                public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
                    return !hasCapability(capability, facing) ? null : BaublesCapabilities.CAPABILITY_ITEM_BAUBLE.cast(new IBauble() {
                        @Nonnull
                        @Override
                        public BaubleType getBaubleType(@Nonnull final ItemStack stack) {
                            switch(((ItemAccessory)stack.getItem()).getType()) {
                                case RING: return BaubleType.RING;
                                case PENDANT: return BaubleType.AMULET;
                                case CAPE: return BaubleType.BODY;
                                case SHIELD: return BaubleType.HEAD;
                                case GLOVE: return BaubleType.BELT;
                                default: return BaubleType.CHARM;
                            }
                        }

                        @Override
                        public void onEquipped(@Nonnull final ItemStack stack, @Nonnull final EntityLivingBase player) {
                            if(playEquipSound) {
                                @Nullable final SoundEvent equipSound = ((ItemAccessory)stack.getItem()).getEquipSound();
                                if(equipSound != null) player.playSound(equipSound, 1, 1);
                            }
                        }
                    });
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void updateLegacyMappings(@Nonnull final RegistryEvent.MissingMappings<Item> event) {
        @Nonnull final List<RegistryEvent.MissingMappings.Mapping<Item>> mappings = event.getAllMappings().stream()
                .filter(mapping -> mapping.key.getNamespace().equals("aether_baubles"))
                .collect(Collectors.toList());

        if(!mappings.isEmpty()) {
            @Nonnull final Map<String, Item> lookup = new HashMap<>(ImmutableMap.<String, Item>builder()
                    .put("agility_cape", ItemsAether.agility_cape)
                    .put("blue_cape", ItemsAether.blue_cape)
                    .put("chain_gloves", ItemsAether.chain_gloves)
                    .put("diamond_gloves", ItemsAether.diamond_gloves)
                    .put("golden_feather", ItemsAether.golden_feather)
                    .put("golden_gloves", ItemsAether.golden_gloves)
                    .put("golden_pendant", ItemsAether.golden_pendant)
                    .put("golden_ring", ItemsAether.golden_ring)
                    .put("gravitite_gloves", ItemsAether.gravitite_gloves)
                    .put("iron_bubble", ItemsAether.iron_bubble)
                    .put("ice_pendant", ItemsAether.ice_pendant)
                    .put("ice_ring", ItemsAether.ice_ring)
                    .put("invisibility_cape", ItemsAether.invisibility_cape)
                    .put("iron_gloves", ItemsAether.iron_gloves)
                    .put("iron_pendant", ItemsAether.iron_pendant)
                    .put("iron_ring", ItemsAether.iron_ring)
                    .put("leather_gloves", ItemsAether.leather_gloves)
                    .put("neptune_gloves", ItemsAether.neptune_gloves)
                    .put("obsidian_gloves", ItemsAether.obsidian_gloves)
                    .put("phoenix_gloves", ItemsAether.phoenix_gloves)
                    .put("red_cape", ItemsAether.red_cape)
                    .put("regeneration_stone", ItemsAether.regeneration_stone)
                    .put("repulsion_shield", ItemsAether.repulsion_shield)
                    .put("swet_cape", ItemsAether.swet_cape)
                    .put("valkyrie_cape", ItemsAether.valkyrie_cape)
                    .put("valkyrie_gloves", ItemsAether.valkyrie_gloves)
                    .put("white_cape", ItemsAether.white_cape)
                    .put("yellow_cape", ItemsAether.yellow_cape)
                    .put("zanite_gloves", ItemsAether.zanite_gloves)
                    .put("zanite_pendant", ItemsAether.zanite_pendant)
                    .put("zanite_ring", ItemsAether.zanite_ring)
                    .build());

            mappings.forEach(mapping -> lookup.computeIfPresent(mapping.key.getPath(), (key, item) -> {
                mapping.remap(item);
                return null;
            }));
        }
    }

    // =============
    // CLIENT EVENTS
    // =============

    @SuppressWarnings("deprecation") // ReflectionHelper is fine to use
    @SideOnly(Side.CLIENT)
    @Mod.EventHandler
    static void preInitClient(@Nonnull final FMLPreInitializationEvent event) {
        @Nonnull final GuiAccessoryButton button = ReflectionHelper.getPrivateValue(AetherClientEvents.class, null, "ACCESSORY_BUTTON");
        button.enabled = false;
        button.visible = false;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void removeAetherAccessoryInventory(@Nonnull final GuiScreenEvent.InitGuiEvent.Post event) {
        if(event.getGui() instanceof GuiContainer) event.getButtonList().removeIf(button -> button instanceof GuiAccessoryButton);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void removeAetherAccessoryKeyBind(@Nonnull final InputEvent.KeyInputEvent event) {
        if(Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().currentScreen == null
        && AetherKeybinds.keyBindingAccessories.isKeyDown()) {
            AetherKeybinds.keyBindingAccessories.unpressKey(); // needed to prevent aether's code
            PacketHandler.INSTANCE.sendToServer(new PacketOpenBaublesInventory());
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void removeAetherAccessoryKeyBind(@Nonnull final GuiScreenEvent.KeyboardInputEvent event) {
        if(event.getGui() instanceof GuiPlayerExpanded
        && Keyboard.getEventKeyState() && Keyboard.getEventKey() == AetherKeybinds.keyBindingAccessories.getKeyCode()) {
            Minecraft.getMinecraft().player.closeScreen();
        }
    }
}
