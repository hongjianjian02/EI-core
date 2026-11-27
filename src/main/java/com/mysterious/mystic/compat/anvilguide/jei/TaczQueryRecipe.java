package com.mysterious.mystic.compat.anvilguide.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mysterious.mystic.compat.anvilguide.data.TaczCompat;
import com.mysterious.mystic.compat.anvilguide.data.TaczCompat.AttachmentEntry;
import com.mysterious.mystic.compat.anvilguide.data.TaczCompat.GunEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 永恒枪械工坊：零的三个查询页共用的一条配方：左边一件东西，右边一整块滚动网格。
 *
 * <p>三种形态（{@link Kind}）：</p>
 * <ul>
 *   <li>{@code AMMO_GUNS}：任意弹药 → 能吃它的全部枪械（按枪种分段）</li>
 *   <li>{@code GUN_ATTACHMENTS}：一把枪 → 它支持的全部配件（按配件位置分段），
 *       枪械格下方另放它用的弹药</li>
 *   <li>{@code ATTACHMENT_GUNS}：一个配件 → 能装它的全部枪械（按枪种分段）</li>
 * </ul>
 *
 * <p>网格里每个段落前空一整行画分隔带，段落一律从新的一行开始，
 * 和铁砧附魔页里「诅咒附魔」的分隔方式一致（复用的 {@link SectionHeader}）。</p>
 */
public final class TaczQueryRecipe {

    /** 三个页签各自的文案键。 */
    public enum Kind {
        AMMO_GUNS("eicore.anvil_guide.tacz.category.ammo_guns",
                "eicore.anvil_guide.tacz.stats.guns",
                "eicore.anvil_guide.tacz.hint.ammo_guns",
                "eicore.anvil_guide.tacz.legend.ammo_guns"),
        GUN_ATTACHMENTS("eicore.anvil_guide.tacz.category.gun_attachments",
                "eicore.anvil_guide.tacz.stats.attachments",
                "eicore.anvil_guide.tacz.hint.gun_attachments",
                "eicore.anvil_guide.tacz.legend.gun_attachments"),
        ATTACHMENT_GUNS("eicore.anvil_guide.tacz.category.attachment_guns",
                "eicore.anvil_guide.tacz.stats.guns",
                "eicore.anvil_guide.tacz.hint.attachment_guns",
                "eicore.anvil_guide.tacz.legend.attachment_guns");

        private final String titleKey;
        private final String statsKey;
        private final String hintKey;
        private final String legendKey;

        Kind(String titleKey, String statsKey, String hintKey, String legendKey) {
            this.titleKey = titleKey;
            this.statsKey = statsKey;
            this.hintKey = hintKey;
            this.legendKey = legendKey;
        }

        public String titleKey() {
            return titleKey;
        }

        public String statsKey() {
            return statsKey;
        }

        public String hintKey() {
            return hintKey;
        }

        public String legendKey() {
            return legendKey;
        }
    }

    /**
     * 网格里的一格。
     *
     * @param stack   物品；占位格与分隔带为空堆
     * @param header  分隔带标签；不是分隔带为 {@code null}
     * @param tooltip 悬停提示；占位格与分隔带为空列表
     */
    public record Cell(ItemStack stack, Component header, List<Component> tooltip) {

        public static Cell item(ItemStack stack, List<Component> tooltip) {
            return new Cell(stack, null, List.copyOf(tooltip));
        }

        public static Cell spacer() {
            return new Cell(ItemStack.EMPTY, null, List.of());
        }

        public static Cell header(Component title) {
            return new Cell(ItemStack.EMPTY, title, List.of());
        }

        public boolean isHeader() {
            return header != null;
        }

        public boolean hasItem() {
            return header == null && !stack.isEmpty();
        }
    }

    private final Kind kind;
    private final ItemStack input;
    private final Component inputTooltip;
    private final List<ItemStack> ammo;
    private final List<Cell> cells;
    private final int itemCount;

    private TaczQueryRecipe(Kind kind, ItemStack input, Component inputTooltip, List<ItemStack> ammo,
                            List<Cell> cells) {
        this.kind = kind;
        this.input = input;
        this.inputTooltip = inputTooltip;
        this.ammo = List.copyOf(ammo);
        this.cells = List.copyOf(cells);
        int count = 0;
        for (Cell cell : this.cells) {
            if (cell.hasItem()) {
                count++;
            }
        }
        this.itemCount = count;
    }

    public Kind kind() {
        return kind;
    }

    public ItemStack input() {
        return input;
    }

    public Component inputTooltip() {
        return inputTooltip;
    }

    /** 枪械页里显示在枪械下方的弹药；别的页签为空列表。 */
    public List<ItemStack> ammo() {
        return ammo;
    }

    public List<Cell> cells() {
        return cells;
    }

    /** 网格里真正放了物品的格数（分隔带与占位格不算）。 */
    public int itemCount() {
        return itemCount;
    }

    /** 一屏装不下才给滚动条留位置，和铁砧附魔页一致。 */
    public boolean needsScrollbar() {
        return cells.size() > Layout.COLUMNS * Layout.ROWS;
    }

    // ---------------------------------------------------------------- 建表

    /** 弹药 → 枪械：每种弹药一条，落在这条配方里的枪械按枪种分段。 */
    public static List<TaczQueryRecipe> ammoPages() {
        if (!TaczCompat.isAvailable()) {
            return List.of();
        }
        List<TaczQueryRecipe> pages = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<GunEntry>> entry
                : TaczCompat.gunsByAmmo().entrySet()) {
            ItemStack ammo = TaczCompat.ammoStack(entry.getKey());
            if (ammo.isEmpty() || entry.getValue().isEmpty()) {
                continue;
            }
            List<Cell> sections = gunSections(entry.getValue());
            if (sections.isEmpty()) {
                continue;
            }
            Component tooltip = Component.translatable("eicore.anvil_guide.tacz.tooltip.ammo_input",
                    ammo.getHoverName(), entry.getValue().size());
            pages.add(new TaczQueryRecipe(Kind.AMMO_GUNS, ammo, tooltip, List.of(), sections));
        }
        return List.copyOf(pages);
    }

    /** 枪械 → 配件：每把有配件的枪一条，配件按位置分段，输入格下方是它吃的弹药。 */
    public static List<TaczQueryRecipe> gunPages() {
        if (!TaczCompat.isAvailable()) {
            return List.of();
        }
        Map<ResourceLocation, List<AttachmentEntry>> byGun = TaczCompat.attachmentsByGun();
        List<TaczQueryRecipe> pages = new ArrayList<>();
        for (GunEntry gun : TaczCompat.guns()) {
            List<AttachmentEntry> allowed = byGun.get(gun.id());
            if (allowed == null || allowed.isEmpty()) {
                continue;
            }
            List<Cell> sections = attachmentSections(allowed);
            if (sections.isEmpty()) {
                continue;
            }
            List<ItemStack> ammo = new ArrayList<>(1);
            ItemStack ammoStack = TaczCompat.ammoStack(gun.ammoId());
            if (!ammoStack.isEmpty()) {
                ammo.add(ammoStack);
            }
            Component tooltip = Component.translatable("eicore.anvil_guide.tacz.tooltip.gun_input",
                    gun.stack().getHoverName(), allowed.size());
            pages.add(new TaczQueryRecipe(Kind.GUN_ATTACHMENTS, gun.stack(), tooltip, ammo, sections));
        }
        return List.copyOf(pages);
    }

    /** 配件 → 枪械：每个配件一条，能装它的枪械按枪种分段。 */
    public static List<TaczQueryRecipe> attachmentPages() {
        if (!TaczCompat.isAvailable()) {
            return List.of();
        }
        Map<ResourceLocation, List<GunEntry>> byAttachment =
                TaczCompat.gunsByAttachment();
        List<TaczQueryRecipe> pages = new ArrayList<>();
        for (AttachmentEntry attachment : TaczCompat.attachments()) {
            List<GunEntry> guns = byAttachment.get(attachment.id());
            if (guns == null || guns.isEmpty()) {
                continue;
            }
            List<Cell> sections = gunSections(guns);
            if (sections.isEmpty()) {
                continue;
            }
            Component tooltip = Component.translatable("eicore.anvil_guide.tacz.tooltip.attachment_input",
                    attachment.stack().getHoverName(), guns.size());
            pages.add(new TaczQueryRecipe(Kind.ATTACHMENT_GUNS, attachment.stack(), tooltip, List.of(), sections));
        }
        return List.copyOf(pages);
    }

    /** 按枪种分段：同一枪种的枪械排在一起，段前一条分隔带。 */
    private static List<Cell> gunSections(List<GunEntry> guns) {
        Map<String, List<Cell>> grouped = new LinkedHashMap<>();
        for (GunEntry gun : guns) {
            grouped.computeIfAbsent(gun.type() == null ? "" : gun.type(), key -> new ArrayList<>())
                    .add(Cell.item(gun.stack(), gunTooltip(gun)));
        }
        List<Cell> cells = new ArrayList<>();
        for (Map.Entry<String, List<Cell>> entry : grouped.entrySet()) {
            Section section = new Section(gunTypeName(entry.getKey()), entry.getValue());
            appendSection(cells, section);
        }
        return cells;
    }

    /** 按配件位置分段：同一位置的配件排在一起，段前一条分隔带。 */
    private static List<Cell> attachmentSections(List<AttachmentEntry> attachments) {
        Map<String, List<Cell>> grouped = new LinkedHashMap<>();
        for (AttachmentEntry attachment : attachments) {
            grouped.computeIfAbsent(attachment.slot(), key -> new ArrayList<>())
                    .add(Cell.item(attachment.stack(), attachmentTooltip(attachment)));
        }
        List<Cell> cells = new ArrayList<>();
        for (Map.Entry<String, List<Cell>> entry : grouped.entrySet()) {
            Section section = new Section(slotName(entry.getKey()), entry.getValue());
            appendSection(cells, section);
        }
        return cells;
    }

    private record Section(Component title, List<Cell> items) {
    }

    /**
     * 把一个段落接进网格：先把当前行补满占位格，再放一整行分隔带（最后一格挂标签，
     * 由 {@link SectionHeader} 往左铺满整行），最后才是这一段的物品。
     */
    private static void appendSection(List<Cell> cells, Section section) {
        if (section.items().isEmpty()) {
            return;
        }
        int remainder = cells.size() % Layout.COLUMNS;
        if (remainder != 0) {
            for (int index = remainder; index < Layout.COLUMNS; index++) {
                cells.add(Cell.spacer());
            }
        }
        for (int index = 0; index < Layout.COLUMNS - 1; index++) {
            cells.add(Cell.spacer());
        }
        cells.add(Cell.header(section.title()));
        cells.addAll(section.items());
    }

    // ---------------------------------------------------------------- 文案

    private static List<Component> gunTooltip(GunEntry gun) {
        List<Component> tooltip = new ArrayList<>(3);
        tooltip.add(Component.translatable("eicore.anvil_guide.tacz.tooltip.gun",
                gun.stack().getHoverName()));
        tooltip.add(Component.translatable("eicore.anvil_guide.tacz.tooltip.gun_type",
                gunTypeName(gun.type())));
        ItemStack ammo = TaczCompat.ammoStack(gun.ammoId());
        if (!ammo.isEmpty()) {
            tooltip.add(Component.translatable("eicore.anvil_guide.tacz.tooltip.ammo",
                    ammo.getHoverName()));
        }
        return List.copyOf(tooltip);
    }

    private static List<Component> attachmentTooltip(AttachmentEntry attachment) {
        List<Component> tooltip = new ArrayList<>(2);
        tooltip.add(Component.translatable("eicore.anvil_guide.tacz.tooltip.attachment",
                attachment.stack().getHoverName()));
        tooltip.add(Component.translatable("eicore.anvil_guide.tacz.tooltip.slot",
                slotName(attachment.slot())));
        return List.copyOf(tooltip);
    }

    /** 枪种显示名：本模组自带 7 种常见枪种的译名，枪械包加了新枪种就直接显示原字样。 */
    public static Component gunTypeName(String type) {
        String key = type == null || type.isBlank() ? "other" : type.toLowerCase(Locale.ROOT);
        return Component.translatableWithFallback("eicore.anvil_guide.tacz.gun_type." + key,
                type == null || type.isBlank() ? "其它" : type);
    }

    /** 配件位置显示名，同理。 */
    public static Component slotName(String slot) {
        String key = slot == null || slot.isBlank() ? "none" : slot.toLowerCase(Locale.ROOT);
        return Component.translatableWithFallback("eicore.anvil_guide.tacz.slot." + key,
                slot == null || slot.isBlank() ? "配件" : slot);
    }
}
