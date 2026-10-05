/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 *  net.minecraft.class_5218
 *  net.minecraft.class_8710
 *  net.minecraft.server.MinecraftServer
 */
package ru.xaerofriends.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_5218;
import net.minecraft.class_8710;
import net.minecraft.server.MinecraftServer;
import ru.xaerofriends.XaeroFriends;
import ru.xaerofriends.network.FriendsS2CPayload;

public final class FriendStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_FRIENDS = 100;
    private static final int MAX_REQUESTS = 50;
    private static final int MAX_MARKERS = 120;
    private static final Map<MinecraftServer, FriendStore> STORES = new ConcurrentHashMap<MinecraftServer, FriendStore>();
    private final MinecraftServer server;
    private final Path dataFile;
    private final State state;
    private final Map<String, Long> lastRequestMillis = new HashMap<String, Long>();

    private FriendStore(MinecraftServer server) {
        this.server = server;
        this.dataFile = server.method_27050(class_5218.field_24188).resolve("data").resolve("xaerofriends.json");
        this.state = FriendStore.load(this.dataFile);
    }

    public static FriendStore forServer(MinecraftServer server) {
        return STORES.computeIfAbsent(server, FriendStore::new);
    }

    public static void forgetServer(MinecraftServer server) {
        STORES.remove(server);
    }

    public void onJoin(class_3222 player) {
        Profile profile = this.profile(player.method_5667(), player.method_5477().getString());
        profile.name = player.method_5477().getString();
        this.save();
        this.syncAll(this.server);
    }

    public void handle(class_3222 player, String rawJson) {
        JsonObject command;
        if (rawJson == null || rawJson.length() > 20000) {
            this.error(player, "xaerofriends.chat.invalid");
            return;
        }
        try {
            command = JsonParser.parseString((String)rawJson).getAsJsonObject();
        }
        catch (RuntimeException exception) {
            this.error(player, "xaerofriends.chat.invalid");
            return;
        }
        String type = FriendStore.string(command, "type", "");
        Profile sender = this.profile(player.method_5667(), player.method_5477().getString());
        sender.name = player.method_5477().getString();
        switch (type) {
            case "sync": {
                this.sync(player);
                break;
            }
            case "request": {
                this.request(player, sender, FriendStore.string(command, "target", ""));
                break;
            }
            case "accept": {
                this.accept(player, sender, FriendStore.string(command, "target", ""));
                break;
            }
            case "reject": {
                this.reject(player, sender, FriendStore.string(command, "target", ""));
                break;
            }
            case "remove_friend": {
                this.removeFriend(player, sender, FriendStore.string(command, "target", ""));
                break;
            }
            case "trust": {
                this.setTrusted(player, sender, FriendStore.string(command, "target", ""), command.has("trusted") && command.get("trusted").getAsBoolean());
                break;
            }
            case "marker": {
                this.updateMarker(player, sender, command);
                break;
            }
            default: {
                this.error(player, "xaerofriends.chat.invalid");
            }
        }
    }

    private void request(class_3222 senderPlayer, Profile sender, String requestedName) {
        if (!this.allowRequest(senderPlayer, "friend", 800L)) {
            return;
        }
        String normalized = requestedName.trim();
        if (normalized.isEmpty() || normalized.length() > 16) {
            this.error(senderPlayer, "xaerofriends.chat.invalid");
            return;
        }
        class_3222 targetPlayer = this.server.method_3760().method_14571().stream().filter(candidate -> candidate.method_5477().getString().equalsIgnoreCase(normalized)).findFirst().orElse(null);
        if (targetPlayer == null) {
            this.error(senderPlayer, "xaerofriends.chat.player_offline");
            this.sync(senderPlayer);
            return;
        }
        if (targetPlayer.method_5667().equals(senderPlayer.method_5667())) {
            this.error(senderPlayer, "xaerofriends.chat.invalid");
            return;
        }
        Profile target = this.profile(targetPlayer.method_5667(), targetPlayer.method_5477().getString());
        target.name = targetPlayer.method_5477().getString();
        String senderId = senderPlayer.method_5845();
        String targetId = targetPlayer.method_5845();
        if (sender.friends.containsKey(targetId) || sender.outgoing.containsKey(targetId) || sender.incoming.containsKey(targetId) || target.incoming.containsKey(senderId) || sender.friends.size() >= 100 || sender.outgoing.size() >= 50 || target.incoming.size() >= 50) {
            this.error(senderPlayer, sender.friends.size() >= 100 || sender.outgoing.size() >= 50 || target.incoming.size() >= 50 ? "xaerofriends.chat.friend_limit" : "xaerofriends.chat.request_exists");
            this.sync(senderPlayer);
            return;
        }
        sender.outgoing.put(targetId, target.name);
        target.incoming.put(senderId, sender.name);
        this.save();
        this.syncAll(this.server);
        senderPlayer.method_7353((class_2561)class_2561.method_43469((String)"xaerofriends.chat.request_sent", (Object[])new Object[]{target.name}), false);
        targetPlayer.method_7353((class_2561)class_2561.method_43469((String)"xaerofriends.chat.request_received", (Object[])new Object[]{sender.name}), false);
    }

    private void accept(class_3222 player, Profile recipient, String requesterId) {
        if (!FriendStore.isUuid(requesterId) || !recipient.incoming.containsKey(requesterId)) {
            this.error(player, "xaerofriends.chat.invalid");
            return;
        }
        Profile requester = this.state.profiles.get(requesterId);
        if (requester == null || recipient.friends.size() >= 100 || requester.friends.size() >= 100) {
            this.error(player, "xaerofriends.chat.friend_limit");
            return;
        }
        String recipientId = player.method_5845();
        recipient.incoming.remove(requesterId);
        requester.outgoing.remove(recipientId);
        recipient.friends.put(requesterId, requester.name);
        requester.friends.put(recipientId, recipient.name);
        this.save();
        this.syncAll(this.server);
        player.method_7353((class_2561)class_2561.method_43469((String)"xaerofriends.chat.request_accepted", (Object[])new Object[]{requester.name}), false);
        this.online(this.server, requesterId).ifPresent(other -> other.method_7353((class_2561)class_2561.method_43469((String)"xaerofriends.chat.request_accepted", (Object[])new Object[]{recipient.name}), false));
    }

    private void reject(class_3222 player, Profile recipient, String requesterId) {
        if (!FriendStore.isUuid(requesterId) || recipient.incoming.remove(requesterId) == null) {
            this.error(player, "xaerofriends.chat.invalid");
            return;
        }
        Profile requester = this.state.profiles.get(requesterId);
        if (requester != null) {
            requester.outgoing.remove(player.method_5845());
        }
        this.save();
        this.syncAll(this.server);
        player.method_7353((class_2561)class_2561.method_43471((String)"xaerofriends.chat.request_rejected"), false);
    }

    private void removeFriend(class_3222 player, Profile profile, String targetId) {
        if (!profile.friends.containsKey(targetId)) {
            this.error(player, "xaerofriends.chat.not_friends");
            return;
        }
        String playerId = player.method_5845();
        profile.friends.remove(targetId);
        profile.trusted.remove(targetId);
        Profile other = this.state.profiles.get(targetId);
        if (other != null) {
            other.friends.remove(playerId);
            other.trusted.remove(playerId);
        }
        this.save();
        this.syncAll(this.server);
    }

    private void setTrusted(class_3222 player, Profile profile, String targetId, boolean trusted) {
        if (!profile.friends.containsKey(targetId)) {
            this.error(player, "xaerofriends.chat.not_friends");
            return;
        }
        if (trusted) {
            profile.trusted.add(targetId);
        } else {
            profile.trusted.remove(targetId);
        }
        this.save();
        this.syncAll(this.server);
    }

    private void updateMarker(class_3222 player, Profile owner, JsonObject command) {
        int z;
        int y;
        int x;
        if (!this.allowRequest(player, "marker", 250L)) {
            return;
        }
        String name = FriendStore.string(command, "name", "").trim();
        String dimension = FriendStore.string(command, "dimension", "").trim();
        String id = FriendStore.string(command, "id", "").trim();
        String visibility = FriendStore.string(command, "visibility", "hidden").toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.length() > 64 || dimension.isEmpty() || dimension.length() > 256 || id.isEmpty() || id.length() > 80 || !Set.of("hidden", "friends", "trusted").contains(visibility) || !command.has("x") || !command.has("y") || !command.has("z")) {
            this.error(player, "xaerofriends.chat.invalid");
            return;
        }
        try {
            x = command.get("x").getAsInt();
            y = command.get("y").getAsInt();
            z = command.get("z").getAsInt();
        }
        catch (RuntimeException exception) {
            this.error(player, "xaerofriends.chat.invalid");
            return;
        }
        if (Math.abs((long)x) > 30000000L || Math.abs((long)z) > 30000000L || y < -2048 || y > 2048) {
            this.error(player, "xaerofriends.chat.invalid");
            return;
        }
        String ownerId = player.method_5845();
        owner.markers.entrySet().removeIf(entry -> ((SharedMarker)entry.getValue()).dimension.equals(dimension) && ((SharedMarker)entry.getValue()).name.equalsIgnoreCase(name));
        if (owner.markers.size() >= 120 && !owner.markers.containsKey(id)) {
            this.error(player, "xaerofriends.chat.marker_limit");
            return;
        }
        owner.markers.put(id, new SharedMarker(id, ownerId, owner.name, name, dimension, x, y, z, visibility));
        this.save();
        this.syncAll(this.server);
    }

    private void syncAll(MinecraftServer server) {
        for (class_3222 online : server.method_3760().method_14571()) {
            this.sync(online);
        }
    }

    private void sync(class_3222 viewer) {
        Profile profile = this.profile(viewer.method_5667(), viewer.method_5477().getString());
        profile.name = viewer.method_5477().getString();
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("type", "snapshot");
        snapshot.add("friends", (JsonElement)this.friendArray(this.server, profile));
        snapshot.add("requests", (JsonElement)this.requestArray(this.server, profile.incoming));
        snapshot.add("pending", (JsonElement)this.requestArray(this.server, profile.outgoing));
        snapshot.add("ownMarkers", (JsonElement)this.ownMarkerArray(profile));
        snapshot.add("markers", (JsonElement)this.visibleMarkerArray(viewer.method_5845(), profile));
        ServerPlayNetworking.send((class_3222)viewer, (class_8710)new FriendsS2CPayload(GSON.toJson((JsonElement)snapshot)));
    }

    private JsonArray friendArray(MinecraftServer server, Profile profile) {
        JsonArray array = new JsonArray();
        profile.friends.forEach((uuid, fallbackName) -> {
            JsonObject friend = new JsonObject();
            friend.addProperty("uuid", uuid);
            friend.addProperty("name", this.knownName(server, (String)uuid, (String)fallbackName));
            friend.addProperty("trusted", Boolean.valueOf(profile.trusted.contains(uuid)));
            array.add((JsonElement)friend);
        });
        return array;
    }

    private JsonArray requestArray(MinecraftServer server, Map<String, String> requests) {
        JsonArray array = new JsonArray();
        requests.forEach((uuid, fallbackName) -> {
            JsonObject request = new JsonObject();
            request.addProperty("uuid", uuid);
            request.addProperty("name", this.knownName(server, (String)uuid, (String)fallbackName));
            array.add((JsonElement)request);
        });
        return array;
    }

    private JsonArray ownMarkerArray(Profile profile) {
        JsonArray array = new JsonArray();
        for (SharedMarker marker : profile.markers.values()) {
            array.add((JsonElement)marker.toJson(false));
        }
        return array;
    }

    private JsonArray visibleMarkerArray(String viewerId, Profile viewer) {
        JsonArray array = new JsonArray();
        for (Map.Entry<String, Profile> entry : this.state.profiles.entrySet()) {
            String ownerId = entry.getKey();
            Profile owner = entry.getValue();
            if (!viewer.friends.containsKey(ownerId)) continue;
            for (SharedMarker marker : owner.markers.values()) {
                boolean visibleToTrusted;
                boolean visibleToFriends = marker.visibility.equals("friends");
                boolean bl = visibleToTrusted = marker.visibility.equals("trusted") && owner.trusted.contains(viewerId);
                if (!visibleToFriends && !visibleToTrusted) continue;
                array.add((JsonElement)marker.toJson(true));
                if (array.size() < 500) continue;
                return array;
            }
        }
        return array;
    }

    private String knownName(MinecraftServer server, String uuid, String fallback) {
        return this.online(server, uuid).map(player -> player.method_5477().getString()).orElse(fallback);
    }

    private Optional<class_3222> online(MinecraftServer server, String uuid) {
        if (!FriendStore.isUuid(uuid)) {
            return Optional.empty();
        }
        return Optional.ofNullable(server.method_3760().method_14602(UUID.fromString(uuid)));
    }

    private boolean allowRequest(class_3222 player, String action, long cooldownMillis) {
        long last;
        String key = player.method_5845() + ":" + action;
        long now = System.currentTimeMillis();
        if (now - (last = this.lastRequestMillis.getOrDefault(key, 0L).longValue()) < cooldownMillis) {
            return false;
        }
        this.lastRequestMillis.put(key, now);
        return true;
    }

    private void error(class_3222 player, String key) {
        player.method_7353((class_2561)class_2561.method_43471((String)key), false);
    }

    private Profile profile(UUID uuid, String name) {
        String key = uuid.toString();
        Profile profile = this.state.profiles.computeIfAbsent(key, ignored -> new Profile());
        profile.normalize();
        if (name != null && !name.isBlank()) {
            profile.name = name;
        }
        return profile;
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsString();
        }
        catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        }
        catch (RuntimeException ignored) {
            return false;
        }
    }

    private void save() {
        try {
            Files.createDirectories(this.dataFile.getParent(), new FileAttribute[0]);
            Path temporary = this.dataFile.resolveSibling(String.valueOf(this.dataFile.getFileName()) + ".tmp");
            Files.writeString(temporary, (CharSequence)GSON.toJson((Object)this.state), new OpenOption[0]);
            try {
                Files.move(temporary, this.dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, this.dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException exception) {
            XaeroFriends.LOGGER.error("Could not save XaeroFriends data to {}", (Object)this.dataFile, (Object)exception);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static State load(Path file) {
        if (!Files.exists(file, new LinkOption[0])) {
            return new State();
        }
        try (BufferedReader reader = Files.newBufferedReader(file);){
            State loaded = (State)GSON.fromJson((Reader)reader, State.class);
            if (loaded == null) {
                State state2 = new State();
                return state2;
            }
            loaded.normalize();
            State state = loaded;
            return state;
        }
        catch (IOException | RuntimeException exception) {
            XaeroFriends.LOGGER.error("Could not load XaeroFriends data from {}", (Object)file, (Object)exception);
            return new State();
        }
    }

    private static final class State {
        private Map<String, Profile> profiles = new LinkedHashMap<String, Profile>();

        private State() {
        }

        private void normalize() {
            if (this.profiles == null) {
                this.profiles = new LinkedHashMap<String, Profile>();
            }
            this.profiles.values().forEach(Profile::normalize);
        }
    }

    private static final class Profile {
        private String name = "";
        private Map<String, String> friends = new LinkedHashMap<String, String>();
        private Set<String> trusted = new HashSet<String>();
        private Map<String, String> incoming = new LinkedHashMap<String, String>();
        private Map<String, String> outgoing = new LinkedHashMap<String, String>();
        private Map<String, SharedMarker> markers = new LinkedHashMap<String, SharedMarker>();

        private Profile() {
        }

        private void normalize() {
            if (this.friends == null) {
                this.friends = new LinkedHashMap<String, String>();
            }
            if (this.trusted == null) {
                this.trusted = new HashSet<String>();
            }
            if (this.incoming == null) {
                this.incoming = new LinkedHashMap<String, String>();
            }
            if (this.outgoing == null) {
                this.outgoing = new LinkedHashMap<String, String>();
            }
            if (this.markers == null) {
                this.markers = new LinkedHashMap<String, SharedMarker>();
            }
            this.markers.values().removeIf(marker -> marker == null);
            for (SharedMarker marker2 : this.markers.values()) {
                marker2.normalize();
            }
        }
    }

    private static final class SharedMarker {
        private String id;
        private String ownerId;
        private String ownerName;
        private String name;
        private String dimension;
        private int x;
        private int y;
        private int z;
        private String visibility;

        private SharedMarker(String id, String ownerId, String ownerName, String name, String dimension, int x, int y, int z, String visibility) {
            this.id = id;
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.name = name;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.visibility = visibility;
        }

        private SharedMarker() {
        }

        private void normalize() {
            if (this.id == null) {
                this.id = "";
            }
            if (this.ownerId == null) {
                this.ownerId = "";
            }
            if (this.ownerName == null) {
                this.ownerName = "";
            }
            if (this.name == null) {
                this.name = "";
            }
            if (this.dimension == null) {
                this.dimension = "";
            }
            if (this.visibility == null) {
                this.visibility = "hidden";
            }
        }

        private JsonObject toJson(boolean includeOwner) {
            JsonObject object = new JsonObject();
            object.addProperty("id", this.id);
            object.addProperty("name", this.name);
            object.addProperty("dimension", this.dimension);
            object.addProperty("x", (Number)this.x);
            object.addProperty("y", (Number)this.y);
            object.addProperty("z", (Number)this.z);
            object.addProperty("visibility", this.visibility);
            if (includeOwner) {
                object.addProperty("ownerId", this.ownerId);
                object.addProperty("ownerName", this.ownerName);
            }
            return object;
        }
    }
}

