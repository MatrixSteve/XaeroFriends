/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Invoker
 *  xaero.common.gui.GuiWaypoints
 *  xaero.common.minimap.waypoints.Waypoint
 */
package ru.xaerofriends.mixin;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;

@Environment(value=EnvType.CLIENT)
@Mixin(value={GuiWaypoints.class}, remap=false)
public interface GuiWaypointsAccessor {
    @Invoker(value="getSelectedWaypointsList", remap=false)
    public ArrayList<Waypoint> xaerofriends$getSelectedWaypoints();
}

