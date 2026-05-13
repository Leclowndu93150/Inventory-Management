package com.leclowndu93150.inventorymanagement.compat;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

public class ContainerAnalyzer {
    private static final Map<AbstractContainerMenu, AnalysisResult> CACHE = new WeakHashMap<>();

    public static AnalysisResult analyze(AbstractContainerMenu menu) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(menu, ContainerAnalyzer::analyzeUncached);
        }
    }

    public static void clearCache(AbstractContainerMenu menu) {
        synchronized (CACHE) {
            CACHE.remove(menu);
        }
    }

    private static AnalysisResult analyzeUncached(AbstractContainerMenu menu) {
        IdentityHashMap<Object, GroupBuilder> buildersByHandle = new IdentityHashMap<>();
        List<GroupBuilder> builders = new ArrayList<>();

        for (Slot slot : menu.slots) {
            if (!isValidSlot(slot)) {
                continue;
            }

            Object handle = getHandle(slot);
            if (handle == null) {
                continue;
            }

            GroupBuilder builder = buildersByHandle.get(handle);
            if (builder == null) {
                builder = GroupBuilder.create(handle);
                buildersByHandle.put(handle, builder);
                builders.add(builder);
            }
            builder.add(slot);
        }

        LinkedHashMap<Object, SlotGroup> groups = new LinkedHashMap<>();
        for (GroupBuilder builder : builders) {
            SlotGroup group = builder.build(menu);
            if (!group.menuSlots().isEmpty()) {
                groups.put(group.handle(), group);
            }
        }

        return new AnalysisResult(menu, groups);
    }

    private static Object getHandle(Slot slot) {
        if (slot instanceof SlotItemHandler slotItemHandler) {
            return slotItemHandler.getItemHandler();
        }

        return slot.container;
    }

    private static boolean isValidSlot(Slot slot) {
        if (slot == null || !slot.isActive()) {
            return false;
        }

        if (slot instanceof SlotItemHandler slotItemHandler) {
            try {
                int slotIndex = slot.getContainerSlot();
                return slotIndex >= 0 && slotIndex < slotItemHandler.getItemHandler().getSlots();
            } catch (Exception e) {
                return false;
            }
        }

        if (slot.container == null) {
            return false;
        }

        try {
            int slotIndex = slot.getContainerSlot();
            int size = slot.container.getContainerSize();
            return slotIndex >= 0 && slotIndex < size;
        } catch (Exception e) {
            return false;
        }
    }

    public static SlotGroup getPlayerInventoryGroup(AbstractContainerMenu menu) {
        return analyze(menu).playerGroup().orElse(null);
    }

    public static SlotGroup getContainerInventoryGroup(AbstractContainerMenu menu) {
        return analyze(menu).containerGroup().orElse(null);
    }

    public static List<SlotGroup> getAllStorageGroups(AbstractContainerMenu menu) {
        return analyze(menu).storageGroups();
    }

    public static SlotGroup getGroup(AbstractContainerMenu menu, Object handle) {
        return analyze(menu).group(handle).orElse(null);
    }

    public static class AnalysisResult {
        private final AbstractContainerMenu menu;
        private final LinkedHashMap<Object, SlotGroup> groups;

        private AnalysisResult(AbstractContainerMenu menu, LinkedHashMap<Object, SlotGroup> groups) {
            this.menu = menu;
            this.groups = groups;
        }

        public Collection<SlotGroup> groups() {
            return Collections.unmodifiableCollection(groups.values());
        }

        public Optional<SlotGroup> group(Object handle) {
            return Optional.ofNullable(groups.get(handle));
        }

        public Optional<SlotGroup> playerGroup() {
            return groups.values().stream()
                    .filter(SlotGroup::isPlayerInventory)
                    .filter(group -> group.menuSlots().stream()
                            .anyMatch(slot -> slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36))
                    .findFirst();
        }

        public Optional<SlotGroup> containerGroup() {
            return storageGroups().stream()
                    .max(Comparator.comparingInt(SlotGroup::slotCount));
        }

        public List<SlotGroup> storageGroups() {
            return groups.values().stream()
                    .filter(group -> !group.isPlayerInventory())
                    .filter(group -> !group.isCraftingInventory())
                    .filter(group -> !group.isResultInventory())
                    .filter(group -> group.slotCount() >= 3)
                    .sorted(Comparator.comparingInt(SlotGroup::slotCount).reversed())
                    .collect(Collectors.toList());
        }

        public AbstractContainerMenu menu() {
            return menu;
        }
    }

    public interface SlotGroup {
        Object handle();

        AbstractContainerMenu menu();

        List<Slot> menuSlots();

        default int slotCount() {
            return menuSlots().size();
        }

        int inventorySize();

        ItemStack get(int slot);

        boolean set(int slot, ItemStack stack);

        ItemStack insert(int slot, ItemStack stack, boolean simulate);

        ItemStack extract(int slot, int amount, boolean simulate);

        boolean mayPlace(int slot, ItemStack stack);

        boolean mayPickup(int slot, Player player);

        int getSlotLimit(int slot, ItemStack stack);

        boolean isItemHandler();

        boolean isModifiable();

        default boolean isPlayerInventory() {
            return handle() instanceof Inventory;
        }

        default boolean isCraftingInventory() {
            return handle() instanceof CraftingContainer;
        }

        default boolean isResultInventory() {
            return handle() instanceof ResultContainer;
        }

        default boolean canSort() {
            return isModifiable();
        }

        default Container asContainer() {
            return handle() instanceof Container container ? container : null;
        }

        default List<Slot> bulkSlots(boolean includeHotbar) {
            return menuSlots().stream()
                    .filter(slot -> isBulkSlot(slot, includeHotbar))
                    .toList();
        }

        private boolean isBulkSlot(Slot slot, boolean includeHotbar) {
            if (isPlayerInventory()) {
                int slotIndex = slot.getContainerSlot();
                return includeHotbar ? slotIndex >= 0 && slotIndex < 36 : slotIndex >= 9 && slotIndex < 36;
            }

            return !(menu() instanceof HorseInventoryMenu) || slot.getContainerSlot() >= 2;
        }
    }

    public static class ContainerSlotGroup implements SlotGroup {
        private final AbstractContainerMenu menu;
        private final Container container;
        private final List<Slot> slots;
        private final Map<Integer, Slot> slotsByContainerIndex;

        private ContainerSlotGroup(AbstractContainerMenu menu, Container container, List<Slot> slots) {
            this.menu = menu;
            this.container = container;
            this.slots = List.copyOf(slots);
            this.slotsByContainerIndex = new LinkedHashMap<>();
            for (Slot slot : slots) {
                this.slotsByContainerIndex.putIfAbsent(slot.getContainerSlot(), slot);
            }
        }

        @Override
        public Object handle() {
            return container;
        }

        @Override
        public AbstractContainerMenu menu() {
            return menu;
        }

        @Override
        public List<Slot> menuSlots() {
            return slots;
        }

        @Override
        public int inventorySize() {
            return container.getContainerSize();
        }

        @Override
        public ItemStack get(int slot) {
            return isInBounds(slot) ? container.getItem(slot) : ItemStack.EMPTY;
        }

        @Override
        public boolean set(int slot, ItemStack stack) {
            if (!isInBounds(slot)) {
                return false;
            }

            Slot menuSlot = slotsByContainerIndex.get(slot);
            if (menuSlot != null) {
                menuSlot.set(stack);
            } else {
                container.setItem(slot, stack);
                container.setChanged();
            }
            return true;
        }

        @Override
        public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isInBounds(slot) || !mayPlace(slot, stack)) {
                return stack;
            }

            ItemStack existing = get(slot);
            int limit = getSlotLimit(slot, stack);
            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(existing, stack) || existing.getCount() >= limit) {
                    return stack;
                }

                int inserted = Math.min(stack.getCount(), limit - existing.getCount());
                if (inserted <= 0) {
                    return stack;
                }

                ItemStack remainder = stack.copy();
                remainder.shrink(inserted);
                if (!simulate) {
                    ItemStack updated = existing.copy();
                    updated.grow(inserted);
                    set(slot, updated);
                }
                return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
            }

            int inserted = Math.min(stack.getCount(), limit);
            ItemStack remainder = stack.copy();
            ItemStack insertedStack = remainder.split(inserted);
            if (!simulate) {
                set(slot, insertedStack);
            }
            return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            if (amount <= 0 || !isInBounds(slot)) {
                return ItemStack.EMPTY;
            }

            ItemStack existing = get(slot);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int extracted = Math.min(amount, existing.getCount());
            ItemStack copy = existing.copy();
            copy.setCount(extracted);
            if (!simulate) {
                Slot menuSlot = slotsByContainerIndex.get(slot);
                if (menuSlot != null) {
                    menuSlot.remove(extracted);
                } else {
                    container.removeItem(slot, extracted);
                    container.setChanged();
                }
            }
            return copy;
        }

        @Override
        public boolean mayPlace(int slot, ItemStack stack) {
            if (!isInBounds(slot) || stack.isEmpty()) {
                return false;
            }

            Slot menuSlot = slotsByContainerIndex.get(slot);
            return container.canPlaceItem(slot, stack) && (menuSlot == null || menuSlot.mayPlace(stack));
        }

        @Override
        public boolean mayPickup(int slot, Player player) {
            if (!isInBounds(slot)) {
                return false;
            }

            Slot menuSlot = slotsByContainerIndex.get(slot);
            return menuSlot == null || menuSlot.mayPickup(player);
        }

        @Override
        public int getSlotLimit(int slot, ItemStack stack) {
            Slot menuSlot = slotsByContainerIndex.get(slot);
            int limit = menuSlot != null ? menuSlot.getMaxStackSize(stack) : container.getMaxStackSize();
            return Math.min(limit, stack.getMaxStackSize());
        }

        @Override
        public boolean isItemHandler() {
            return false;
        }

        @Override
        public boolean isModifiable() {
            return true;
        }

        private boolean isInBounds(int slot) {
            return slot >= 0 && slot < container.getContainerSize();
        }
    }

    public static class ItemHandlerSlotGroup implements SlotGroup {
        private final AbstractContainerMenu menu;
        private final IItemHandler handler;
        private final List<Slot> slots;
        private final Map<Integer, Slot> slotsByHandlerIndex;

        private ItemHandlerSlotGroup(AbstractContainerMenu menu, IItemHandler handler, List<Slot> slots) {
            this.menu = menu;
            this.handler = handler;
            this.slots = List.copyOf(slots);
            this.slotsByHandlerIndex = new LinkedHashMap<>();
            for (Slot slot : slots) {
                this.slotsByHandlerIndex.putIfAbsent(slot.getContainerSlot(), slot);
            }
        }

        @Override
        public Object handle() {
            return handler;
        }

        @Override
        public AbstractContainerMenu menu() {
            return menu;
        }

        @Override
        public List<Slot> menuSlots() {
            return slots;
        }

        @Override
        public int inventorySize() {
            return handler.getSlots();
        }

        @Override
        public ItemStack get(int slot) {
            return isInBounds(slot) ? handler.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public boolean set(int slot, ItemStack stack) {
            if (!isInBounds(slot) || !(handler instanceof IItemHandlerModifiable modifiable)) {
                return false;
            }

            try {
                modifiable.setStackInSlot(slot, stack);
                Slot menuSlot = slotsByHandlerIndex.get(slot);
                if (menuSlot != null) {
                    menuSlot.setChanged();
                }
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isInBounds(slot)) {
                return stack;
            }

            try {
                return handler.insertItem(slot, stack, simulate);
            } catch (RuntimeException e) {
                return stack;
            }
        }

        @Override
        public ItemStack extract(int slot, int amount, boolean simulate) {
            if (amount <= 0 || !isInBounds(slot)) {
                return ItemStack.EMPTY;
            }

            try {
                return handler.extractItem(slot, amount, simulate);
            } catch (RuntimeException e) {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public boolean mayPlace(int slot, ItemStack stack) {
            if (!isInBounds(slot) || stack.isEmpty()) {
                return false;
            }

            Slot menuSlot = slotsByHandlerIndex.get(slot);
            try {
                return handler.isItemValid(slot, stack) && (menuSlot == null || menuSlot.mayPlace(stack));
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public boolean mayPickup(int slot, Player player) {
            if (!isInBounds(slot)) {
                return false;
            }

            Slot menuSlot = slotsByHandlerIndex.get(slot);
            return (menuSlot == null || menuSlot.mayPickup(player)) && !extract(slot, 1, true).isEmpty();
        }

        @Override
        public int getSlotLimit(int slot, ItemStack stack) {
            if (!isInBounds(slot)) {
                return 0;
            }

            try {
                return Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
            } catch (RuntimeException e) {
                return 0;
            }
        }

        @Override
        public boolean isItemHandler() {
            return true;
        }

        @Override
        public boolean isModifiable() {
            return handler instanceof IItemHandlerModifiable;
        }

        private boolean isInBounds(int slot) {
            return slot >= 0 && slot < handler.getSlots();
        }
    }

    public static class ContainerInfo {
        private final SlotGroup group;
        private final Set<String> slotTypes;

        public ContainerInfo(SlotGroup group) {
            this.group = group;
            this.slotTypes = group.menuSlots().stream()
                    .map(slot -> slot.getClass().getSimpleName())
                    .collect(Collectors.toSet());
        }

        public Object getHandle() {
            return group.handle();
        }

        public Container getContainer() {
            return group.asContainer();
        }

        public SlotGroup getGroup() {
            return group;
        }

        public List<Slot> getSlots() {
            return new ArrayList<>(group.menuSlots());
        }

        public int getSlotCount() {
            return group.slotCount();
        }

        public boolean isHomogeneous() {
            return slotTypes.size() <= 1;
        }

        public boolean isItemHandler() {
            return group.isItemHandler();
        }

        public Set<String> getSlotTypes() {
            return Set.copyOf(slotTypes);
        }

        public String getContainerType() {
            if (group.isPlayerInventory()) {
                return "Player";
            }
            if (group.isCraftingInventory()) {
                return "Crafting";
            }
            if (group.isResultInventory()) {
                return "Result";
            }
            if (group.isItemHandler()) {
                return "ItemHandler";
            }
            return "Container";
        }
    }

    public static Map<Object, ContainerInfo> analyzeMenu(AbstractContainerMenu menu) {
        Map<Object, ContainerInfo> result = new LinkedHashMap<>();
        for (SlotGroup group : analyze(menu).groups()) {
            result.put(group.handle(), new ContainerInfo(group));
        }
        return result;
    }

    public static ContainerInfo getPlayerInventoryInfo(AbstractContainerMenu menu) {
        SlotGroup group = getPlayerInventoryGroup(menu);
        return group != null ? new ContainerInfo(group) : null;
    }

    public static ContainerInfo getContainerInventoryInfo(AbstractContainerMenu menu) {
        SlotGroup group = getContainerInventoryGroup(menu);
        return group != null ? new ContainerInfo(group) : null;
    }

    public static List<ContainerInfo> getAllStorageContainers(AbstractContainerMenu menu) {
        return getAllStorageGroups(menu).stream()
                .filter(group -> group.slotCount() >= 9)
                .map(ContainerInfo::new)
                .collect(Collectors.toList());
    }

    private static class GroupBuilder {
        private final Object handle;
        private final Container container;
        private final IItemHandler itemHandler;
        private final List<Slot> slots = new ArrayList<>();

        private GroupBuilder(Object handle, Container container, IItemHandler itemHandler) {
            this.handle = handle;
            this.container = container;
            this.itemHandler = itemHandler;
        }

        private static GroupBuilder create(Object handle) {
            if (handle instanceof IItemHandler itemHandler) {
                return new GroupBuilder(handle, null, itemHandler);
            }
            return new GroupBuilder(handle, (Container) handle, null);
        }

        private void add(Slot slot) {
            slots.add(slot);
        }

        private SlotGroup build(AbstractContainerMenu menu) {
            slots.sort(Comparator.comparingInt(slot -> slot.index));
            if (itemHandler != null) {
                return new ItemHandlerSlotGroup(menu, itemHandler, slots);
            }
            return new ContainerSlotGroup(menu, container, slots);
        }
    }
}
