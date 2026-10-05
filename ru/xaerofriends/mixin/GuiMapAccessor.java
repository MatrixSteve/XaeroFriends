/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  xaero.map.element.HoveredMapElementHolder
 *  xaero.map.gui.GuiMap
 */
package ru.xaerofriends.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.gui.GuiMap;

@Environment(value=EnvType.CLIENT)
@Mixin(value={GuiMap.class}, remap=false)
public interface GuiMapAccessor {
    @Accessor(value="viewed", remap=false)
    public HoveredMapElementHolder<?, ?> xaerofriends$getViewed();
}

