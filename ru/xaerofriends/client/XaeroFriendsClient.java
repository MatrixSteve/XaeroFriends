/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  java.lang.MatchException
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_7919
 *  net.minecraft.class_8710
 *  xaero.common.minimap.waypoints.Waypoint
 */
package ru.xaerofriends.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_7919;
import net.minecraft.class_8710;
import ru.xaerofriends.XaeroFriends;
import ru.xaerofriends.client.XaeroWaypointBridge;
import ru.xaerofriends.client.ui.FriendsScreen;
import ru.xaerofriends.mixin.GuiMapAccessor;
import ru.xaerofriends.mixin.GuiWaypointsAccessor;
import ru.xaerofriends.mixin.ScreenAccessor;
import ru.xaerofriends.network.FriendsC2SPayload;
import ru.xaerofriends.network.FriendsS2CPayload;
import xaero.common.minimap.waypoints.Waypoint;

@Environment(value=EnvType.CLIENT)
public final class XaeroFriendsClient
implements ClientModInitializer {
    private static final String MAP_SCREEN = "xaero.map.gui.GuiMap";
    private static final String WAYPOINTS_SCREEN = "xaero.common.gui.GuiWaypoints";
    private static final List<FriendEntry> friends = new ArrayList<FriendEntry>();
    private static final List<RequestEntry> incoming = new ArrayList<RequestEntry>();
    private static final List<RequestEntry> outgoing = new ArrayList<RequestEntry>();
    private static final List<FriendMarker> sharedMarkers = new ArrayList<FriendMarker>();
    private static final Map<String, String> ownMarkerPolicies = new HashMap<String, String>();
    private static Waypoint lastWaypoint;
    private static boolean markerRefreshPending;
    private static String lastDimension;
    private static class_4185 visibilityButton;

    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FriendsS2CPayload.ID, (payload, context) -> context.client().execute(() -> XaeroFriendsClient.applySnapshot(payload.json())));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> XaeroFriendsClient.sendCommand(XaeroFriendsClient.command("sync"))));
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> XaeroFriendsClient.onScreenOpened(client, screen));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.field_1724 == null || client.field_1687 == null) {
                return;
            }
            String dimension = client.field_1687.method_27983().method_29177().toString();
            if (!dimension.equals(lastDimension)) {
                lastDimension = dimension;
                markerRefreshPending = true;
            }
            if (markerRefreshPending && XaeroWaypointBridge.refresh(sharedMarkers)) {
                markerRefreshPending = false;
            }
        });
        XaeroFriends.LOGGER.info("XaeroFriends client interface is ready.");
    }

    private static void onScreenOpened(class_310 client, class_437 screen) {
        String screenClass = screen.getClass().getName();
        boolean worldMap = screenClass.equals(MAP_SCREEN);
        boolean minimapWaypoints = screenClass.equals(WAYPOINTS_SCREEN);
        if (!worldMap && !minimapWaypoints) {
            return;
        }
        ScreenEvents.afterRender((class_437)screen).register((renderedScreen, drawContext, mouseX, mouseY, tickDelta) -> XaeroFriendsClient.onScreenRendered(renderedScreen));
        lastWaypoint = null;
        int friendsX = Math.min(screen.field_22789 - 94, screen.field_22789 / 2 + 235);
        class_4185 friendsButton = class_4185.method_46430((class_2561)class_2561.method_43471((String)"xaerofriends.button.friends"), button -> client.method_1507((class_437)new FriendsScreen(screen))).method_46434(Math.max(4, friendsX), 22, 86, 20).method_46431();
        ((ScreenAccessor)screen).xaerofriends$addDrawableChild(friendsButton);
        visibilityButton = null;
        if (worldMap || minimapWaypoints) {
            visibilityButton = class_4185.method_46430((class_2561)XaeroFriendsClient.visibilityText(Visibility.HIDDEN), button -> XaeroFriendsClient.cycleVisibility(client)).method_46434(Math.max(4, screen.field_22789 / 2 - 320), screen.field_22790 - 56, 100, 20).method_46436(class_7919.method_47407((class_2561)class_2561.method_43471((String)"xaerofriends.visibility.tooltip"))).method_46431();
            XaeroFriendsClient.visibilityButton.field_22763 = false;
            ((ScreenAccessor)screen).xaerofriends$addDrawableChild(visibilityButton);
        }
    }

    private static void onScreenRendered(class_437 screen) {
        Waypoint current;
        boolean valid;
        if (visibilityButton == null || screen != class_310.method_1551().field_1755) {
            return;
        }
        Waypoint selected = XaeroFriendsClient.selectedWaypoint(screen);
        if (selected != null && !XaeroFriendsClient.isThirdParty(selected)) {
            lastWaypoint = selected;
        }
        XaeroFriendsClient.visibilityButton.field_22763 = valid = (current = lastWaypoint) != null && !XaeroFriendsClient.isThirdParty(current);
        if (valid) {
            String dimension = XaeroFriendsClient.currentDimension();
            String markerId = XaeroFriendsClient.markerId(current, dimension);
            visibilityButton.method_25355(XaeroFriendsClient.visibilityText(XaeroFriendsClient.parseVisibility(ownMarkerPolicies.get(markerId))));
        }
    }

    private static Waypoint selectedWaypoint(class_437 screen) {
        try {
            GuiWaypointsAccessor accessor;
            ArrayList<Waypoint> selected;
            if (screen.getClass().getName().equals(MAP_SCREEN) && screen instanceof GuiMapAccessor) {
                GuiMapAccessor accessor2 = (GuiMapAccessor)screen;
                return XaeroWaypointBridge.findWaypoint(accessor2.xaerofriends$getViewed());
            }
            if (screen.getClass().getName().equals(WAYPOINTS_SCREEN) && screen instanceof GuiWaypointsAccessor && (selected = (accessor = (GuiWaypointsAccessor)screen).xaerofriends$getSelectedWaypoints()) != null) {
                for (Object e : selected) {
                    Waypoint waypoint = XaeroWaypointBridge.findWaypoint(e);
                    if (waypoint == null || XaeroFriendsClient.isThirdParty(waypoint)) continue;
                    return waypoint;
                }
            }
        }
        catch (RuntimeException exception) {
            XaeroFriends.LOGGER.debug("Could not read the selected Xaero waypoint.", (Throwable)exception);
        }
        return null;
    }

    private static void cycleVisibility(class_310 client) {
        Waypoint waypoint = lastWaypoint;
        if (waypoint == null || XaeroFriendsClient.isThirdParty(waypoint)) {
            if (client.field_1724 != null) {
                client.field_1724.method_7353((class_2561)class_2561.method_43471((String)"xaerofriends.screen.select_waypoint"), false);
            }
            return;
        }
        String dimension = XaeroFriendsClient.currentDimension();
        String id = XaeroFriendsClient.markerId(waypoint, dimension);
        Visibility next = XaeroFriendsClient.parseVisibility(ownMarkerPolicies.get(id)).next();
        ownMarkerPolicies.put(id, next.key);
        XaeroFriendsClient.sendMarker(waypoint, dimension, id, next);
        if (visibilityButton != null) {
            visibilityButton.method_25355(XaeroFriendsClient.visibilityText(next));
        }
    }

    private static void sendMarker(Waypoint waypoint, String dimension, String id, Visibility visibility) {
        JsonObject payload = XaeroFriendsClient.command("marker");
        payload.addProperty("id", id);
        payload.addProperty("name", waypoint.getName());
        payload.addProperty("dimension", dimension);
        payload.addProperty("x", (Number)waypoint.getX());
        payload.addProperty("y", (Number)waypoint.getY());
        payload.addProperty("z", (Number)waypoint.getZ());
        payload.addProperty("visibility", visibility.key);
        XaeroFriendsClient.sendCommand(payload);
    }

    private static void sendCommand(JsonObject command) {
        if (!FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            return;
        }
        if (!ClientPlayNetworking.canSend(FriendsC2SPayload.ID)) {
            class_310 client = class_310.method_1551();
            if (client.field_1724 != null) {
                client.field_1724.method_7353((class_2561)class_2561.method_43471((String)"xaerofriends.screen.server_missing"), false);
            }
            return;
        }
        ClientPlayNetworking.send((class_8710)new FriendsC2SPayload(command.toString()));
    }

    private static void applySnapshot(String json) {
        try {
            JsonObject object = JsonParser.parseString((String)json).getAsJsonObject();
            friends.clear();
            incoming.clear();
            outgoing.clear();
            sharedMarkers.clear();
            ownMarkerPolicies.clear();
            XaeroFriendsClient.readFriends(object.getAsJsonArray("friends"));
            XaeroFriendsClient.readRequests(object.getAsJsonArray("requests"), incoming);
            XaeroFriendsClient.readRequests(object.getAsJsonArray("pending"), outgoing);
            XaeroFriendsClient.readOwnMarkerPolicies(object.getAsJsonArray("ownMarkers"));
            XaeroFriendsClient.readSharedMarkers(object.getAsJsonArray("markers"));
            markerRefreshPending = true;
        }
        catch (RuntimeException exception) {
            XaeroFriends.LOGGER.warn("Received an invalid XaeroFriends server snapshot.", (Throwable)exception);
        }
    }

    private static void readFriends(JsonArray array) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject friend = element.getAsJsonObject();
            friends.add(new FriendEntry(XaeroFriendsClient.jsonString(friend, "uuid"), XaeroFriendsClient.jsonString(friend, "name"), friend.has("trusted") && friend.get("trusted").getAsBoolean()));
        }
    }

    private static void readRequests(JsonArray array, List<RequestEntry> destination) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject request = element.getAsJsonObject();
            destination.add(new RequestEntry(XaeroFriendsClient.jsonString(request, "uuid"), XaeroFriendsClient.jsonString(request, "name")));
        }
    }

    private static void readOwnMarkerPolicies(JsonArray array) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject marker = element.getAsJsonObject();
            ownMarkerPolicies.put(XaeroFriendsClient.jsonString(marker, "id"), XaeroFriendsClient.jsonString(marker, "visibility"));
        }
    }

    private static void readSharedMarkers(JsonArray array) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject marker = element.getAsJsonObject();
            try {
                sharedMarkers.add(new FriendMarker(XaeroFriendsClient.jsonString(marker, "id"), XaeroFriendsClient.jsonString(marker, "ownerId"), XaeroFriendsClient.jsonString(marker, "ownerName"), XaeroFriendsClient.jsonString(marker, "name"), XaeroFriendsClient.jsonString(marker, "dimension"), marker.get("x").getAsInt(), marker.get("y").getAsInt(), marker.get("z").getAsInt()));
            }
            catch (RuntimeException runtimeException) {}
        }
    }

    private static String jsonString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static JsonObject command(String type) {
        JsonObject command = new JsonObject();
        command.addProperty("type", type);
        return command;
    }

    public static void sendFriendRequest(String name) {
        JsonObject request = XaeroFriendsClient.command("request");
        request.addProperty("target", name.trim());
        XaeroFriendsClient.sendCommand(request);
    }

    public static void acceptRequest(String uuid) {
        XaeroFriendsClient.sendTargetCommand("accept", uuid);
    }

    public static void rejectRequest(String uuid) {
        XaeroFriendsClient.sendTargetCommand("reject", uuid);
    }

    public static void removeFriend(String uuid) {
        XaeroFriendsClient.sendTargetCommand("remove_friend", uuid);
    }

    public static void toggleTrusted(FriendEntry friend) {
        JsonObject request = XaeroFriendsClient.command("trust");
        request.addProperty("target", friend.uuid());
        request.addProperty("trusted", Boolean.valueOf(!friend.trusted()));
        XaeroFriendsClient.sendCommand(request);
    }

    private static void sendTargetCommand(String type, String uuid) {
        JsonObject request = XaeroFriendsClient.command(type);
        request.addProperty("target", uuid);
        XaeroFriendsClient.sendCommand(request);
    }

    public static List<FriendEntry> friends() {
        return List.copyOf(friends);
    }

    public static List<RequestEntry> incomingRequests() {
        return List.copyOf(incoming);
    }

    public static List<RequestEntry> outgoingRequests() {
        return List.copyOf(outgoing);
    }

    private static String currentDimension() {
        class_310 client = class_310.method_1551();
        return client.field_1687 == null ? "minecraft:overworld" : client.field_1687.method_27983().method_29177().toString();
    }

    private static String markerId(Waypoint waypoint, String dimension) {
        String stableKey = waypoint.getName().toLowerCase(Locale.ROOT) + "|" + dimension + "|" + waypoint.getX() + "|" + waypoint.getY() + "|" + waypoint.getZ();
        return UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static boolean isThirdParty(Waypoint waypoint) {
        try {
            return waypoint.isThirdParty();
        }
        catch (RuntimeException ignored) {
            return true;
        }
    }

    private static Visibility parseVisibility(String key) {
        if ("friends".equals(key)) {
            return Visibility.FRIENDS;
        }
        if ("trusted".equals(key)) {
            return Visibility.TRUSTED;
        }
        return Visibility.HIDDEN;
    }

    private static class_2561 visibilityText(Visibility visibility) {
        return class_2561.method_43469((String)"xaerofriends.button.visibility", (Object[])new Object[]{class_2561.method_43471((String)("xaerofriends.visibility." + visibility.key))});
    }

    static {
        lastDimension = "";
    }

    @Environment(value=EnvType.CLIENT)
    private static enum Visibility {
        HIDDEN("hidden"),
        FRIENDS("friends"),
        TRUSTED("trusted");

        private final String key;

        private Visibility(String key) {
            this.key = key;
        }

        private Visibility next() {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> FRIENDS;
                case 1 -> TRUSTED;
                case 2 -> HIDDEN;
            };
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record FriendEntry(String uuid, String name, boolean trusted) {
    }

    @Environment(value=EnvType.CLIENT)
    public record RequestEntry(String uuid, String name) {
    }

    @Environment(value=EnvType.CLIENT)
    public record FriendMarker(String id, String ownerId, String ownerName, String name, String dimension, int x, int y, int z) {
    }
}

