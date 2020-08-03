package com.craftingdead.core.entity.grenade;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.capability.ModCapabilities;
import com.craftingdead.core.capability.hat.IHat;
import com.craftingdead.core.client.ClientDist;
import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.core.entity.ModEntityTypes;
import com.craftingdead.core.inventory.InventorySlotType;
import com.craftingdead.core.item.GrenadeItem;
import com.craftingdead.core.item.ModItems;
import com.craftingdead.core.particle.RGBFlashParticleData;
import com.craftingdead.core.potion.FlashBlindnessEffect;
import com.craftingdead.core.potion.ModEffects;
import com.craftingdead.core.util.EntityUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class FlashGrenadeEntity extends GrenadeEntity {

  public static final double FLASH_MAX_RANGE = 50D;
  public static final int EFFECT_MAX_DURATION = 110;

  public FlashGrenadeEntity(EntityType<? extends GrenadeEntity> entityIn, World worldIn) {
    super(entityIn, worldIn);
  }

  public FlashGrenadeEntity(LivingEntity thrower, World worldIn) {
    super(ModEntityTypes.flashGrenade, thrower, worldIn);
  }

  @Override
  public void onActivationStateChange(boolean activated) {
    if (activated) {
      this.flash();
    } else {
      if (!this.world.isRemote()) {
        this.remove();
      }
    }
  }

  @Override
  public Integer getMinimumTicksUntilAutoActivation() {
    return 30;
  }

  @Override
  public Integer getMinimumTicksUntilAutoDeactivation() {
    return 5;
  }

  @Override
  public void onGrenadeTick() {}

  @Override
  public void onMotionStop(int stopsCount) {}

  public void flash() {
    if (this.world.isRemote()) {
      this.world
          .addParticle(new RGBFlashParticleData(1F, 1F, 1F, 2F), this.getPosX(), this.getPosY(),
              this.getPosZ(), 0D, 0D, 0D);

      // Applies the flash effect at client side for a better delay compensation
      // and better FOV calculation

      ClientDist clientDist = (ClientDist) CraftingDead.getInstance().getModDist();
      clientDist.getPlayer().ifPresent(clientPlayer -> {
        int duration = this
            .calculateDuration(clientPlayer.getEntity(), RenderUtil.isInsideGameFOV(this, false));
        if (duration > 0) {
          EffectInstance flashEffect =
              new EffectInstance(ModEffects.FLASH_BLINDNESS.get(), duration);
          ModEffects.applyOrOverrideIfLonger(clientPlayer.getEntity(), flashEffect);
        }
      });
    } else {
      this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 1.2F);

      this.world
          .getEntitiesInAABBexcluding(this, this.getBoundingBox().grow(FLASH_MAX_RANGE),
              (entity) -> entity instanceof LivingEntity && !(entity instanceof PlayerEntity))
          .stream()
          .map(entity -> (LivingEntity) entity)
          .forEach(livingEntity -> {
            int duration = this
                .calculateDuration(livingEntity, EntityUtil.isInsideFOV(livingEntity, this, 90F));
            if (duration > 0) {
              boolean wasFlashApplied = ModEffects
                  .applyOrOverrideIfLonger(livingEntity,
                      new EffectInstance(ModEffects.FLASH_BLINDNESS.get(), duration));
              if (wasFlashApplied && livingEntity instanceof MobEntity) {
                MobEntity mobEntity = (MobEntity) livingEntity;
                // Removes the attack target
                mobEntity.setAttackTarget(null);
              }
            }
          });
    }
  }

  /**
   * Calculates the amount of ticks that must be used in a {@link FlashBlindnessEffect} in according
   * to the possible variables, like blocks in front of view, resistance from equipments and others.
   *
   * @return int - The amount in ticks. Zero if it should not be applied.
   */
  public int calculateDuration(LivingEntity viewerEntity, boolean insideFOV) {
    if (!viewerEntity.canEntityBeSeen(this)) {
      return 0;
    }

    ItemStack hatItemStack = viewerEntity
        .getCapability(ModCapabilities.LIVING)
        .map(living -> living.getItemHandler().getStackInSlot(InventorySlotType.HAT.getIndex()))
        .orElse(ItemStack.EMPTY);

    final boolean isImmuneToFlashes =
        hatItemStack.getCapability(ModCapabilities.HAT).map(IHat::isImmuneToFlashes).orElse(false);

    if (insideFOV && !isImmuneToFlashes) {
      double distanceProportion =
          MathHelper.clamp(this.getDistance(viewerEntity.getEntity()) / FLASH_MAX_RANGE, 0F, 1F);
      int calculatedDuration =
          (int) MathHelper.lerp(1F - distanceProportion, 0, EFFECT_MAX_DURATION);

      if (!(viewerEntity instanceof PlayerEntity)) {
        // Non-player entities has extra duration
        calculatedDuration *= 4;
      }

      return calculatedDuration;
    }

    // Put a minimum duration for players, so they can see
    // a cool and short flash effect behind them
    return (viewerEntity instanceof PlayerEntity) ? 5 : 0;
  }

  @Override
  public GrenadeItem asItem() {
    return ModItems.FLASH_GRENADE.get();
  }
}