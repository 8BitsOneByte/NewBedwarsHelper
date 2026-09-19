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

import java.math.BigDecimal;
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
        removeDuplicateConfiguredRules();
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

        Set<String> signatures = ruleIndex.componentSignatures().get(itemId);
        if (signatures == null || signatures.isEmpty()) {
            return false;
        }

        String signatureKey = signatureKey(stack);
        return signatureKey != null && signatures.contains(signatureKey);
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
        String firstKey = ruleKey(first);
        return firstKey != null && firstKey.equals(ruleKey(second));
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static JsonElement signature(ItemStack stack) {
        CachedSignature cached = cachedSignature(stack);
        return cached == null ? null : cached.signature();
    }

    private static String signatureKey(ItemStack stack) {
        CachedSignature cached = cachedSignature(stack);
        return cached == null ? null : cached.canonicalKey();
    }

    private static CachedSignature cachedSignature(ItemStack stack) {
        int sourceHash = ItemStack.hashItemAndComponents(stack);
        CachedSignature cached = SIGNATURE_CACHE.get(stack);
        if (cached != null && cached.sourceHash() == sourceHash) {
            return cached;
        }

        JsonElement signature = createSignature(stack);
        if (signature == null) {
            return null;
        }

        CachedSignature created = new CachedSignature(sourceHash, signature, canonicalSignature(signature));
        SIGNATURE_CACHE.put(stack, created);
        return created;
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

    private static void removeDuplicateConfiguredRules() {
        ModConfig config = ModConfig.getInstance();
        Set<String> seenRules = new HashSet<>();
        boolean removed = config.itemProtection.rules.removeIf(rule -> {
            String key = ruleKey(rule);
            return key != null && !seenRules.add(key);
        });
        if (removed) {
            config.save();
        }
    }

    private static String ruleKey(ModConfig.ItemProtectionRule rule) {
        if (rule == null || rule.matchType == null || rule.itemId == null) {
            return null;
        }
        if (BLOCK_ITEM_MATCH_TYPE.equals(rule.matchType)) {
            return BLOCK_ITEM_MATCH_TYPE + '\0' + rule.itemId;
        }
        if (COMPONENTS_MATCH_TYPE.equals(rule.matchType) && rule.signature != null) {
            return COMPONENTS_MATCH_TYPE + '\0' + rule.itemId + '\0' + canonicalSignature(rule.signature);
        }
        return null;
    }

    private static String canonicalSignature(JsonElement signature) {
        StringBuilder builder = new StringBuilder();
        appendCanonicalJson(signature, builder);
        return builder.toString();
    }

    private static void appendCanonicalJson(JsonElement element, StringBuilder builder) {
        if (element == null || element.isJsonNull()) {
            builder.append('n');
            return;
        }
        if (element.isJsonArray()) {
            builder.append('[');
            for (JsonElement child : element.getAsJsonArray()) {
                appendCanonicalJson(child, builder);
            }
            builder.append(']');
            return;
        }
        if (element.isJsonObject()) {
            builder.append('{');
            element.getAsJsonObject().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        appendCanonicalString(entry.getKey(), builder);
                        appendCanonicalJson(entry.getValue(), builder);
                    });
            builder.append('}');
            return;
        }

        if (element.getAsJsonPrimitive().isString()) {
            builder.append('s');
            appendCanonicalString(element.getAsString(), builder);
        } else if (element.getAsJsonPrimitive().isBoolean()) {
            builder.append(element.getAsBoolean() ? "bt" : "bf");
        } else {
            builder.append('d').append(canonicalNumber(element.getAsString())).append(';');
        }
    }

    private static void appendCanonicalString(String value, StringBuilder builder) {
        builder.append(value.length()).append(':').append(value);
    }

    private static String canonicalNumber(String value) {
        try {
            BigDecimal number = new BigDecimal(value).stripTrailingZeros();
            return number.signum() == 0 ? "0" : number.toString();
        } catch (NumberFormatException exception) {
            return value;
        }
    }

    private static void rebuildIndex() {
        Set<String> blockItemIds = new HashSet<>();
        Map<String, Set<String>> componentSignatures = new HashMap<>();

        for (ModConfig.ItemProtectionRule rule : ModConfig.getInstance().itemProtection.rules) {
            if (rule == null || rule.itemId == null || rule.matchType == null) {
                continue;
            }
            if (BLOCK_ITEM_MATCH_TYPE.equals(rule.matchType)) {
                blockItemIds.add(rule.itemId);
            } else if (COMPONENTS_MATCH_TYPE.equals(rule.matchType) && rule.signature != null) {
                componentSignatures.computeIfAbsent(rule.itemId, ignored -> new HashSet<>())
                        .add(canonicalSignature(rule.signature));
            }
        }

        Map<String, Set<String>> immutableSignatures = new HashMap<>();
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

    private record RuleIndex(Set<String> blockItemIds, Map<String, Set<String>> componentSignatures) {
        private static final RuleIndex EMPTY = new RuleIndex(Set.of(), Map.of());
    }

    private record CachedSignature(int sourceHash, JsonElement signature, String canonicalKey) {
    }
}
