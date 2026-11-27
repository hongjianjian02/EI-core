package com.mysterious.mystic.compat.anvilguide.data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import org.slf4j.Logger;

/**
 * 当前同步注册表与浏览器物品样例的查询索引。
 * Holder 和组件变体都属于本次加载；退服、换世界和重载必须 reset 后重新 build。
 */
public final class AnvilEnchantData {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static Registry<Enchantment> registry;
    private static boolean built;
    private static boolean stale;
    private static int generation;
    private static List<Holder<Enchantment>> enchantments = List.of();
    private static List<ItemStack> targetStacks = List.of();
    private static List<ItemStack> enchantableStacks = List.of();
    private static List<ItemStack> materialCandidates = List.of();
    private static List<ItemStack> observedBooks = List.of();

    private static final Map<Holder<Enchantment>, TreeSet<Integer>> OBSERVED_LEVELS = new HashMap<>();
    private static final Map<StackKey, List<BookEntry>> HIGHEST_BOOKS = new HashMap<>();
    private static final Map<StackKey, List<BookEntry>> ALL_BOOKS = new HashMap<>();
    private static final Map<Holder<Enchantment>, List<ItemStack>> ITEMS_BY_ENCHANT = new HashMap<>();
    private static final Map<BookTargetKey, List<ItemStack>> ITEMS_FOR_BOOK = new HashMap<>();
    private static final Map<Holder<Enchantment>, List<Holder<Enchantment>>> CONFLICTS = new HashMap<>();
    private static final Map<Holder<Enchantment>, Map<EquipGroup, List<ItemStack>>> GROUPED_ITEMS = new HashMap<>();
    private static final Map<BookKey, BookEntry> ENTRIES = new HashMap<>();
    private static final Map<StackKey, List<ItemStack>> REPAIR_MATERIALS = new HashMap<>();
    private static final Map<StackKey, List<ItemStack>> REPAIR_TARGETS = new HashMap<>();
    private static final Map<Class<?>, RepairSource> REPAIR_SOURCES = new HashMap<>();
    private static final Map<Class<?>, Boolean> CUSTOM_SUPPORT = new HashMap<>();
    private static final Map<Item, List<Holder<Enchantment>>> SUPPORTED_BY_ITEM = new HashMap<>();
    private static List<RepairGroup> repairGroups = List.of();
    private static boolean repairIndexBuilt;

    private AnvilEnchantData() {
    }

    /** 由浏览器生命周期传入已同步的注册表和样例，不在主菜单猜测客户端世界。 */
    public static synchronized void build(RegistryAccess registries, List<ItemStack> browserStacks) {
        reset();
        registry = registries.registryOrThrow(Registries.ENCHANTMENT);
        enchantments = registry.holders().map(holder -> (Holder<Enchantment>) holder)
                .filter(EnchantmentCompat::isAvailable)
                .sorted(Comparator.comparing(AnvilEnchantData::enchantmentId))
                .toList();

        Map<StackKey, ItemStack> samples = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            try {
                addSample(samples, rebindEnchantments(item.getDefaultInstance()));
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("物品 {} 的默认组件含有当前注册表不存在的附魔，未添加该样例", itemId(item), exception);
            }
        }
        Map<StackKey, ItemStack> books = new LinkedHashMap<>();
        for (ItemStack stack : browserStacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack current;
            try {
                current = rebindEnchantments(stack);
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("浏览器物品 {} 的组件含有当前注册表不存在的附魔，未添加该样例", itemId(stack.getItem()), exception);
                continue;
            }
            addSample(samples, current);
            observeLevels(current.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
            ItemEnchantments stored = current.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            observeLevels(stored);
            if (isAnvilBook(current)) {
                addSample(books, current);
            }
        }
        observedBooks = List.copyOf(books.values());
        materialCandidates = samples.values().stream()
                .sorted(Comparator.comparing(stack -> itemId(stack.getItem())))
                .toList();
        targetStacks = materialCandidates.stream().filter(AnvilEnchantData::isEligible).toList();
        rebuildIndexes();
    }

    /** 仅用于本次加载中晚到的观测等级；没有注册表时返回空数据。 */
    public static synchronized void build() {
        if (registry != null && (!built || stale)) {
            rebuildIndexes();
        }
    }

    /** 清除所有本次连接的 Holder、书等级和组件索引，避免带到其他服务器。 */
    public static synchronized void reset() {
        clearDerivedData();
        registry = null;
        built = false;
        stale = false;
        enchantments = List.of();
        targetStacks = List.of();
        enchantableStacks = List.of();
        materialCandidates = List.of();
        observedBooks = List.of();
        OBSERVED_LEVELS.clear();
        generation++;
    }

    public static void clear() {
        reset();
    }

    private static void addSample(Map<StackKey, ItemStack> samples, ItemStack stack) {
        if (stack != null && !stack.isEmpty() && !stack.is(Items.AIR)) {
            StackKey key = new StackKey(stack);
            samples.putIfAbsent(key, key.stack.copy());
        }
    }

    private static boolean isEligible(ItemStack stack) {
        return !stack.is(Items.BOOK) && !stack.is(Items.WRITABLE_BOOK)
                && !(stack.getItem() instanceof EnchantedBookItem)
                && !stack.has(DataComponents.STORED_ENCHANTMENTS);
    }

    /** 与 AnvilMenu 的右侧书标记及实际附魔读取保持一致。 */
    public static boolean isAnvilBook(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.STORED_ENCHANTMENTS)
                && !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
    }

    private static void observeLevels(ItemEnchantments levels) {
        for (var entry : levels.entrySet()) {
            observeBookLevel(entry.getKey(), entry.getIntValue());
        }
    }

    public static void observeBookLevel(Holder<Enchantment> enchantment, int level) {
        Holder<Enchantment> current = currentHolder(enchantment).orElse(null);
        if (current == null || level <= 0) {
            return;
        }
        // ItemEnchantments 的 codec、构造函数和原版组件均只支持 0..255。
        // 越界值必须明确报告，不能显示成另一个被截断的等级。
        if (level > Enchantment.MAX_LEVEL) {
            LOGGER.warn("附魔 {} 的等级 {} 超出 1.21 组件允许的 255，未生成错误等级的书",
                    enchantmentId(current), level);
            return;
        }
        if (OBSERVED_LEVELS.computeIfAbsent(current, ignored -> new TreeSet<>()).add(level) && built) {
            stale = true;
        }
    }

    /** 外部书或旧页面的 Holder 只能按注册键重新进入当前连接的规则。 */
    public static Optional<Holder<Enchantment>> currentHolder(Holder<Enchantment> enchantment) {
        if (!EnchantmentCompat.isAvailable(enchantment) || registry == null) {
            return Optional.empty();
        }
        return enchantment.unwrapKey().flatMap(registry::getHolder).map(holder -> holder);
    }

    private static Holder<Enchantment> requireCurrentHolder(Holder<Enchantment> enchantment) {
        return currentHolder(enchantment).orElseThrow(() -> new IllegalArgumentException(
                "附魔不在当前同步注册表中: " + enchantmentId(enchantment)));
    }

    /** 保留数量与其它组件，只重新绑定附魔引用；未知 ID 明确拒绝而不是沿用旧世界规则。 */
    public static ItemStack rebindEnchantments(ItemStack stack) {
        ItemStack result = stack;
        for (DataComponentType<ItemEnchantments> type : List.of(DataComponents.ENCHANTMENTS, DataComponents.STORED_ENCHANTMENTS)) {
            ItemEnchantments original = stack.get(type);
            if (original == null || original.isEmpty()) {
                continue;
            }
            ItemEnchantments.Mutable rebound = null;
            for (var entry : original.entrySet()) {
                Holder<Enchantment> current = requireCurrentHolder(entry.getKey());
                if (current != entry.getKey()) {
                    if (rebound == null) {
                        // Mutable 保留原组件的 show_in_tooltip，不能重建成默认可见的书。
                        rebound = new ItemEnchantments.Mutable(original);
                    }
                    rebound.removeIf(holder -> holder.equals(entry.getKey()));
                    rebound.upgrade(current, entry.getIntValue());
                }
            }
            if (rebound != null) {
                if (result == stack) {
                    result = stack.copy();
                }
                result.set(type, rebound.toImmutable());
            }
        }
        return result;
    }

    private static ItemStack queryStack(ItemStack stack) {
        try {
            return rebindEnchantments(stack);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("查询物品 {} 含有当前注册表不存在的附魔，已拒绝旧组件查询", itemId(stack.getItem()), exception);
            return ItemStack.EMPTY;
        }
    }

    private static void clearDerivedData() {
        HIGHEST_BOOKS.clear();
        ALL_BOOKS.clear();
        ITEMS_BY_ENCHANT.clear();
        ITEMS_FOR_BOOK.clear();
        CONFLICTS.clear();
        GROUPED_ITEMS.clear();
        ENTRIES.clear();
        REPAIR_MATERIALS.clear();
        REPAIR_TARGETS.clear();
        REPAIR_SOURCES.clear();
        CUSTOM_SUPPORT.clear();
        SUPPORTED_BY_ITEM.clear();
        repairGroups = List.of();
        repairIndexBuilt = false;
    }

    private static void rebuildIndexes() {
        long started = System.nanoTime();
        clearDerivedData();
        for (Holder<Enchantment> enchantment : enchantments) {
            List<Holder<Enchantment>> conflicts = enchantments.stream()
                    .filter(other -> !other.equals(enchantment) && !Enchantment.areCompatible(enchantment, other))
                    .toList();
            CONFLICTS.put(enchantment, conflicts);
            // supported_items 只是候选索引；最终仍调用 NeoForge 的 ItemStack 钩子。
            for (Holder<Item> item : enchantment.value().definition().supportedItems()) {
                SUPPORTED_BY_ITEM.computeIfAbsent(item.value(), ignored -> new ArrayList<>()).add(enchantment);
            }
        }
        List<Holder<Enchantment>> ordered = orderedEnchantments();
        List<ItemStack> accepted = new ArrayList<>();
        for (ItemStack stack : targetStacks) {
            List<BookEntry> books = collectHighestBooks(stack, ordered);
            if (!books.isEmpty()) {
                HIGHEST_BOOKS.put(new StackKey(stack), books);
                accepted.add(stack);
                for (BookEntry book : books) {
                    ITEMS_BY_ENCHANT.computeIfAbsent(book.enchantment(), ignored -> new ArrayList<>()).add(stack);
                }
            }
        }
        enchantableStacks = List.copyOf(accepted);
        for (Holder<Enchantment> enchantment : enchantments) {
            Map<EquipGroup, List<ItemStack>> groups = new EnumMap<>(EquipGroup.class);
            for (ItemStack stack : ITEMS_BY_ENCHANT.getOrDefault(enchantment, List.of())) {
                groups.computeIfAbsent(EquipGroup.classify(stack), ignored -> new ArrayList<>()).add(stack);
            }
            Map<EquipGroup, List<ItemStack>> immutable = new EnumMap<>(EquipGroup.class);
            groups.forEach((group, stacks) -> immutable.put(group, List.copyOf(stacks)));
            GROUPED_ITEMS.put(enchantment, Map.copyOf(immutable));
        }
        built = true;
        stale = false;
        generation++;
        LOGGER.info("1.21 铁砧索引已建立：附魔 {} 种，可附魔组件变体 {} 个，观测附魔书 {} 本，耗时 {} ms",
                enchantments.size(), enchantableStacks.size(), observedBooks.size(),
                (System.nanoTime() - started) / 1_000_000L);
    }

    private static List<Holder<Enchantment>> candidates(ItemStack stack, List<Holder<Enchantment>> ordered) {
        boolean custom = CUSTOM_SUPPORT.computeIfAbsent(stack.getItem().getClass(), type -> {
            try {
                Method method = type.getMethod("supportsEnchantment", ItemStack.class, Holder.class);
                return method.getDeclaringClass() != IItemExtension.class;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return true;
            }
        });
        if (custom || isPoweredEnchantmentUnit(stack)) {
            return ordered;
        }
        List<Holder<Enchantment>> supported = SUPPORTED_BY_ITEM.getOrDefault(stack.getItem(), List.of());
        return ordered.stream().filter(supported::contains).toList();
    }

    private static List<Holder<Enchantment>> orderedEnchantments() {
        List<Holder<Enchantment>> ordered = new ArrayList<>(enchantments);
        ordered.sort(Comparator.comparing(EnchantmentCompat::isCurse));
        return ordered;
    }

    /** 初始索引与晚到组件查询使用同一套候选筛选、适用判定和最高等级规则。 */
    private static List<BookEntry> collectHighestBooks(ItemStack stack, List<Holder<Enchantment>> ordered) {
        List<BookEntry> books = new ArrayList<>();
        for (Holder<Enchantment> enchantment : candidates(stack, ordered)) {
            try {
                if (canApply(enchantment, stack)) {
                    books.add(bookEntry(enchantment, maxLevel(enchantment)));
                }
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.warn("解析物品 {} 的附魔 {} 时出错，已跳过这一关系",
                        itemId(stack.getItem()), enchantmentId(enchantment), exception);
            }
        }
        return List.copyOf(books);
    }

    public static boolean canApply(Holder<Enchantment> enchantment, ItemStack target) {
        Holder<Enchantment> current = currentHolder(enchantment).orElse(null);
        return current != null && canApply(current, target, bookEntry(current, maxLevel(current)).stack());
    }

    public static boolean canApply(Holder<Enchantment> enchantment, ItemStack target, ItemStack book) {
        Holder<Enchantment> current = currentHolder(enchantment).orElse(null);
        if (current == null || !isAnvilBook(book)) {
            return false;
        }
        try {
            return EnchantmentCompat.canApply(rebindEnchantments(target), current, rebindEnchantments(book));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean isPoweredEnchantmentUnit(ItemStack stack) {
        return EnchantmentCompat.isPoweredModule(stack);
    }

    /** 内部共享规则：动力附魔装置只处理真实书的首项，普通装备逐项处理。 */
    public static boolean processesBookEntry(ItemStack target, int index) {
        return index == 0 || !isPoweredEnchantmentUnit(target);
    }

    public static int generation() {
        build();
        return generation;
    }

    public static int maxLevel(Holder<Enchantment> enchantment) {
        Holder<Enchantment> current = requireCurrentHolder(enchantment);
        int declared = current.value().getMaxLevel();
        TreeSet<Integer> observed = OBSERVED_LEVELS.get(current);
        return observed == null || observed.isEmpty() ? declared : Math.max(declared, observed.last());
    }

    /** 定义内的普通等级，加上真实出现过的额外等级；不为一本 255 级书伪造中间等级。 */
    public static List<Integer> levelsToRegister(Holder<Enchantment> enchantment) {
        Holder<Enchantment> current = requireCurrentHolder(enchantment);
        TreeSet<Integer> levels = new TreeSet<>();
        for (int level = 1; level <= current.value().getMaxLevel(); level++) {
            levels.add(level);
        }
        levels.addAll(OBSERVED_LEVELS.getOrDefault(current, new TreeSet<>()));
        return List.copyOf(levels);
    }

    public static List<ItemStack> observedBooks() {
        return observedBooks;
    }

    public static BookEntry bookEntry(Holder<Enchantment> enchantment, int level) {
        Holder<Enchantment> current = requireCurrentHolder(enchantment);
        if (level <= 0 || level > Enchantment.MAX_LEVEL) {
            throw new IllegalArgumentException("Invalid 1.21 enchantment level: " + level);
        }
        return ENTRIES.computeIfAbsent(new BookKey(current, level), ignored -> {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            stored.set(current, level);
            book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
            return new BookEntry(current, level, book, anvilCost(current, level));
        });
    }

    /** 与 1.21.1 AnvilMenu 一致：书的 anvil_cost 减半，最终等级受附魔定义上限约束。 */
    public static int anvilCost(Holder<Enchantment> enchantment, int level) {
        Holder<Enchantment> current = requireCurrentHolder(enchantment);
        int effectiveLevel = Math.min(Math.max(1, level), current.value().getMaxLevel());
        return Math.max(1, current.value().getAnvilCost() / 2) * effectiveLevel;
    }

    public static int anvilCost(Holder<Enchantment> enchantment, int level, ItemStack target) {
        Holder<Enchantment> current = requireCurrentHolder(enchantment);
        if (isPoweredEnchantmentUnit(target)) {
            return Math.multiplyExact(3, target.getCount());
        }
        return anvilCost(current, level);
    }

    public static List<Holder<Enchantment>> enchantments() {
        build();
        return enchantments;
    }

    public static List<ItemStack> enchantableStacks() {
        build();
        return enchantableStacks;
    }

    public static List<Item> enchantableItems() {
        return enchantableStacks().stream().map(ItemStack::getItem).distinct().toList();
    }

    public static List<BookEntry> highestBooks(Item item) {
        return highestBooks(item.getDefaultInstance());
    }

    public static List<BookEntry> highestBooks(ItemStack stack) {
        build();
        ItemStack current = queryStack(stack);
        if (!built || current.isEmpty() || !isEligible(current)) {
            return List.of();
        }
        return HIGHEST_BOOKS.computeIfAbsent(new StackKey(current), ignored -> {
            // 玩家查询的损伤或模块状态可能没有出现在浏览器的初始化样例中。
            return collectHighestBooks(current, orderedEnchantments());
        });
    }

    public static List<BookEntry> allBooks(Item item) {
        return allBooks(item.getDefaultInstance());
    }

    public static List<BookEntry> allBooks(ItemStack stack) {
        build();
        ItemStack current = queryStack(stack);
        if (!built || current.isEmpty() || !isEligible(current)) {
            return List.of();
        }
        return ALL_BOOKS.computeIfAbsent(new StackKey(current), ignored -> {
            List<BookEntry> all = new ArrayList<>();
            for (BookEntry entry : highestBooks(current)) {
                List<Integer> levels = levelsToRegister(entry.enchantment());
                for (int index = levels.size() - 1; index >= 0; index--) {
                    all.add(bookEntry(entry.enchantment(), levels.get(index)));
                }
            }
            return List.copyOf(all);
        });
    }

    public static boolean hasLowerLevels(Item item) {
        return hasLowerLevels(item.getDefaultInstance());
    }

    public static boolean hasLowerLevels(ItemStack stack) {
        return highestBooks(stack).stream().anyMatch(entry -> levelsToRegister(entry.enchantment()).size() > 1);
    }

    public static Map<EquipGroup, List<ItemStack>> groupedItems(Holder<Enchantment> enchantment) {
        build();
        return currentHolder(enchantment).map(current -> GROUPED_ITEMS.getOrDefault(current, Map.of())).orElse(Map.of());
    }

    /** 实际书的钩子可能接受默认单附魔书无法通过的目标；只扫描本次加载已收集的组件样例。 */
    public static List<ItemStack> itemsForBook(Holder<Enchantment> enchantment, ItemStack book) {
        build();
        Holder<Enchantment> current = currentHolder(enchantment).orElse(null);
        ItemStack currentBook = queryStack(book);
        if (!built || current == null || !isAnvilBook(currentBook)) {
            return List.of();
        }
        return ITEMS_FOR_BOOK.computeIfAbsent(new BookTargetKey(current, new StackKey(currentBook)), ignored -> {
            List<ItemStack> targets = new ArrayList<>();
            // groupedItems 是默认单附魔书的反向索引，不能作为组件受限真实书的候选全集。
            for (ItemStack target : targetStacks) {
                try {
                    if (canApply(current, target, currentBook)) {
                        targets.add(target);
                    }
                } catch (RuntimeException | LinkageError exception) {
                    LOGGER.warn("解析物品 {} 对实际书的附魔 {} 适用性时出错，已跳过这一关系",
                            itemId(target.getItem()), enchantmentId(current), exception);
                }
            }
            return List.copyOf(targets);
        });
    }

    public static List<Holder<Enchantment>> conflicts(Holder<Enchantment> enchantment) {
        build();
        return currentHolder(enchantment).map(current -> CONFLICTS.getOrDefault(current, List.of())).orElse(List.of());
    }

    public static List<Holder<Enchantment>> conflictsFor(Holder<Enchantment> enchantment, ItemStack stack) {
        if (EnchantmentCompat.ignoresConflicts(stack)) {
            return List.of();
        }
        return conflicts(enchantment).stream().filter(other -> canApply(other, stack)).toList();
    }

    public static List<ItemStack> repairMaterials(Item item) {
        return repairMaterials(item.getDefaultInstance());
    }

    public static List<ItemStack> repairMaterials(ItemStack stack) {
        build();
        ensureRepairIndex();
        ItemStack current = queryStack(stack);
        if (!built || current.isEmpty() || !current.isDamageableItem()) {
            return List.of();
        }
        return REPAIR_MATERIALS.computeIfAbsent(new StackKey(current), ignored -> {
            try {
                return List.copyOf(collectRepairMaterials(current));
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.warn("解析物品 {} 的组件变体修复材料时出错", itemId(current.getItem()), exception);
                return List.of();
            }
        });
    }

    public static List<RepairGroup> repairGroups() {
        build();
        ensureRepairIndex();
        return repairGroups;
    }

    /** Exact material components for a focused query; only scans the current, precollected targets. */
    public static List<ItemStack> repairableItems(ItemStack material) {
        build();
        ItemStack current = queryStack(material);
        if (!built || current.isEmpty()) {
            return List.of();
        }
        return REPAIR_TARGETS.computeIfAbsent(new StackKey(current), ignored -> {
            List<ItemStack> targets = new ArrayList<>();
            for (ItemStack target : targetStacks) {
                if (!target.isDamageableItem() || target.getItem() == current.getItem()) {
                    continue;
                }
                try {
                    if (target.getItem().isValidRepairItem(target.copy(), current.copy())) {
                        targets.add(target);
                    }
                } catch (RuntimeException | LinkageError exception) {
                    LOGGER.debug("跳过材料 {} 无法修补的组件变体 {}", itemId(current.getItem()), itemId(target.getItem()), exception);
                }
            }
            return List.copyOf(targets);
        });
    }

    private static synchronized void ensureRepairIndex() {
        if (repairIndexBuilt || !built) {
            return;
        }
        long started = System.nanoTime();
        Map<StackKey, List<StackKey>> targetsByMaterial = new LinkedHashMap<>();
        Map<StackKey, ItemStack> materialsByKey = new LinkedHashMap<>();
        Map<StackKey, ItemStack> targetsByKey = new LinkedHashMap<>();
        for (ItemStack target : targetStacks) {
            if (!target.isDamageableItem()) {
                continue;
            }
            try {
                List<ItemStack> materials = collectRepairMaterials(target);
                if (materials.isEmpty()) {
                    continue;
                }
                StackKey targetKey = new StackKey(target);
                targetsByKey.put(targetKey, target);
                REPAIR_MATERIALS.put(targetKey, List.copyOf(materials));
                for (ItemStack material : materials) {
                    StackKey materialKey = new StackKey(material);
                    materialsByKey.putIfAbsent(materialKey, material);
                    targetsByMaterial.computeIfAbsent(materialKey, ignored -> new ArrayList<>()).add(targetKey);
                }
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.warn("解析物品 {} 的修复材料时出错，已跳过该物品", itemId(target.getItem()), exception);
            }
        }
        Map<List<StackKey>, List<ItemStack>> grouped = new LinkedHashMap<>();
        targetsByMaterial.forEach((material, targets) ->
                grouped.computeIfAbsent(List.copyOf(targets), ignored -> new ArrayList<>()).add(materialsByKey.get(material)));
        List<RepairGroup> result = new ArrayList<>();
        grouped.forEach((targets, materials) -> result.add(new RepairGroup(List.copyOf(materials),
                targets.stream().map(targetsByKey::get).toList())));
        repairGroups = List.copyOf(result);
        repairIndexBuilt = true;
        LOGGER.info("铁砧修复索引已建立：{} 组材料，{} 个装备组件变体，耗时 {} ms",
                repairGroups.size(), REPAIR_MATERIALS.size(), (System.nanoTime() - started) / 1_000_000L);
    }

    private static List<ItemStack> collectRepairMaterials(ItemStack target) {
        Item item = target.getItem();
        RepairSource source = REPAIR_SOURCES.computeIfAbsent(item.getClass(), type -> {
            try {
                Class<?> declaring = type.getMethod("isValidRepairItem", ItemStack.class, ItemStack.class).getDeclaringClass();
                if (declaring == TieredItem.class) {
                    return RepairSource.TIER;
                }
                if (declaring == ArmorItem.class) {
                    return RepairSource.ARMOR;
                }
                return declaring == Item.class ? RepairSource.NONE : RepairSource.CUSTOM;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return RepairSource.CUSTOM;
            }
        });
        Ingredient ingredient;
        if (source == RepairSource.TIER) {
            ingredient = ((TieredItem) item).getTier().getRepairIngredient();
        } else if (source == RepairSource.ARMOR) {
            ingredient = ((ArmorItem) item).getMaterial().value().repairIngredient().get();
        } else if (source == RepairSource.NONE) {
            return List.of();
        } else {
            return scanRepairMaterials(target);
        }
        try {
            Map<StackKey, ItemStack> materials = new LinkedHashMap<>();
            for (ItemStack candidate : ingredient.getItems()) {
                if (!candidate.isEmpty() && item.isValidRepairItem(target.copy(), candidate.copy())) {
                    addSample(materials, candidate);
                }
            }
            return List.copyOf(materials.values());
        } catch (RuntimeException | LinkageError exception) {
            return scanRepairMaterials(target);
        }
    }

    private static List<ItemStack> scanRepairMaterials(ItemStack target) {
        Map<StackKey, ItemStack> materials = new LinkedHashMap<>();
        for (ItemStack candidate : materialCandidates) {
            if (candidate.getItem() == target.getItem()) {
                continue;
            }
            try {
                if (target.getItem().isValidRepairItem(target.copy(), candidate.copy())) {
                    addSample(materials, candidate);
                }
            } catch (RuntimeException | LinkageError ignored) {
                // 一个自定义材料的异常不影响其他候选。
            }
        }
        return List.copyOf(materials.values());
    }

    public static Map<String, Integer> statistics() {
        build();
        Map<String, Integer> statistics = new LinkedHashMap<>();
        statistics.put("enchantments", enchantments.size());
        statistics.put("items", enchantableStacks.size());
        statistics.put("books", enchantments.stream().mapToInt(enchantment -> levelsToRegister(enchantment).size()).sum());
        statistics.put("curses", (int) enchantments.stream().filter(EnchantmentCompat::isCurse).count());
        return statistics;
    }

    private static String enchantmentId(Holder<Enchantment> enchantment) {
        if (enchantment == null) {
            return "[unregistered]";
        }
        return enchantment.unwrapKey().map(key -> key.location().toString()).orElse("[unregistered]");
    }

    private static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private record BookKey(Holder<Enchantment> enchantment, int level) {
    }

    private record BookTargetKey(Holder<Enchantment> enchantment, StackKey book) {
    }

    /** ItemStack 自身按对象身份比较；缓存必须使用物品及完整组件且忽略数量。 */
    private static final class StackKey {
        private final ItemStack stack;
        private final int hash;

        StackKey(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
            this.hash = ItemStack.hashItemAndComponents(this.stack);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StackKey key && ItemStack.isSameItemSameComponents(stack, key.stack);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private enum RepairSource {
        TIER,
        ARMOR,
        CUSTOM,
        NONE
    }

    public record RepairGroup(List<ItemStack> materials, List<ItemStack> items) {
    }
}
