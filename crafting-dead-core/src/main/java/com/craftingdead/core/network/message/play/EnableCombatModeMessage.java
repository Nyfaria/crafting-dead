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

package com.craftingdead.core.network.message.play;

import java.util.function.Supplier;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class EnableCombatModeMessage {

  private final boolean enabled;

  public EnableCombatModeMessage(boolean enabled) {
    this.enabled = enabled;
  }

  public void encode(FriendlyByteBuf out) {
    out.writeBoolean(this.enabled);
  }

  public static EnableCombatModeMessage decode(FriendlyByteBuf in) {
    return new EnableCombatModeMessage(in.readBoolean());
  }

  public boolean handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(
        () -> PlayerExtension.getOrThrow(ctx.get().getSender()).setCombatModeEnabled(this.enabled));
    return true;
  }
}
