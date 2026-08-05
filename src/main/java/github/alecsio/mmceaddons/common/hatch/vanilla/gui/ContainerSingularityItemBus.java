package github.alecsio.mmceaddons.common.hatch.vanilla.gui;

import github.alecsio.mmceaddons.util.SingularitySlotItemHandler;
import hellfirepvp.modularmachinery.common.container.ContainerItemBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerSingularityItemBus extends ContainerItemBus {

    private static final int PLAYER_SLOT_COUNT = 36;
    private static final int COUNT_PROPERTY_BASE = 0x4000;
    private final int machineSlotCount;
    private final boolean[] changedMachineSlots;
    private final int[] pendingCountLow;

    public ContainerSingularityItemBus(TileItemBus owner, EntityPlayer opening) {
        super(owner, opening);
        this.machineSlotCount = owner.getSize().getSlotCount();
        this.changedMachineSlots = new boolean[this.machineSlotCount];
        this.pendingCountLow = new int[this.machineSlotCount];
    }

    @Override
    public void detectAndSendChanges() {
        if (this.listeners.isEmpty()) {
            super.detectAndSendChanges();
            return;
        }
        for (int machineSlot = 0; machineSlot < this.machineSlotCount; machineSlot++) {
            int containerSlot = PLAYER_SLOT_COUNT + machineSlot;
            this.changedMachineSlots[machineSlot] = !ItemStack.areItemStacksEqual(this.inventoryItemStacks.get(containerSlot), this.inventorySlots.get(containerSlot).getStack());
        }
        super.detectAndSendChanges();
        for (int machineSlot = 0; machineSlot < this.machineSlotCount; machineSlot++) {
            if (!this.changedMachineSlots[machineSlot]) {
                continue;
            }
            this.changedMachineSlots[machineSlot] = false;
            ItemStack stack = this.inventorySlots.get(PLAYER_SLOT_COUNT + machineSlot).getStack();
            int count = stack.getCount();
            if (count <= Byte.MAX_VALUE) {
                continue;
            }
            int property = COUNT_PROPERTY_BASE + machineSlot * 2;
            for (IContainerListener listener : this.listeners) {
                listener.sendWindowProperty(this, property, count & 0xFFFF);
                listener.sendWindowProperty(this, property + 1, count >>> 16);
            }
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        int property = id - COUNT_PROPERTY_BASE;
        if (property < 0 || property >= this.machineSlotCount * 2) {
            super.updateProgressBar(id, data);
            return;
        }
        int machineSlot = property / 2;
        if ((property & 1) == 0) {
            this.pendingCountLow[machineSlot] = data & 0xFFFF;
            return;
        }
        int count = this.pendingCountLow[machineSlot] | (data & 0xFFFF) << 16;
        if (count <= Byte.MAX_VALUE) {
            return;
        }
        Slot slot = this.inventorySlots.get(PLAYER_SLOT_COUNT + machineSlot);
        if (!(slot instanceof SingularitySlotItemHandler)) {
            return;
        }
        ItemStack stack = slot.getStack();
        if (stack != ItemStack.EMPTY) {
            stack.setCount(count);
        }
    }

    int findRenderedMachineSlot(ItemStack stack, int x, int y) {
        for (int machineSlot = 0; machineSlot < this.machineSlotCount; machineSlot++) {
            Slot slot = this.inventorySlots.get(PLAYER_SLOT_COUNT + machineSlot);
            if (slot instanceof SingularitySlotItemHandler && slot.xPos == x && slot.yPos == y && slot.getStack() == stack) {
                return machineSlot;
            }
        }
        return -1;
    }

    @Nonnull
    @Override
    public ItemStack slotClick(int slotId, int dragType, @Nonnull ClickType clickTypeIn, EntityPlayer player) {
        InventoryPlayer inventoryplayer = player.inventory;
        if (slotId > 0 && this.inventorySlots.get(slotId) instanceof SingularitySlotItemHandler singularitySlot && clickTypeIn == ClickType.PICKUP && (dragType == 0 || dragType == 1)) {
            ItemStack heldStack = inventoryplayer.getItemStack();
            ItemStack inventoryStack = singularitySlot.getStack();


            if (!heldStack.isEmpty() && !inventoryStack.isEmpty() && heldStack.getItem() == inventoryStack.getItem() && heldStack.getMetadata() == inventoryStack.getMetadata() && ItemStack.areItemStackTagsEqual(heldStack, inventoryStack)) {
                int amountToDrop = dragType == 0 ? heldStack.getCount() : 1;

                if (amountToDrop > singularitySlot.getItemStackLimit(heldStack) - inventoryStack.getCount()) {
                    amountToDrop = singularitySlot.getItemStackLimit(heldStack) - inventoryStack.getCount();
                }

                ItemStack toReturn = heldStack.copy();
                heldStack.shrink(amountToDrop);
                inventoryStack.grow(amountToDrop);
                return toReturn;
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }
}