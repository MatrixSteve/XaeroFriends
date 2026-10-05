/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
 *  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package ru.xaerofriends;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.xaerofriends.network.FriendsC2SPayload;
import ru.xaerofriends.network.FriendsS2CPayload;
import ru.xaerofriends.server.FriendStore;

public final class XaeroFriends
implements ModInitializer {
    public static final String MOD_ID = "xaerofriends";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"xaerofriends");

    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(FriendsC2SPayload.ID, FriendsC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FriendsS2CPayload.ID, FriendsS2CPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FriendsC2SPayload.ID, (payload, context) -> context.server().execute(() -> FriendStore.forServer(context.server()).handle(context.player(), payload.json())));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> FriendStore.forServer(server).onJoin(handler.field_14140)));
        ServerLifecycleEvents.SERVER_STOPPED.register(FriendStore::forgetServer);
        LOGGER.info("XaeroFriends server networking is ready.");
    }
}

