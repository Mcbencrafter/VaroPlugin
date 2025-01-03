package de.cuuky.varo.list.enchantment.lists;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.bukkit.enchantments.Enchantment;

import de.cuuky.varo.list.enchantment.EnchantmentList;

public class BlockedEnchantments extends EnchantmentList {

    public BlockedEnchantments() {
        super("BlockedEnchantments");

        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);

            Class<?> enchantmentClass = Class.forName("net.minecraft.server.v1_8_R3.Enchantment");
            Field enchantmentArrayField = enchantmentClass.getDeclaredField("b");
            enchantmentArrayField.setAccessible(true);
            modifiersField.setInt(enchantmentArrayField, ~Modifier.FINAL & enchantmentArrayField.getModifiers());

            Object[] filteredEnchantmentArray = Arrays.stream(Enchantment.values()).filter(enchantment -> !this.isBlocked(enchantment)).map(enchantment -> {
                try {
                    Field targetField = enchantment.getClass().getDeclaredField("target");
                    targetField.setAccessible(true);

                    return targetField.get(enchantment);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }).toArray(size -> (Object[]) Array.newInstance(enchantmentClass, size));

            enchantmentArrayField.set(null, filteredEnchantmentArray);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void loadDefaultValues() {
        this.enchantments.add(Enchantment.DAMAGE_ARTHROPODS.getName());
    }

    public boolean isBlocked(Enchantment ench) {
        return enchantments.contains(ench.getName());
    }
}