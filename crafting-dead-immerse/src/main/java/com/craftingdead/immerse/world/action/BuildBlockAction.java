package com.craftingdead.immerse.world.action;

import javax.annotation.Nullable;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

public class BuildBlockAction extends BlueprintAction {

  private final BuildBlockActionType type;

  @Nullable
  private BlockState blockState;

  protected BuildBlockAction(LivingExtension<?, ?> performer, BlockPlaceContext context,
      BuildBlockActionType type) {
    super(performer, context);
    this.type = type;
    this.blockState = type.getBlock().getStateForPlacement(context);
  }

  @Override
  public boolean start(boolean simulate) {
    return this.blockState != null
        && super.start(simulate)
        && this.canPlace(this.getContext().getClickedPos());
  }

  @Override
  public boolean tick() {
    if (super.tick()) {
      return true;
    }

    if (this.getTicksUsingItem() % 10 == 0) {
      this.getPerformer().getEntity().playSound(SoundEvents.WOOD_BREAK, 1.0F, 1.0F);
      this.addBuildEffects(this.getContext().getClickedPos(), this.blockState);
    }

    return false;
  }

  @Override
  public void stop(StopReason reason) {
    super.stop(reason);
    if (!reason.isCompleted()) {
      return;
    }
    this.placeBlock(this.getContext().getClickedPos(), this.blockState);
  }

  @Override
  public BuildBlockActionType getType() {
    return this.type;
  }
}