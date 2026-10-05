/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_2960
 *  xaero.common.HudMod
 *  xaero.common.minimap.waypoints.Waypoint
 *  xaero.hud.minimap.module.MinimapSession
 *  xaero.hud.minimap.waypoint.WaypointColor
 *  xaero.hud.minimap.waypoint.WaypointPurpose
 *  xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypointManager
 *  xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints
 *  xaero.hud.minimap.world.container.MinimapWorldRootContainer
 *  xaero.hud.module.HudModule
 *  xaero.hud.module.ModuleSession
 */
package ru.xaerofriends.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2960;
import ru.xaerofriends.XaeroFriends;
import ru.xaerofriends.client.XaeroFriendsClient;
import xaero.common.HudMod;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypointManager;
import xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;
import xaero.hud.module.HudModule;
import xaero.hud.module.ModuleSession;

@Environment(value=EnvType.CLIENT)
final class XaeroWaypointBridge {
    private static final class_2960 ORIGIN = class_2960.method_60655((String)"xaerofriends", (String)"friends");
    private static String renderedDimension = "";

    private XaeroWaypointBridge() {
    }

    static boolean refresh(List<XaeroFriendsClient.FriendMarker> markers) {
        if (!FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            return true;
        }
        try {
            String dimension;
            MinimapSession session = XaeroWaypointBridge.currentSession();
            if (session == null || session.getWorldManager().getCurrentRootContainer() == null) {
                return false;
            }
            MinimapWorldRootContainer root = session.getWorldManager().getCurrentRootContainer();
            ThirdPartyWaypointManager manager = root.getThirdPartyWaypointManager();
            ThirdPartyWaypoints origin = manager.get(ORIGIN);
            origin.clear();
            renderedDimension = dimension = session.getMc().field_1687 == null ? "" : session.getMc().field_1687.method_27983().method_29177().toString();
            for (XaeroFriendsClient.FriendMarker marker : markers) {
                if (!marker.dimension().equals(dimension)) continue;
                String displayName = marker.ownerName() + " \u2014 " + marker.name();
                String initials = marker.ownerName().isBlank() ? "F" : marker.ownerName().substring(0, 1).toUpperCase();
                Waypoint waypoint = new Waypoint(marker.x(), marker.y(), marker.z(), displayName, initials, WaypointColor.AQUA, WaypointPurpose.NORMAL);
                origin.add(marker.ownerId() + ":" + marker.id(), waypoint);
            }
            return true;
        }
        catch (LinkageError | RuntimeException exception) {
            XaeroFriends.LOGGER.debug("Waiting for the current Xaero Minimap session.", exception);
            return false;
        }
    }

    static Waypoint findWaypoint(Object root) {
        return XaeroWaypointBridge.findWaypoint(root, Collections.newSetFromMap(new IdentityHashMap()), 0);
    }

    private static Waypoint findWaypoint(Object value, Set<Object> visited, int depth) {
        if (value == null || depth > 6 || !visited.add(value)) {
            return null;
        }
        if (value instanceof Waypoint) {
            Waypoint waypoint = (Waypoint)value;
            return waypoint;
        }
        String name = value.getClass().getName();
        if (name.equals("xaero.map.mods.gui.Waypoint")) {
            try {
                Method original = value.getClass().getMethod("getOriginal", new Class[0]);
                Object result = original.invoke(value, new Object[0]);
                if (result instanceof Waypoint) {
                    Waypoint waypoint = (Waypoint)result;
                    return waypoint;
                }
            }
            catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        for (Class<?> type = value.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType().isPrimitive() || field.getType().isEnum() || field.getType().getName().startsWith("java.lang.")) continue;
                try {
                    field.setAccessible(true);
                    Waypoint found = XaeroWaypointBridge.findWaypoint(field.get(value), visited, depth + 1);
                    if (found == null) continue;
                    return found;
                }
                catch (ReflectiveOperationException | RuntimeException exception) {
                    // empty catch block
                }
            }
        }
        return null;
    }

    private static MinimapSession currentSession() {
        HudMod mod = HudMod.INSTANCE;
        if (mod == null || mod.getHud() == null) {
            return null;
        }
        for (HudModule module : mod.getHud().getModuleManager().getModules()) {
            ModuleSession session = module.getCurrentSession();
            if (!(session instanceof MinimapSession)) continue;
            MinimapSession minimapSession = (MinimapSession)session;
            return minimapSession;
        }
        return null;
    }
}

