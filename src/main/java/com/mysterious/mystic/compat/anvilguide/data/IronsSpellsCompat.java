package com.mysterious.mystic.compat.anvilguide.data;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Iron's Spells 'n Spellbooks（Iron 的法术与魔法书）兼容层。
 *
 * <p>这个模组没有编译期依赖：没装的时候所有方法直接返回空，装了才通过反射调用它的公开 API。
 * 用到的接口都是它自己的 JEI 插件在客户端用同一套逻辑跑过的，所以行为与它的
 * 「奥术铁砧」页签一致：
 * <ul>
 *   <li>{@code Utils#canImbue(ItemStack)} —— 这件物品能不能吃法术卷轴</li>
 *   <li>{@code SpellRegistry#getEnabledSpells()} —— 当前配置下启用的全部法术</li>
 *   <li>{@code ISpellContainer#createScrollContainer(spell, level, stack)} —— 造出一张指定法术与等级的卷轴</li>
 * </ul>
 * 卷轴和附魔书一样按「材料 → 适用物品」参与铁砧表格，于是魔法卷轴与武器的合成表就并进了同一页。</p>
 */
public final class IronsSpellsCompat {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_ID = "irons_spellbooks";
    /** 已按官方 1.21.1 源码核对的版本；其他版本不隐藏其现有配方。 */
    public static final String SUPPORTED_MOD_VERSION = "1.21.1-3.16.4";
    private static final String UTILS = "io.redspace.ironsspellbooks.api.util.Utils";
    private static final String SPELL_REGISTRY = "io.redspace.ironsspellbooks.api.registry.SpellRegistry";
    private static final String ABSTRACT_SPELL = "io.redspace.ironsspellbooks.api.spells.AbstractSpell";
    private static final String SPELL_CONTAINER = "io.redspace.ironsspellbooks.api.spells.ISpellContainer";
    private static final String SCROLL_ITEM_CLASS = "io.redspace.ironsspellbooks.item.Scroll";
    private static final String JEI_RECIPE_CLASS = "io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe";

    private static Boolean available;

    private static Method canImbueMethod;
    private static Method getEnabledSpellsMethod;
    private static Method getMinLevelMethod;
    private static Method getMaxLevelMethod;
    private static Method getSpellNameMethod;
    private static Method createScrollContainerMethod;
    private static Method isSpellContainerMethod;
    private static Class<?> arcaneAnvilRecipeClass;
    private static Field recipeTypeField;
    private static Item scrollItem;

    private static List<ScrollEntry> scrolls;
    private static List<ScrollEntry> everyScroll;
    private static List<ItemStack> imbueableItems;

    private IronsSpellsCompat() {
    }

    /** 当前环境是否装了已核对的 Iron's Spells 1.21.1 版本，并且目标 API 可用。 */
    public static boolean isAvailable() {
        if (available == null) {
            available = detect();
        }
        return available;
    }

    private static boolean detect() {
        try {
            if (!ModList.get().isLoaded(MOD_ID)) {
                return false;
            }
            String version = ModList.get().getModContainerById(MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString()).orElse("");
            if (!SUPPORTED_MOD_VERSION.equals(version)) {
                LOGGER.warn("Iron's Spells {} 未经本模组的 1.21.1 适配核对；目前只适配 {}，保留其原始奥术铁砧页面",
                        version, SUPPORTED_MOD_VERSION);
                return false;
            }
            Class<?> utils = Class.forName(UTILS);
            canImbueMethod = utils.getMethod("canImbue", ItemStack.class);

            Class<?> spellClass = Class.forName(ABSTRACT_SPELL);
            getMinLevelMethod = spellClass.getMethod("getMinLevel");
            getMaxLevelMethod = spellClass.getMethod("getMaxLevel");
            getSpellNameMethod = spellClass.getMethod("getSpellName");
            getEnabledSpellsMethod = Class.forName(SPELL_REGISTRY).getMethod("getEnabledSpells");

            Class<?> containerClass = Class.forName(SPELL_CONTAINER);
            createScrollContainerMethod = containerClass.getMethod("createScrollContainer", spellClass, int.class,
                    ItemStack.class);
            isSpellContainerMethod = containerClass.getMethod("isSpellContainer", ItemStack.class);
            arcaneAnvilRecipeClass = Class.forName(JEI_RECIPE_CLASS);
            recipeTypeField = arcaneAnvilRecipeClass.getDeclaredField("type");
            recipeTypeField.setAccessible(true);
            if (!recipeTypeField.getType().isEnum()) {
                return false;
            }

            scrollItem = findScrollItem();
            if (scrollItem == null) {
                LOGGER.warn("Iron's Spells 已安装但找不到卷轴物品，法术卷轴将不参与铁砧表格");
                return false;
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            LOGGER.warn("Iron's Spells 兼容初始化失败，将不显示法术卷轴", exception);
            return false;
        }
    }

    /**
     * 找卷轴物品：先按注册名 {@code irons_spellbooks:scroll} 取，
     * 取不到再按类名在物品表里兜底，免得模组改了注册名就整段失效。
     */
    private static Item findScrollItem() {
        Item byName = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, "scroll"));
        if (byName != null && byName != Items.AIR) {
            return byName;
        }
        for (Item candidate : BuiltInRegistries.ITEM) {
            if (candidate.getClass().getName().equals(SCROLL_ITEM_CLASS)) {
                return candidate;
            }
        }
        return null;
    }

    /** 奥术铁砧方块物品；模组没装或找不到时返回空堆。 */
    public static ItemStack arcaneAnvilStack() {
        if (!isAvailable()) {
            return ItemStack.EMPTY;
        }
        Item anvil = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, "arcane_anvil"));
        return anvil == null || anvil == Items.AIR ? ItemStack.EMPTY : new ItemStack(anvil);
    }

    /** Iron's Spells 的奥术铁砧 JEI 分类 id（{@code irons_spellbooks:arcane_anvil}）。 */
    public static ResourceLocation arcaneAnvilCategoryId() {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "arcane_anvil");
    }

    /** 这件物品能不能在奥术铁砧上吃法术卷轴。 */
    public static boolean canImbue(ItemStack stack) {
        if (!isAvailable() || stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            // 1.21.1 的 canImbue 读取 spell_container 组件、物品标签及配置，不能按 Item 缓存状态。
            return Boolean.TRUE.equals(canImbueMethod.invoke(null, stack));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    /** 每种法术最高等级的卷轴，按名字排序；没装模组时为空。 */
    public static List<ScrollEntry> maxLevelScrolls() {
        if (!isAvailable()) {
            return List.of();
        }
        if (scrolls != null) {
            return scrolls;
        }
        List<ScrollEntry> result = new ArrayList<>();
        try {
            Object rawSpells = getEnabledSpellsMethod.invoke(null);
            if (rawSpells instanceof List<?> spells) {
                for (Object spell : spells) {
                    ScrollEntry entry = scrollFor(spell, maxLevelOf(spell));
                    if (entry != null) {
                        result.add(entry);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            // 不写缓存：模组数据可能只是还没就绪，下次加载再试
            LOGGER.warn("读取 Iron's Spells 法术列表失败，本次不显示法术卷轴", exception);
            return List.of();
        }
        result.sort(Comparator.comparing(entry -> entry.fullName().getString()));
        if (result.isEmpty()) {
            return List.of();
        }
        scrolls = List.copyOf(result);
        return scrolls;
    }

    /**
     * 每种法术每个等级的卷轴各一张（合并页的输入格用）。
     *
     * <p>卷轴上的法术与等级是物品数据，低等级卷轴在 JEI 里是另一种原料，
     * 只放最高等级的话，对着低等级卷轴查看用途就找不到这张合并表。</p>
     */
    public static List<ScrollEntry> allScrolls() {
        if (!isAvailable()) {
            return List.of();
        }
        if (everyScroll != null) {
            return everyScroll;
        }
        List<ScrollEntry> result = new ArrayList<>();
        try {
            Object rawSpells = getEnabledSpellsMethod.invoke(null);
            if (rawSpells instanceof List<?> spells) {
                for (Object spell : spells) {
                    int minLevel = Math.max(1, minLevelOf(spell));
                    int maxLevel = Math.max(minLevel, maxLevelOf(spell));
                    for (int level = minLevel; level <= maxLevel; level++) {
                        ScrollEntry entry = scrollFor(spell, level);
                        if (entry != null) {
                            result.add(entry);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            LOGGER.warn("读取 Iron's Spells 的法术等级失败，本次不显示卷轴灌注合并页", exception);
            return List.of();
        }
        if (result.isEmpty()) {
            return List.of();
        }
        everyScroll = List.copyOf(result);
        return everyScroll;
    }

    private static int minLevelOf(Object spell) throws ReflectiveOperationException {
        return (Integer) getMinLevelMethod.invoke(spell);
    }

    private static int maxLevelOf(Object spell) {
        try {
            return Math.max(1, (Integer) getMaxLevelMethod.invoke(spell));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return 1;
        }
    }

    private static ScrollEntry scrollFor(Object spell, int level) {
        try {
            ItemStack stack = new ItemStack(scrollItem);
            createScrollContainerMethod.invoke(null, spell, level, stack);
            if (stack.isEmpty() || !Boolean.TRUE.equals(isSpellContainerMethod.invoke(null, stack))) {
                return null;
            }
            return new ScrollEntry(stack, scrollName(stack, spell), level);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            // 单个法术造不出卷轴不影响其他法术
            return null;
        }
    }

    /** 卷轴名带法术与等级；个别法术取名要求玩家上下文时退回法术本身的注册名。 */
    private static Component scrollName(ItemStack stack, Object spell) {
        try {
            Component name = stack.getHoverName();
            if (name != null && !name.getString().isBlank()) {
                return name;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // 退回下面的兜底名字
        }
        try {
            Object spellName = getSpellNameMethod.invoke(spell);
            if (spellName instanceof String value && !value.isBlank()) {
                return Component.literal(value);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // 继续兜底
        }
        return Component.translatable("item.irons_spellbooks.scroll");
    }

    /** 全部能灌注法术的物品（卷轴页反查用），按物品 id 排序；没装模组时为空。 */
    public static List<ItemStack> imbueableItems() {
        if (!isAvailable()) {
            return List.of();
        }
        if (imbueableItems != null) {
            return imbueableItems;
        }
        Map<String, ItemStack> found = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            try {
                ItemStack stack = new ItemStack(item);
                if (canImbue(stack)) {
                    found.put(itemId(item), stack);
                }
            } catch (RuntimeException | LinkageError exception) {
                // 单个模组物品判定异常不影响其他物品
            }
        }
        List<String> keys = new ArrayList<>(found.keySet());
        keys.sort(Comparator.naturalOrder());
        List<ItemStack> result = new ArrayList<>(keys.size());
        for (String key : keys) {
            result.add(found.get(key));
        }
        if (result.isEmpty()) {
            // 同样不缓存空结果：法术容器数据可能晚一步才准备好
            return List.of();
        }
        imbueableItems = List.copyOf(result);
        LOGGER.info("Iron's Spells：可灌注物品 {} 件、法术卷轴 {} 张", imbueableItems.size(),
                maxLevelScrolls().size());
        return imbueableItems;
    }

    /** JEI 重新加载时清掉缓存，跟随配置变化。 */
    public static void invalidate() {
        scrolls = null;
        everyScroll = null;
        imbueableItems = null;
    }

    /**
     * 这张奥术铁砧配方是不是「武器 / 物品 + 卷轴 → 灌注后的物品」那一类。
     *
     * <p>Iron's 把这四类操作（卷轴升级、升级球、灌注、亲和指环）塞在同一条 JEI 页签里，
     * 其中灌注那一类是「一件物品一条配方、右侧列满全部等级的卷轴」，对着卷轴查看用途会
     * 冒出几百页。本模组只隐藏这一类，另外三类（卷轴自身升级、升级球升级物品、
     * 亲和指环灌注）原样留在它自己的页签里。</p>
     *
     * <p>只识别已核对版本的 {@code ArcaneAnvilJeiRecipe.Type.Imbue}；字段不可读时保留原配方。
     * 兼容经 {@code RecipeHolder} 包装的调用，不按列表形状推断其他操作。</p>
     */
    public static boolean isImbueRecipe(Object recipe) {
        if (recipe == null || !isAvailable()) {
            return false;
        }
        if (recipe instanceof RecipeHolder<?> holder) {
            recipe = holder.value();
        }
        if (recipe.getClass() != arcaneAnvilRecipeClass) {
            return false;
        }
        try {
            return recipeTypeField.get(recipe) instanceof Enum<?> constant && "Imbue".equals(constant.name());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static String itemId(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key == null ? item.getDescriptionId() : key.toString();
    }
}
