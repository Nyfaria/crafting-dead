/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.survival.world.level.block;

import java.util.function.Supplier;
import com.craftingdead.survival.CraftingDeadSurvival;
import com.craftingdead.survival.particles.SurvivalParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SurvivalBlocks {

  public static final DeferredRegister<Block> deferredRegister =
      DeferredRegister.create(ForgeRegistries.BLOCKS, CraftingDeadSurvival.ID);

  public static final RegistryObject<Block> MILITARY_LOOT =
      deferredRegister.register("military_loot",
          () -> new LootBlock(BlockBehaviour.Properties.of(Material.STONE)
              .strength(5.0F, 5.0F)
              .noOcclusion()));

  public static final RegistryObject<Block> MEDICAL_LOOT =
      deferredRegister.register("medic_loot",
          () -> new LootBlock(BlockBehaviour.Properties.of(Material.STONE)
              .strength(5.0F, 5.0F)
              .noOcclusion()));

  public static final RegistryObject<Block> CIVILIAN_LOOT =
      deferredRegister.register("civilian_loot",
          () -> new LootBlock(BlockBehaviour.Properties.of(Material.STONE)
              .strength(5.0F, 5.0F)
              .noOcclusion()));

  public static final RegistryObject<Block> RARE_CIVILIAN_LOOT =
      deferredRegister.register("civilian_rare_loot",
          () -> new LootBlock(BlockBehaviour.Properties.of(Material.STONE)
              .strength(5.0F, 5.0F)
              .noOcclusion()));

  public static final RegistryObject<Block> POLICE_LOOT =
      deferredRegister.register("police_loot",
          () -> new LootBlock(BlockBehaviour.Properties.of(Material.STONE)
              .strength(5.0F, 5.0F)
              .noOcclusion()));

  public static final RegistryObject<Block> MILITARY_LOOT_GENERATOR =
      deferredRegister.register("military_loot_gen",
          () -> lootGenerator(MILITARY_LOOT, SurvivalParticleTypes.MILITARY_LOOT_GEN,
              CraftingDeadSurvival.serverConfig.militaryLootRefreshDelayTicks,
              CraftingDeadSurvival.serverConfig.militaryLootEnabled));

  public static final RegistryObject<Block> MEDICAL_LOOT_GENERATOR =
      deferredRegister.register("medic_loot_gen",
          () -> lootGenerator(MEDICAL_LOOT, SurvivalParticleTypes.MEDIC_LOOT_GEN,
              CraftingDeadSurvival.serverConfig.medicalLootRefreshDelayTicks,
              CraftingDeadSurvival.serverConfig.medicalLootEnabled));

  public static final RegistryObject<Block> CIVILIAN_LOOT_GENERATOR =
      deferredRegister.register("civilian_loot_gen",
          () -> lootGenerator(CIVILIAN_LOOT, SurvivalParticleTypes.CIVILIAN_LOOT_GEN,
              CraftingDeadSurvival.serverConfig.civilianLootRefreshDelayTicks,
              CraftingDeadSurvival.serverConfig.civilianLootEnabled));

  public static final RegistryObject<Block> RARE_CIVILIAN_LOOT_GENERATOR =
      deferredRegister.register("civilian_rare_loot_gen",
          () -> lootGenerator(RARE_CIVILIAN_LOOT,
              SurvivalParticleTypes.CIVILIAN_RARE_LOOT_GEN,
              CraftingDeadSurvival.serverConfig.rareCivilianLootRefreshDelayTicks,
              CraftingDeadSurvival.serverConfig.rareCivilianLootEnabled));

  public static final RegistryObject<Block> POLICE_LOOT_GENERATOR =
      deferredRegister.register("police_loot_gen",
          () -> lootGenerator(POLICE_LOOT, SurvivalParticleTypes.POLICE_LOOT_GEN,
              CraftingDeadSurvival.serverConfig.policeLootRefreshDelayTicks,
              CraftingDeadSurvival.serverConfig.policeLootEnabled));

  private static LootGeneratorBlock lootGenerator(Supplier<Block> lootBlock,
      Supplier<? extends ParticleOptions> particleOptions, Supplier<Integer> refreshDelayTicks,
      Supplier<Boolean> enabled) {
    return new LootGeneratorBlock(
        BlockBehaviour.Properties.of(Material.STRUCTURAL_AIR)
            .strength(5.0F, 5.0F)
            .randomTicks()
            .noOcclusion()
            .noCollission(),
        lootBlock, particleOptions, refreshDelayTicks, enabled);
  }
}
