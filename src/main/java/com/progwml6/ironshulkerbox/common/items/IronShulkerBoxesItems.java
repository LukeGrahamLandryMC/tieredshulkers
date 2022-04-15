package com.progwml6.ironshulkerbox.common.items;

import com.google.common.collect.ImmutableMap;
import com.progwml6.ironshulkerbox.IronShulkerBoxes;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class IronShulkerBoxesItems {

  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IronShulkerBoxes.MOD_ID);

  public static final ImmutableMap<BoxUpgradeType, RegistryObject<BoxUpgradeItem>> UPGRADES = ImmutableMap.copyOf(Arrays.stream(BoxUpgradeType.values())
    .collect(Collectors.toMap(Function.identity(), type -> register(type.name().toLowerCase(Locale.ROOT) + "_shulker_box_upgrade",
      () -> new BoxUpgradeItem(type, new Item.Properties().tab(IronShulkerBoxes.IRONSHULKERBOX_ITEM_GROUP).stacksTo(1))))));

  private static <T extends Item> net.minecraftforge.registries.RegistryObject<T> register(final String name, final Supplier<T> sup) {
    return ITEMS.register(name, sup);
  }
}
