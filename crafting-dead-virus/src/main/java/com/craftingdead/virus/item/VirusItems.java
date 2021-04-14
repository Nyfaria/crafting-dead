package com.craftingdead.virus.item;

import com.craftingdead.core.item.ActionItem;
import com.craftingdead.virus.CraftingDeadVirus;
import com.craftingdead.virus.action.VirusActionTypes;
import com.craftingdead.virus.entity.ModEntityTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class VirusItems {

  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, CraftingDeadVirus.ID);

  public static final ItemGroup TAB = new ItemGroup("craftingdeadvirus") {

    @Override
    public ItemStack makeIcon() {
      return new ItemStack(RBI_SYRINGE::get);
    }
  };

  // ================================================================================
  // Virus
  // ================================================================================

  public static final RegistryObject<Item> RBI_SYRINGE = ITEMS.register("rbi_syringe",
      () -> new ActionItem((ActionItem.Properties) new ActionItem.Properties()
          .setAction(VirusActionTypes.USE_RBI_SYRINGE)
          .stacksTo(1)
          .tab(TAB)));

  public static final RegistryObject<Item> CURE_SYRINGE = ITEMS.register("cure_syringe",
      () -> new ActionItem((ActionItem.Properties) new ActionItem.Properties()
          .setAction(VirusActionTypes.USE_CURE_SYRINGE)
          .stacksTo(1)
          .tab(TAB)));

  // ================================================================================
  // Spawn Eggs
  // ================================================================================

  public static final RegistryObject<Item> ADVANCED_ZOMBIE_SPAWN_EGG =
      ITEMS.register("advanced_zombie_spawn_egg",
          () -> new SpawnEggItem(ModEntityTypes.advancedZombie, 0x000000, 0xFFFFFF,
              new Item.Properties().tab(TAB)));

  public static final RegistryObject<Item> FAST_ZOMBIE_SPAWN_EGG =
      ITEMS.register("fast_zombie_spawn_egg",
          () -> new SpawnEggItem(ModEntityTypes.fastZombie, 0x000000, 0xFFFFFF,
              new Item.Properties().tab(TAB)));

  public static final RegistryObject<Item> TANK_ZOMBIE_SPAWN_EGG =
      ITEMS.register("tank_zombie_spawn_egg",
          () -> new SpawnEggItem(ModEntityTypes.tankZombie, 0x000000, 0xFFFFFF,
              new Item.Properties().tab(TAB)));

  public static final RegistryObject<Item> WEAK_ZOMBIE_SPAWN_EGG =
      ITEMS.register("weak_zombie_spawn_egg",
          () -> new SpawnEggItem(ModEntityTypes.weakZombie, 0x000000, 0xFFFFFF,
              new Item.Properties().tab(TAB)));

  public static final RegistryObject<Item> POLICE_ZOMBIE_SPAWN_EGG =
      ITEMS.register("police_zombie_spawn_egg",
          () -> new SpawnEggItem(ModEntityTypes.policeZombie, 0x000000, 0xFFFFFF,
              new Item.Properties().tab(TAB)));

  public static final RegistryObject<Item> DOCTOR_ZOMBIE_SPAWN_EGG =
      ITEMS.register("doctor_zombie_spawn_egg",
          () -> new SpawnEggItem(ModEntityTypes.doctorZombie, 0x000000, 0xFFFFFF,
              new Item.Properties().tab(TAB)));
}
