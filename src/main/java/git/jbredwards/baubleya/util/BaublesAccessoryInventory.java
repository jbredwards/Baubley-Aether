/*
 * Copyright (c) 2023. jbredwards
 * All rights reserved.
 */

package git.jbredwards.baubleya.util;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.api.player.util.IAccessoryInventory;
import com.gildedgames.the_aether.containers.inventory.InventoryAccessories;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractList;
import java.util.Objects;

/**
 * An implementation of {@link IAccessoryInventory} that wraps around a baubles inventory.
 * @author jbred
 *
 */
@SuppressWarnings("unused") // called via asm
public class BaublesAccessoryInventory extends InventoryAccessories
{
    @Nullable
    protected IBaublesItemHandler handler;
    public BaublesAccessoryInventory(@Nonnull final EntityPlayer playerIn) {
        super(Objects.requireNonNull(playerIn, "Aether fired a sync packet, please report this to Baubley Aether's issues page"));
        // wraps this inventory around the baubles inventory handler
        stacks = new NonNullList<>(new AbstractList<ItemStack>() {
            @Nonnull
            @Override
            public ItemStack get(final int index) { return getBaublesHandler().getStackInSlot(index); }

            @Nonnull
            @Override
            public ItemStack set(final int index, @Nonnull final ItemStack stack) {
                @Nonnull final ItemStack prev = get(index);
                getBaublesHandler().setStackInSlot(index, stack);
                return prev;
            }

            @Override
            public int size() { return getBaublesHandler().getSlots(); }
        }, ItemStack.EMPTY);
    }

    @Nonnull
    public IBaublesItemHandler getBaublesHandler() {
        return handler == null ? handler = BaublesApi.getBaublesHandler(player) : handler;
    }

    @Nonnull
    public ItemStack findStackForType(@Nonnull final BaubleType type) {
        for(final int index : type.getValidSlots()) if(!stacks.get(index).isEmpty()) return stacks.get(index);
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack decrStackSize(final int slotID, final int decreaseSize) {
        return getBaublesHandler().extractItem(slotID, decreaseSize, false);
    }

    @Override
    public boolean isItemValidForSlot(final int slotID, @Nonnull final ItemStack stack) {
        return getBaublesHandler().isItemValidForSlot(slotID, stack, player);
    }

    @Override
    public void damageWornStack(final int damageAmount, @Nonnull final ItemStack stack) {
        if(!player.isCreative()) {
            for(int index = 0; index < getSizeInventory(); index++) {
                @Nonnull final ItemStack wornStack = getStackInSlot(index);
                if(stack.isItemEqualIgnoreDurability(wornStack)) {
                    final int prevDamage = wornStack.getItemDamage();
                    wornStack.damageItem(damageAmount, player);

                    if(wornStack.isEmpty()) setInventorySlotContents(index, ItemStack.EMPTY);
                    else if(prevDamage != wornStack.getItemDamage()) getBaublesHandler().setChanged(index, true);
                    return;
                }
            }
        }
    }

    @Override
    public boolean setAccessorySlot(@Nonnull final ItemStack stack) {
        if(!stack.isEmpty() && AetherAPI.getInstance().isAccessory(stack) && stack.hasCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null)) {
            for(final int index : Objects.requireNonNull(stack.getCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null)).getBaubleType(stack).getValidSlots()) {
                if(getStackInSlot(index).isEmpty()) {
                    setInventorySlotContents(index, stack);
                    return true;
                }
            }
        }

        return false;
    }

    // =====================================
    // NO-OP (handled internally by Baubles)
    // =====================================

    @Override
    public void markDirty() {}

    @Override
    public void writeToNBT(@Nonnull final NBTTagCompound nbt) {}

    @Override
    public void readFromNBT(@Nonnull final NBTTagCompound nbt) {
        // handle old aether inventory data, give back accessories that used to be in the aether accessories container
        @Nonnull final NBTTagList list = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        if(!list.isEmpty()) player.sendMessage(new TextComponentTranslation("baubleya.message.old_data_found"));
        for(int i = 0; i < list.tagCount(); i++) ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(list.getCompoundTagAt(i)));
    }

    @Override
    public void writeData(@Nonnull final ByteBuf buf) {}

    @Override
    public void readData(@Nonnull final ByteBuf buf) {}
}
