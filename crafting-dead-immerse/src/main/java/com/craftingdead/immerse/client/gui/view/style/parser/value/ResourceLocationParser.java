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

package com.craftingdead.immerse.client.gui.view.style.parser.value;

import net.minecraft.resources.ResourceLocation;

public class ResourceLocationParser implements ValueParser<ResourceLocation> {

  public static final ResourceLocationParser INSTANCE = new ResourceLocationParser();

  private ResourceLocationParser() {}

  @Override
  public int validate(String style) {
    return style.substring(0, style.indexOf(')') + 1).length();
  }

  @Override
  public ResourceLocation parse(String style) {
    var pathWithNamespace = style.split("\\(");
    var namespace = pathWithNamespace[0].trim();
    var path = pathWithNamespace[1].replace(")", "").replace("\"", "");
    return new ResourceLocation(namespace, path);
  }
}