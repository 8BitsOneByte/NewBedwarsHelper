package org.exmple.newbedwarshelper.client.itemprotection;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.exmple.newbedwarshelper.ModConstants;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ItemProtectionManager {
    public static final String BLOCK_ITEM_MATCH_TYPE = "block_item";
    public static final String COMPONENTS_MATCH_TYPE = "components";

    private static final Logger LOGGER = LoggerFactory.getLogger(ModConstants.MOD_ID + "/item-protection");
    private static final Map<ItemStack, CachedSignature> SIGNATURE_CACHE = new WeakHashMap<>();

    private static RuleIndex ruleIndex = RuleIndex.EMPTY;

    private ItemProtectionManager() {
    }

    public static void init() {
        rebuildIndex();
        ItemProtectionCommands.register();
    }

    public static boolean isProtected(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String itemId = itemId(stack);
        if (stack.getItem() instanceof BlockItem && ruleIndex.blockItemIds().contains(itemId)) {
            return true;
        }

        Set<JsonElement> signatures = ruleIndex.componentSignatures().get(itemId);
        if (signatures == null || signatures.isEmpty()) {
            return false;
        }

        JsonElement signature = signature(stack);
        return signature != null && signatures.contains(signature);
    }

    public static ChangeResult protect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ChangeResult.NO_ITEM;
        }

        ModConfig.ItemProtectionRule rule = createRule(stack);
        if (rule == null) {
            return ChangeResult.SIGNATURE_FAILED;
        }
        if (containsRule(rule)) {
            return ChangeResult.ALREADY_PRESENT;
        }

        ModConfig config = ModConfig.getInstance();
        config.itemProtection.rules.add(rule);
        config.save();
        rebuildIndex();
        return ChangeResult.CHANGED;
    }

    public static ChangeResult unprotect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ChangeResult.NO_ITEM;
        }

        ModConfig.ItemProtectionRule rule = createRule(stack);
        if (rule == null) {
            return ChangeResult.SIGNATURE_FAILED;
        }

        ModConfig config = ModConfig.getInstance();
        boolean removed = config.itemProtection.rules.removeIf(existing -> sameRule(existing, rule));
        if (!removed) {
            return ChangeResult.NOT_PRESENT;
        }

        config.save();
        rebuildIndex();
        return ChangeResult.CHANGED;
    }

    public static boolean canFullyReturnToInventory(Player player, ItemStack carried) {
        if (player == null || carried == null || carried.isEmpty()) {
            return true;
        }

        int remaining = carried.getCount();
        List<ItemStack> inventoryItems = player.getInventory().getNonEquipmentItems();

        for (ItemStack inventoryStack : inventoryItems) {
            if (inventoryStack.isEmpty() || !ItemStack.isSameItemSameComponents(inventoryStack, carried)) {
                continue;
            }

            int stackLimit = Math.min(inventoryStack.getMaxStackSize(), carried.getMaxStackSize());
            remaining -= Math.max(0, stackLimit - inventoryStack.getCount());
            if (remaining <= 0) {
                return true;
            }
        }

        int emptySlotCapacity = Math.max(1, carried.getMaxStackSize());
        for (ItemStack inventoryStack : inventoryItems) {
            if (!inventoryStack.isEmpty()) {
                continue;
            }

            remaining -= emptySlotCapacity;
            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private static ModConfig.ItemProtectionRule createRule(ItemStack stack) {
        String itemId = itemId(stack);
        if (stack.getItem() instanceof BlockItem) {
            return new ModConfig.ItemProtectionRule(BLOCK_ITEM_MATCH_TYPE, itemId, null);
        }

        JsonElement signature = signature(stack);
        return signature == null
                ? null
                : new ModConfig.ItemProtectionRule(COMPONENTS_MATCH_TYPE, itemId, signature);
    }

    private static boolean containsRule(ModConfig.ItemProtectionRule candidate) {
        return ModConfig.getInstance().itemProtection.rules.stream()
                .anyMatch(existing -> sameRule(existing, candidate));
    }

    private static boolean sameRule(ModConfig.ItemProtectionRule first, ModConfig.ItemProtectionRule second) {
        if (first == null || second == null
                || !second.matchType.equals(first.matchType)
                || !second.itemId.equals(first.itemId)) {
            return false;
        }
        if (BLOCK_ITEM_MATCH_TYPE.equals(second.matchType)) {
            return true;
        }
        return second.signature != null && second.signature.equals(first.signature);
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static JsonElement signature(ItemStack stack) {
        int sourceHash = ItemStack.hashItemAndComponents(stack);
        CachedSignature cached = SIGNATURE_CACHE.get(stack);
        if (cached != null && cached.sourceHash() == sourceHash) {
            return cached.signature();
        }

        JsonElement signature = createSignature(stack);
        if (signature != null) {
            SIGNATURE_CACHE.put(stack, new CachedSignature(sourceHash, signature));
        }
        return signature;
    }

    private static JsonElement createSignature(ItemStack source) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }

        try {
            DataComponentMap.Builder components = DataComponentMap.builder();
            components.addAll(source.getComponents().filter(type -> type != DataComponents.DAMAGE
                    && type != DataComponents.REPAIR_COST
                    && type != DataComponents.ATTRIBUTE_MODIFIERS
                    && type != DataComponents.CUSTOM_DATA));

            ItemAttributeModifiers modifiers = normalizedAttributeModifiers(source);
            if (modifiers != null) {
                components.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
            }

            CustomData customData = normalizedCustomData(source);
            if (customData != null && !customData.isEmpty()) {
                components.set(DataComponents.CUSTOM_DATA, customData);
            }

            return DataComponentMap.CODEC.encodeStart(
                            client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                            components.build()
                    )
                    .resultOrPartial(message -> LOGGER.warn("Unable to encode protected item signature: {}", message))
                    .orElse(null);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to create protected item signature", exception);
            return null;
        }
    }

    private static ItemAttributeModifiers normalizedAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null || modifiers.modifiers().size() < 2) {
            return modifiers;
        }

        List<ItemAttributeModifiers.Entry> sorted = new ArrayList<>(modifiers.modifiers());
        sorted.sort(Comparator
                .comparing(ItemProtectionManager::attributeId)
                .thenComparing(entry -> entry.modifier().id().toString())
                .thenComparingDouble(entry -> entry.modifier().amount())
                .thenComparing(entry -> entry.modifier().operation().name())
                .thenComparing(entry -> entry.slot().getSerializedName())
                .thenComparing(entry -> entry.display().toString()));
        return new ItemAttributeModifiers(List.copyOf(sorted));
    }

    private static String attributeId(ItemAttributeModifiers.Entry entry) {
        Holder<Attribute> attribute = entry.attribute();
        return attribute.unwrapKey()
                .map(ResourceKey::identifier)
                .map(Object::toString)
                .orElseGet(() -> attribute.value().toString());
    }

    private static CustomData normalizedCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return customData;
        }

        CompoundTag normalized = customData.copyTag();
        removeUuidFields(normalized);
        return normalized.isEmpty() ? null : CustomData.of(normalized);
    }

    private static void removeUuidFields(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            for (String key : List.copyOf(compound.keySet())) {
                if (key.equalsIgnoreCase("uuid")) {
                    compound.remove(key);
                    continue;
                }
                Tag child = compound.get(key);
                if (child != null) {
                    removeUuidFields(child);
                }
            }
        } else if (tag instanceof ListTag list) {
            for (Tag child : list) {
                removeUuidFields(child);
            }
        }
    }

    private static void rebuildIndex() {
        Set<String> blockItemIds = new HashSet<>();
        Map<String, Set<JsonElement>> componentSignatures = new HashMap<>();

        for (ModConfig.ItemProtectionRule rule : ModConfig.getInstance().itemProtection.rules) {
            if (rule == null || rule.itemId == null || rule.matchType == null) {
                continue;
            }
            if (BLOCK_ITEM_MATCH_TYPE.equals(rule.matchType)) {
                blockItemIds.add(rule.itemId);
            } else if (COMPONENTS_MATCH_TYPE.equals(rule.matchType) && rule.signature != null) {
                componentSignatures.computeIfAbsent(rule.itemId, ignored -> new HashSet<>()).add(rule.signature);
            }
        }

        Map<String, Set<JsonElement>> immutableSignatures = new HashMap<>();
        componentSignatures.forEach((itemId, signatures) -> immutableSignatures.put(itemId, Set.copyOf(signatures)));
        ruleIndex = new RuleIndex(Set.copyOf(blockItemIds), Map.copyOf(immutableSignatures));
        SIGNATURE_CACHE.clear();
    }

    public enum ChangeResult {
        CHANGED,
        ALREADY_PRESENT,
        NOT_PRESENT,
        NO_ITEM,
        SIGNATURE_FAILED
    }

    private record RuleIndex(Set<String> blockItemIds, Map<String, Set<JsonElement>> componentSignatures) {
        private static final RuleIndex EMPTY = new RuleIndex(Set.of(), Map.of());
    }

    private record CachedSignature(int sourceHash, JsonElement signature) {
    }
}
