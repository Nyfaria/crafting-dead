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

package com.craftingdead.core.world.entity.extension;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import com.craftingdead.core.event.LivingExtensionEvent;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.CancelActionMessage;
import com.craftingdead.core.network.message.play.CrouchMessage;
import com.craftingdead.core.network.message.play.PerformActionMessage;
import com.craftingdead.core.sounds.ModSoundEvents;
import com.craftingdead.core.world.action.Action;
import com.craftingdead.core.world.inventory.ModEquipmentSlot;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.clothing.Clothing;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.hat.Hat;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;

class LivingExtensionImpl<E extends LivingEntity, H extends LivingHandler>
    implements LivingExtension<E, H> {

  /**
   * The vanilla entity.
   */
  private final E entity;

  protected final Map<ResourceLocation, H> handlers = new Object2ObjectArrayMap<>();

  protected final Map<ResourceLocation, H> dirtyHandlers = new Object2ObjectArrayMap<>();

  private final IntSet dirtySlots = new IntOpenHashSet();

  private final EntitySnapshot[] snapshots = new EntitySnapshot[20];

  private final ItemStackHandler itemHandler =
      new ItemStackHandler(ModEquipmentSlot.values().length) {
        @Override
        protected void onLoad() {
          if (this.getSlots() != ModEquipmentSlot.values().length) {
            this.setSize(ModEquipmentSlot.values().length);
          }
        }

        @Override
        public void onContentsChanged(int slot) {
          if (!LivingExtensionImpl.this.entity.getCommandSenderWorld().isClientSide()) {
            LivingExtensionImpl.this.dirtySlots.add(slot);
          }
        }
      };

  /**
   * The last held {@link ItemStack} - used to check if the entity has switched item.
   */
  protected ItemStack lastHeldStack = null;

  @Nullable
  private Action action;

  @Nullable
  private ProgressMonitor progressMonitor;

  private boolean movementBlocked;

  private boolean crouching;

  private Vec3 lastPos;

  private boolean moving;

  private Visibility cachedVisibility = Visibility.VISIBLE;

  private ItemStack lastClothingStack = ItemStack.EMPTY;

  LivingExtensionImpl(E entity) {
    this.entity = entity;
  }

  @Override
  public void load() {
    MinecraftForge.EVENT_BUS.post(new LivingExtensionEvent.Load(this));
  }

  @Override
  public void registerHandler(ResourceLocation id, H extension) {
    if (this.handlers.containsKey(id)) {
      throw new IllegalArgumentException(
          "Handler with id " + id.toString() + " already registered");
    }
    this.handlers.put(id, extension);
  }

  @Override
  public Optional<H> getHandler(ResourceLocation id) {
    return Optional.ofNullable(this.handlers.get(id));
  }

  @Override
  public H getHandlerOrThrow(ResourceLocation id) {
    H handler = this.handlers.get(id);
    if (handler == null) {
      throw new IllegalStateException("Missing handler with ID: " + id.toString());
    }
    return handler;
  }

  @Override
  public Optional<Action> getAction() {
    return Optional.ofNullable(this.action);
  }

  @Override
  public boolean performAction(Action action, boolean force, boolean sendUpdate) {
    if (MinecraftForge.EVENT_BUS.post(new LivingExtensionEvent.PerformAction<>(this, action))) {
      return false;
    }

    final var targetProgressMonitor = action.getTarget()
        .flatMap(LivingExtension::getProgressMonitor)
        .orElse(null);

    if (this.progressMonitor != null || targetProgressMonitor != null) {
      if (!force) {
        return false;
      }
      this.progressMonitor.stop();
      if (targetProgressMonitor != this.progressMonitor) {
        targetProgressMonitor.stop();
      }
    }

    if ((this.action != null && !force) || !action.start()) {
      return false;
    }

    this.cancelAction(true);
    this.action = action;
    this.progressMonitor = action.getPerformerProgress();
    action.getTarget().ifPresent(target -> target.setProgressMonitor(action.getTargetProgress()));
    if (sendUpdate) {
      var target = this.getLevel().isClientSide()
          ? PacketDistributor.SERVER.noArg()
          : PacketDistributor.TRACKING_ENTITY_AND_SELF.with(this::getEntity);
      int targetId = action.getTarget()
          .map(LivingExtension::getEntity)
          .map(Entity::getId)
          .orElse(-1);
      NetworkChannel.PLAY.getSimpleChannel().send(target,
          new PerformActionMessage(action.getType(), this.getEntity().getId(), targetId));
    }
    return true;
  }

  @Override
  public void cancelAction(boolean sendUpdate) {
    if (this.action == null) {
      return;
    }
    this.action.cancel();
    this.removeAction();
    if (sendUpdate) {
      PacketTarget target = this.getLevel().isClientSide()
          ? PacketDistributor.SERVER.noArg()
          : PacketDistributor.TRACKING_ENTITY_AND_SELF.with(this::getEntity);
      NetworkChannel.PLAY.getSimpleChannel().send(target,
          new CancelActionMessage(this.getEntity().getId()));
    }
  }

  @Override
  public void setProgressMonitor(ProgressMonitor actionProgress) {
    this.progressMonitor = actionProgress;
  }

  @Override
  public Optional<ProgressMonitor> getProgressMonitor() {
    return Optional.ofNullable(this.progressMonitor);
  }

  private void removeAction() {
    if (this.action != null) {
      this.progressMonitor = null;
      this.action.getTarget().ifPresent(target -> target.setProgressMonitor(null));
      this.action = null;
    }
  }

  @Override
  public void setMovementBlocked(boolean movementBlocked) {
    this.movementBlocked = movementBlocked;
  }

  @Override
  public boolean isMovementBlocked() {
    return this.movementBlocked;
  }

  @Override
  public boolean isMoving() {
    return this.moving;
  }

  @Override
  public void tick() {
    ItemStack heldStack = this.getMainHandItem();
    if (heldStack != this.lastHeldStack) {
      this.getProgressMonitor().ifPresent(ProgressMonitor::stop);
      if (this.lastHeldStack != null) {
        this.lastHeldStack.getCapability(Gun.CAPABILITY)
            .ifPresent(gun -> gun.reset(this));
      }
      if (heldStack.getCapability(Gun.CAPABILITY).isPresent()) {
        this.entity.playSound(ModSoundEvents.GUN_EQUIP.get(), 0.25F, 1.0F);
      }
      this.lastHeldStack = heldStack;
    }

    // Reset this every tick
    this.movementBlocked = false;

    if (this.action != null && this.action.tick()) {
      this.removeAction();
    }

    heldStack.getCapability(Gun.CAPABILITY).ifPresent(gun -> gun.tick(this));

    this.updateClothing();
    this.updateHat();

    if (!this.entity.getCommandSenderWorld().isClientSide()) {
      // This is called at the start of the entity tick so it's equivalent of last tick's position.
      this.snapshots[this.entity.getServer().getTickCount() % 20] =
          new EntitySnapshot(this.entity);
    }

    this.moving = !this.entity.position().equals(this.lastPos);
    this.lastPos = this.entity.position();

    for (var entry : this.handlers.entrySet()) {
      this.tickHandler(entry.getKey(), entry.getValue());
    }
  }

  protected void tickHandler(ResourceLocation handlerId, H handler) {
    handler.tick();

    // Precedence = (1) INVISIBLE (2) PARTIALLY_VISIBLE (3) VISIBLE
    this.cachedVisibility = Visibility.VISIBLE;
    switch (handler.getVisibility()) {
      case INVISIBLE:
        this.cachedVisibility = Visibility.INVISIBLE;
      case PARTIALLY_VISIBLE:
        if (this.cachedVisibility == Visibility.VISIBLE) {
          this.cachedVisibility = Visibility.PARTIALLY_VISIBLE;
        }
        break;
      default:
        break;
    }

    if (handler.isMovementBlocked()) {
      this.movementBlocked = true;
    }

    if (handler.requiresSync()) {
      this.dirtyHandlers.put(handlerId, handler);
    }
  }

  private void updateHat() {
    var headStack = this.itemHandler.getStackInSlot(ModEquipmentSlot.HAT.getIndex());
    var hat = headStack.getCapability(Hat.CAPABILITY).orElse(null);
    if (headStack.getItem() == ModItems.SCUBA_MASK.get()
        && this.entity.isEyeInFluid(FluidTags.WATER)) {
      this.entity.addEffect(
          new MobEffectInstance(MobEffects.WATER_BREATHING, 2, 0, false, false, false));
    } else if (hat != null && hat.hasNightVision()) {
      this.entity.addEffect(
          new MobEffectInstance(MobEffects.NIGHT_VISION, 2, 0, false, false, false));
    }
  }

  private void updateClothing() {
    var clothingStack = this.itemHandler.getStackInSlot(ModEquipmentSlot.CLOTHING.getIndex());
    var clothing = clothingStack.getCapability(Clothing.CAPABILITY).orElse(null);

    if (clothingStack != this.lastClothingStack) {
      this.lastClothingStack.getCapability(Clothing.CAPABILITY)
          .map(Clothing::getAttributeModifiers)
          .ifPresent(this.entity.getAttributes()::removeAttributeModifiers);
      if (clothing != null) {
        this.entity.getAttributes().addTransientAttributeModifiers(
            clothing.getAttributeModifiers());
      }
    }

    if (clothing != null) {
      // Fire immunity
      if (clothing.hasFireImmunity()) {
        if (this.entity.getRemainingFireTicks() > 0) {
          this.entity.clearFire();
        }

        this.entity
            .addEffect(
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, 0, false, false, false));
      }
    }

    if (clothingStack.getItem() == ModItems.SCUBA_CLOTHING.get()
        && this.entity.isEyeInFluid(FluidTags.WATER)) {
      this.entity.addEffect(
          new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 2, 0, false, false, false));
    }

    this.lastClothingStack = clothingStack;
  }


  @Override
  public float handleDamaged(DamageSource source, float amount) {
    return this.handlers.values().stream().reduce(amount,
        (result, extension) -> extension.handleDamaged(source, result), (u, t) -> t);
  }

  @Override
  public boolean handleHurt(DamageSource source, float amount) {
    return this.handlers.values().stream().anyMatch(e -> e.handleHurt(source, amount));
  }

  @Override
  public boolean handleKill(Entity target) {
    return this.handlers.values().stream().anyMatch(e -> e.handleKill(target));
  }

  @Override
  public boolean handleDeath(DamageSource cause) {
    return this.handlers.values().stream().anyMatch(e -> e.handleDeath(cause));
  }

  @Override
  public boolean handleDeathLoot(DamageSource cause, Collection<ItemEntity> drops) {
    if (this.handlers.values().stream()
        .filter(e -> e.handleDeathLoot(cause, drops))
        .findAny()
        .isPresent()) {
      return true;
    }

    if (!this.keepInventory()) {
      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
        var itemStack = this.itemHandler.extractItem(i, Integer.MAX_VALUE, false);
        if (!itemStack.isEmpty()) {
          var itemEntity = new ItemEntity(this.getLevel(), this.getEntity().getX(),
              this.getEntity().getY(), this.getEntity().getZ(), itemStack);
          itemEntity.setDefaultPickUpDelay();
          drops.add(itemEntity);
        }
      }
    }
    return false;
  }

  protected boolean keepInventory() {
    return false;
  }

  @Override
  public Visibility getVisibility() {
    return this.cachedVisibility;
  }

  @Override
  public IItemHandlerModifiable getItemHandler() {
    return this.itemHandler;
  }

  @Override
  public EntitySnapshot getSnapshot(int tick) {
    final int currentTick = this.entity.getServer().getTickCount();
    if (tick >= currentTick) {
      return new EntitySnapshot(this.entity);
    } else if (tick < currentTick - 20) {
      return this.snapshots[0];
    }

    final int snapshotIndex = tick % 20;
    var snapshot = this.snapshots[snapshotIndex];
    if (snapshot == null) {
      throw new IndexOutOfBoundsException();
    }
    return snapshot;
  }

  @Override
  public boolean isCrouching() {
    return this.crouching;
  }

  @Override
  public void setCrouching(boolean crouching, boolean sendUpdate) {
    if (!this.entity.isOnGround()) {
      return;
    }
    this.crouching = crouching;
    if (sendUpdate) {
      var target = this.getLevel().isClientSide()
          ? PacketDistributor.SERVER.noArg()
          : PacketDistributor.TRACKING_ENTITY_AND_SELF.with(this::getEntity);
      NetworkChannel.PLAY.getSimpleChannel().send(target,
          new CrouchMessage(this.getEntity().getId(), crouching));
    }
  }

  @Override
  public E getEntity() {
    return this.entity;
  }

  @Override
  public CompoundTag serializeNBT() {
    var tag = new CompoundTag();
    tag.put("inventory", this.itemHandler.serializeNBT());
    for (var entry : this.handlers.entrySet()) {
      var extensionTag = entry.getValue().serializeNBT();
      if (!extensionTag.isEmpty()) {
        tag.put(entry.getKey().toString(), extensionTag);
      }
    }
    return tag;
  }

  @Override
  public void deserializeNBT(CompoundTag tag) {
    this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
    for (var entry : this.handlers.entrySet()) {
      var extensionTag = tag.getCompound(entry.getKey().toString());
      if (!extensionTag.isEmpty()) {
        entry.getValue().deserializeNBT(extensionTag);
      }
    }
  }

  @Override
  public int hashCode() {
    return this.entity.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj)
        || (obj instanceof LivingExtension<?, ?> extension
            && extension.getEntity().equals(this.entity));
  }

  @Override
  public void encode(FriendlyByteBuf out, boolean writeAll) {
    // Item Handler
    if (writeAll) {
      for (int i = 0; i < this.itemHandler.getSlots(); i++) {
        out.writeShort(i);
        out.writeItem(this.itemHandler.getStackInSlot(i));
      }
    } else {
      this.dirtySlots.forEach(slot -> {
        out.writeShort(slot);
        out.writeItem(this.itemHandler.getStackInSlot(slot));
      });
      this.dirtySlots.clear();
    }
    out.writeShort(255);

    // Handlers
    var handlersToSend = writeAll ? this.handlers.entrySet() : this.dirtyHandlers.entrySet();
    out.writeVarInt(handlersToSend.size());
    for (var entry : handlersToSend) {
      out.writeResourceLocation(entry.getKey());
      var handlerData = new FriendlyByteBuf(Unpooled.buffer());
      entry.getValue().encode(handlerData, writeAll);
      out.writeVarInt(handlerData.readableBytes());
      out.writeBytes(handlerData);
    }
    this.dirtyHandlers.clear();
  }

  @Override
  public void decode(FriendlyByteBuf in) {
    // Item Handler
    int slot;
    while ((slot = in.readShort()) != 255) {
      this.itemHandler.setStackInSlot(slot, in.readItem());
    }

    // Handlers
    int handlersSize = in.readVarInt();
    for (int x = 0; x < handlersSize; x++) {
      var id = in.readResourceLocation();
      int dataSize = in.readVarInt();
      H handler = this.handlers.get(id);
      if (handler == null) {
        in.readerIndex(in.readerIndex() + dataSize);
        continue;
      }
      handler.decode(in);
    }
  }

  @Override
  public boolean requiresSync() {
    return !this.dirtySlots.isEmpty() || !this.dirtyHandlers.isEmpty();
  }
}
