package com.mysterious.mystic.compat.anvilguide.data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * 永恒枪械工坊：零（Timeless and Classics Zero，modid {@code tacz}）兼容层。
 *
 * <p>这个模组没有编译期依赖：没装时所有方法直接返回空，装了才通过反射调用它的公开 API。
 * 用到的都是它自己的 JEI 插件在客户端跑过一遍的接口：</p>
 *
 * <ul>
 *   <li>{@code TimelessAPI#getAllCommonGunIndex()} / {@code getAllCommonAttachmentIndex()}
 *       —— 当前数据包里的全部枪械与配件</li>
 *   <li>{@code CommonGunIndex#getType()} —— 枪种（rifle / pistol …）；
 *       {@code GunData#getAmmoId()} —— 这把枪吃哪种弹药；
 *       {@code GunData#getAllowAttachments()} —— 这把枪能装哪几个位置的配件</li>
 *   <li>{@code IGun#allowAttachment(gun, attachment)} —— 它自己判断「这把枪能不能装这个配件」，
 *       配件过滤、专属配件、黑名单全都算在内，本模组不另写一套规则</li>
 *   <li>{@code GunItemBuilder / AmmoItemBuilder / AttachmentItemBuilder} —— 造出带完整数据的物品堆，
 *       JEI 里才能按枪械 id / 弹药 id / 配件 id 精确定位</li>
 * </ul>
 *
 * <p>枪械、配件、弹药各建一次索引，之后按「弹药 → 枪械」「枪械 → 配件」「配件 → 枪械」
 * 三种视图直接查这三个表。</p>
 */
public final class TaczCompat {

    public static final String MOD_ID = "tacz";
    /** 已按官方 1.21.1 源码核对的版本；其他版本保留原模组的 JEI 页面。 */
    public static final String SUPPORTED_MOD_VERSION = "1.2.0-hotfix";
    /** TACZ 全部类的包前缀，用来认出它（或它的附属）注册的 JEI 页签。 */
    public static final String MOD_ID_PACKAGE = "com.tacz.";

    private static final Set<ResourceLocation> QUERY_CATEGORY_IDS = Set.of(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "ammo_query"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "attachment_query"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "gun_ammo_query"),
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "gun_attachment_query"));

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TIMELESS_API = "com.tacz.guns.api.TimelessAPI";
    private static final String GUN_INDEX = "com.tacz.guns.resource.index.CommonGunIndex";
    private static final String ATTACHMENT_INDEX = "com.tacz.guns.resource.index.CommonAttachmentIndex";
    private static final String GUN_BUILDER = "com.tacz.guns.api.item.builder.GunItemBuilder";
    private static final String AMMO_BUILDER = "com.tacz.guns.api.item.builder.AmmoItemBuilder";
    private static final String ATTACHMENT_BUILDER = "com.tacz.guns.api.item.builder.AttachmentItemBuilder";
    private static final String GUN_ITEM = "com.tacz.guns.api.item.IGun";

    /** 枪种在页签里的排列顺序；没列到的枪种排在最后。 */
    private static final List<String> GUN_TYPE_ORDER = List.of("rifle", "smg", "pistol", "shotgun", "sniper", "mg", "rpg");

    private static Boolean available;

    private static Method getAllGunIndex;
    private static Method getAllAttachmentIndex;
    private static Method gunCreate;
    private static Method gunSetId;
    private static Method gunBuild;
    private static Method ammoCreate;
    private static Method ammoSetId;
    private static Method ammoBuild;
    private static Method attachmentCreate;
    private static Method attachmentSetId;
    private static Method attachmentBuild;
    private static Method getIGunOrNull;
    private static Method allowAttachment;
    private static Method indexGetType;
    private static Method indexGetGunData;
    private static Method gunDataGetAmmoId;
    private static Method gunDataGetAllowAttachments;
    private static Method attachmentIndexGetPojo;
    private static Method pojoGetType;
    private static Method pojoIsHidden;

    private static List<GunEntry> guns;
    private static List<AttachmentEntry> attachments;
    private static Map<ResourceLocation, List<GunEntry>> gunsByAmmo;
    private static Map<ResourceLocation, List<AttachmentEntry>> attachmentsByGun;
    private static Map<ResourceLocation, List<GunEntry>> gunsByAttachment;
    private static final Map<ResourceLocation, ItemStack> AMMO_STACKS = new HashMap<>();

    /**
     * 一把枪。
     *
     * @param id          枪械 id（{@code tacz:ak47} 这种）
     * @param stack       带完整数据的物品堆，直接放进 JEI 格子
     * @param type        枪种，来自数据包的 {@code type} 字段
     * @param ammoId      它吃的弹药 id
     * @param allowedSlots 它能装的配件位置（{@code AttachmentType} 的枚举名）
     */
    public record GunEntry(ResourceLocation id, ItemStack stack, String type, ResourceLocation ammoId,
                           Set<String> allowedSlots) {
    }

    /**
     * 一个配件。
     *
     * @param id    配件 id
     * @param stack 带完整数据的物品堆
     * @param slot  配件位置（{@code AttachmentType} 的枚举名，大写）
     * @param order 位置在枚举里的序号，用来决定分隔段的先后
     */
    public record AttachmentEntry(ResourceLocation id, ItemStack stack, String slot, int order) {
    }

    private TaczCompat() {
    }

    /** 当前环境是否装了已核对的 TACZ 1.21.1 版本，并且目标 API 可用。 */
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
                LOGGER.warn("TACZ {} 未经本模组的 1.21.1 适配核对；目前只适配 {}，保留其原始 JEI 查询页面",
                        version, SUPPORTED_MOD_VERSION);
                return false;
            }
            Class<?> api = Class.forName(TIMELESS_API);
            getAllGunIndex = api.getMethod("getAllCommonGunIndex");
            getAllAttachmentIndex = api.getMethod("getAllCommonAttachmentIndex");

            Class<?> gunBuilder = Class.forName(GUN_BUILDER);
            gunCreate = gunBuilder.getMethod("create");
            gunSetId = gunBuilder.getMethod("setId", ResourceLocation.class);
            gunBuild = gunBuilder.getMethod("build");

            Class<?> ammoBuilder = Class.forName(AMMO_BUILDER);
            ammoCreate = ammoBuilder.getMethod("create");
            ammoSetId = ammoBuilder.getMethod("setId", ResourceLocation.class);
            ammoBuild = ammoBuilder.getMethod("build");

            Class<?> attachmentBuilder = Class.forName(ATTACHMENT_BUILDER);
            attachmentCreate = attachmentBuilder.getMethod("create");
            attachmentSetId = attachmentBuilder.getMethod("setId", ResourceLocation.class);
            attachmentBuild = attachmentBuilder.getMethod("build");

            Class<?> gunItem = Class.forName(GUN_ITEM);
            getIGunOrNull = gunItem.getMethod("getIGunOrNull", ItemStack.class);
            allowAttachment = gunItem.getMethod("allowAttachment", ItemStack.class, ItemStack.class);

            Class<?> gunIndex = Class.forName(GUN_INDEX);
            indexGetType = gunIndex.getMethod("getType");
            indexGetGunData = gunIndex.getMethod("getGunData");
            Class<?> gunData = indexGetGunData.getReturnType();
            gunDataGetAmmoId = gunData.getMethod("getAmmoId");
            // 允许的配件位置是可选的：读不到就退回升级版的「每个配件都问一遍 allowAttachment」
            gunDataGetAllowAttachments = optionalMethod(gunData, "getAllowAttachments");

            Class<?> attachmentIndex = Class.forName(ATTACHMENT_INDEX);
            attachmentIndexGetPojo = attachmentIndex.getMethod("getPojo");
            Class<?> pojo = attachmentIndexGetPojo.getReturnType();
            pojoGetType = pojo.getMethod("getType");
            pojoIsHidden = optionalMethod(pojo, "isHidden");
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            LOGGER.warn("TACZ 兼容初始化失败，将不显示枪械 / 配件相关页签", exception);
            return false;
        }
    }

    private static Method optionalMethod(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    /** 官方目标版本的四个查询分类；不会匹配枪械工作台或附属模组分类。 */
    public static Set<ResourceLocation> queryCategoryIds() {
        return isAvailable() ? QUERY_CATEGORY_IDS : Set.of();
    }

    /** 全部枪械，按枪种再按 id 排序；没装 TACZ 时为空。 */
    public static List<GunEntry> guns() {
        ensureData();
        return guns == null ? List.of() : guns;
    }

    /** 全部配件（跳过数据包里标了 hidden 的），按位置再按 id 排序。 */
    public static List<AttachmentEntry> attachments() {
        ensureData();
        return attachments == null ? List.of() : attachments;
    }

    /** 弹药 id → 吃这种弹药的枪械。 */
    public static Map<ResourceLocation, List<GunEntry>> gunsByAmmo() {
        ensureData();
        return gunsByAmmo == null ? Map.of() : gunsByAmmo;
    }

    /** 枪械 id → 它支持的配件（按位置排序）。 */
    public static Map<ResourceLocation, List<AttachmentEntry>> attachmentsByGun() {
        ensureData();
        return attachmentsByGun == null ? Map.of() : attachmentsByGun;
    }

    /** 配件 id → 支持它的枪械（按枪种再按 id 排序）。 */
    public static Map<ResourceLocation, List<GunEntry>> gunsByAttachment() {
        ensureData();
        return gunsByAttachment == null ? Map.of() : gunsByAttachment;
    }

    /** 按弹药 id 造一个弹药物品堆；造不出来时返回空堆。 */
    public static ItemStack ammoStack(ResourceLocation ammoId) {
        if (ammoId == null || !isAvailable()) {
            return ItemStack.EMPTY;
        }
        ItemStack cached = AMMO_STACKS.get(ammoId);
        if (cached != null) {
            return cached;
        }
        ItemStack stack = build(ammoCreate, ammoSetId, ammoBuild, ammoId);
        if (!stack.isEmpty()) {
            AMMO_STACKS.put(ammoId, stack);
        }
        return stack;
    }

    /** 页签图标用的代表物品：任意一把枪 / 任意一个配件 / 任意一种弹药。 */
    public static ItemStack firstGunStack() {
        List<GunEntry> all = guns();
        return all.isEmpty() ? itemStack("modern_kinetic_gun") : all.get(0).stack();
    }

    public static ItemStack firstAttachmentStack() {
        List<AttachmentEntry> all = attachments();
        return all.isEmpty() ? itemStack("attachment") : all.get(0).stack();
    }

    public static ItemStack firstAmmoStack() {
        for (ResourceLocation ammoId : gunsByAmmo().keySet()) {
            ItemStack stack = ammoStack(ammoId);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return itemStack("ammo");
    }

    /** 枪种在页签里的排列名次。 */
    public static int gunTypeOrder(String type) {
        int index = type == null ? -1 : GUN_TYPE_ORDER.indexOf(type.toLowerCase(Locale.ROOT));
        return index < 0 ? GUN_TYPE_ORDER.size() : index;
    }

    /** 数据是否已经就绪（枪械表非空）。页签只在有数据时才注册配方。 */
    public static boolean hasData() {
        return !guns().isEmpty();
    }

    /**
     * 数据版本戳：枪械 / 配件数量变了（换数据包、加枪械包）就重建页签。
     */
    public static int stamp() {
        return guns().size() * 1000 + attachments().size() * 7 + gunsByAmmo().size();
    }

    /** JEI 重新加载时清掉缓存，让数据包改动重新生效。 */
    public static void invalidate() {
        guns = null;
        attachments = null;
        gunsByAmmo = null;
        attachmentsByGun = null;
        gunsByAttachment = null;
        AMMO_STACKS.clear();
    }

    private static void ensureData() {
        if (guns != null || !isAvailable()) {
            return;
        }
        try {
            buildData();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            // 不写缓存：数据包可能只是还没加载完，下次注册配方时再试
            guns = null;
            attachments = null;
            LOGGER.warn("读取 TACZ 枪械 / 配件数据失败，本次不显示枪械页签", exception);
        }
    }

    @SuppressWarnings("unchecked") // 反射拿回来的都是擦除后的 Map.Entry 集合
    private static void buildData() throws ReflectiveOperationException {
        List<GunEntry> gunList = new ArrayList<>();
        for (Object raw : (Set<?>) getAllGunIndex.invoke(null)) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) raw;
            if (!(entry.getKey() instanceof ResourceLocation id)) {
                continue;
            }
            Object index = entry.getValue();
            if (index == null) {
                continue;
            }
            Object data = indexGetGunData.invoke(index);
            if (data == null) {
                continue;
            }
            ItemStack stack = build(gunCreate, gunSetId, gunBuild, id);
            if (stack.isEmpty()) {
                continue;
            }
            String type = stringOf(indexGetType.invoke(index));
            ResourceLocation ammoId = gunDataGetAmmoId.invoke(data) instanceof ResourceLocation ammo ? ammo : null;
            gunList.add(new GunEntry(id, stack, type, ammoId, allowedSlots(data)));
        }
        gunList.sort(Comparator.comparingInt((GunEntry gun) -> gunTypeOrder(gun.type()))
                .thenComparing(gun -> gun.id().toString()));

        List<AttachmentEntry> attachmentList = new ArrayList<>();
        for (Object raw : (Set<?>) getAllAttachmentIndex.invoke(null)) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) raw;
            if (!(entry.getKey() instanceof ResourceLocation id) || entry.getValue() == null) {
                continue;
            }
            Object pojo = attachmentIndexGetPojo.invoke(entry.getValue());
            if (pojo == null) {
                continue;
            }
            if (pojoIsHidden != null && Boolean.TRUE.equals(pojoIsHidden.invoke(pojo))) {
                continue;
            }
            Object typeValue = pojoGetType.invoke(pojo);
            if (!(typeValue instanceof Enum<?> type)) {
                continue;
            }
            ItemStack stack = build(attachmentCreate, attachmentSetId, attachmentBuild, id);
            if (stack.isEmpty()) {
                continue;
            }
            attachmentList.add(new AttachmentEntry(id, stack, type.name(), type.ordinal()));
        }
        attachmentList.sort(Comparator.comparingInt(AttachmentEntry::order)
                .thenComparing(attachment -> attachment.id().toString()));

        Map<ResourceLocation, List<AttachmentEntry>> byGun = new LinkedHashMap<>();
        Map<ResourceLocation, List<GunEntry>> byAttachment = new LinkedHashMap<>();
        int checks = 0;
        for (GunEntry gun : gunList) {
            Object igun = getIGunOrNull.invoke(null, gun.stack());
            if (igun == null) {
                continue;
            }
            List<AttachmentEntry> allowed = new ArrayList<>();
            for (AttachmentEntry attachment : attachmentList) {
                if (!gun.allowedSlots().isEmpty() && !gun.allowedSlots().contains(attachment.slot())) {
                    continue;
                }
                checks++;
                boolean fits;
                try {
                    fits = Boolean.TRUE.equals(allowAttachment.invoke(igun, gun.stack(), attachment.stack()));
                } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                    // Method.invoke 会把目标异常包装为 InvocationTargetException，也应只跳过当前配件。
                    LOGGER.debug("跳过 TACZ 配件适配判定：枪械 {}，配件 {}", gun.id(), attachment.id(), exception);
                    continue;
                }
                if (!fits) {
                    continue;
                }
                allowed.add(attachment);
                byAttachment.computeIfAbsent(attachment.id(), key -> new ArrayList<>()).add(gun);
            }
            if (!allowed.isEmpty()) {
                byGun.put(gun.id(), allowed);
            }
        }

        Map<ResourceLocation, List<GunEntry>> byAmmo = new LinkedHashMap<>();
        for (GunEntry gun : gunList) {
            if (gun.ammoId() != null) {
                byAmmo.computeIfAbsent(gun.ammoId(), key -> new ArrayList<>()).add(gun);
            }
        }

        Map<ResourceLocation, List<GunEntry>> frozenByAttachment = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<GunEntry>> entry : byAttachment.entrySet()) {
            frozenByAttachment.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        Map<ResourceLocation, List<AttachmentEntry>> frozenByGun = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<AttachmentEntry>> entry : byGun.entrySet()) {
            frozenByGun.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        Map<ResourceLocation, List<GunEntry>> frozenByAmmo = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<GunEntry>> entry : byAmmo.entrySet()) {
            frozenByAmmo.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        attachments = List.copyOf(attachmentList);
        guns = List.copyOf(gunList);
        attachmentsByGun = frozenByGun;
        gunsByAttachment = frozenByAttachment;
        gunsByAmmo = frozenByAmmo;
        LOGGER.info("TACZ：枪械 {} 把、配件 {} 个、弹药 {} 种，配件适配判定 {} 次",
                guns.size(), attachments.size(), gunsByAmmo.size(), checks);
    }

    private static Set<String> allowedSlots(Object gunData) {
        Set<String> slots = new LinkedHashSet<>();
        if (gunDataGetAllowAttachments == null) {
            return slots;
        }
        try {
            if (gunDataGetAllowAttachments.invoke(gunData) instanceof List<?> list) {
                for (Object value : list) {
                    if (value instanceof Enum<?> type) {
                        slots.add(type.name());
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            // 读不到就让调用方退回逐配件判定
        }
        return slots;
    }

    private static ItemStack build(Method create, Method setId, Method buildMethod, ResourceLocation id) {
        try {
            Object builder = create.invoke(null);
            setId.invoke(builder, id);
            Object result = buildMethod.invoke(builder);
            return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return ItemStack.EMPTY;
        }
    }

    /** 数据还没读到（页签图标早于数据加载）时，退回 TACZ 的原始物品。 */
    private static ItemStack itemStack(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static String stringOf(Object value) {
        return value instanceof String text ? text : "";
    }
}
