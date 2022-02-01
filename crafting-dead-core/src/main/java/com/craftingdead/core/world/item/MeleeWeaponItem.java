/*
 * Crafting Dead
 * Copyright (C) 2021  NexusNode LTD
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.craftingdead.core.world.item;

import javax.annotation.Nullable;
import com.craftingdead.core.capability.CapabilityUtil;
import com.craftingdead.core.world.item.combatslot.CombatSlot;
import com.craftingdead.core.world.item.combatslot.CombatSlotProvider;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class MeleeWeaponItem extends ToolItem {

  private final int attackDamage;
  private final double attackSpeed;

  private final Multimap<Attribute, AttributeModifier> attributeModifiers;

  public MeleeWeaponItem(int attackDamage, double attackSpeed, Item.Properties properties) {
    super(properties);
    this.attackSpeed = attackSpeed;
    this.attackDamage = attackDamage;

    Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
    builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
        "Weapon modifier", this.attackDamage, AttributeModifier.Operation.ADDITION));
    builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
        "Weapon modifier", this.attackSpeed, AttributeModifier.Operation.ADDITION));
    this.attributeModifiers = builder.build();
  }

  @Override
  public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
      EquipmentSlot equipmentSlot, ItemStack itemStack) {
    return equipmentSlot == EquipmentSlot.MAINHAND ? this.attributeModifiers
        : super.getAttributeModifiers(equipmentSlot, itemStack);
  }

  @Override
  public ICapabilityProvider initCapabilities(ItemStack itemStack, @Nullable CompoundTag nbt) {
    return CapabilityUtil.provider(() -> CombatSlot.MELEE, CombatSlotProvider.CAPABILITY);
  }
}
