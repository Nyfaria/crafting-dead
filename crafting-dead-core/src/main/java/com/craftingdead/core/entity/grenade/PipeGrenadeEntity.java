package com.craftingdead.core.entity.grenade;

import com.craftingdead.core.entity.ModEntityTypes;
import com.craftingdead.core.item.GrenadeItem;
import com.craftingdead.core.item.ModItems;
import com.craftingdead.core.particle.RGBFlashParticleData;
import com.craftingdead.core.util.ModDamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class PipeGrenadeEntity extends GrenadeEntity {

  private static final RGBFlashParticleData RED_FLASH =
      new RGBFlashParticleData(1F, 0.35F, 0.35F, 1F);

  public PipeGrenadeEntity(EntityType<? extends GrenadeEntity> entityIn, World worldIn) {
    super(entityIn, worldIn);
  }

  public PipeGrenadeEntity(LivingEntity thrower, World worldIn) {
    super(ModEntityTypes.pipeGrenade, thrower, worldIn);
  }

  @Override
  public Integer getMinimumTicksUntilAutoActivation() {
    return 100;
  }

  @Override
  public void onActivationStateChange(boolean activated) {
    if (activated) {
      if (!this.world.isRemote()) {
        this.remove();
        this.world.createExplosion(this,
            ModDamageSource.causeUnscaledExplosionDamage(this.getThrower().orElse(null)),
            this.getPosX(), this.getPosY() + this.getHeight(), this.getPosZ(), 4F, false,
            Explosion.Mode.NONE);
      }
    }
  }

  @Override
  public void onGrenadeTick() {
    if (this.ticksExisted % 6 == 0) {
      if (this.world.isRemote()) {
        this.world.addParticle(RED_FLASH, true,
            this.getPosX(), this.getPosY(), this.getPosZ(), 0D, 0D, 0D);
      } else {
        float pitchProgress =
            this.ticksExisted / (float) (this.getMinimumTicksUntilAutoActivation());
        float gradualPitch = MathHelper.lerp(pitchProgress, 1.0F, 2F);
        this.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL, 1.7F, gradualPitch);
      }
    }
  }

  @Override
  public boolean isAttracting() {
    return true;
  }

  @Override
  public GrenadeItem asItem() {
    return ModItems.PIPE_GRENADE.get();
  }

  @Override
  public void onMotionStop(int stopsCount) {}
}