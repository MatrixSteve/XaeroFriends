/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_8710
 *  net.minecraft.class_8710$class_9154
 *  net.minecraft.class_9129
 *  net.minecraft.class_9135
 *  net.minecraft.class_9139
 */
package ru.xaerofriends.network;

import net.minecraft.class_2960;
import net.minecraft.class_8710;
import net.minecraft.class_9129;
import net.minecraft.class_9135;
import net.minecraft.class_9139;

public record FriendsS2CPayload(String json) implements class_8710
{
    public static final class_8710.class_9154<FriendsS2CPayload> ID = new class_8710.class_9154(class_2960.method_60655((String)"xaerofriends", (String)"server_snapshot"));
    public static final class_9139<class_9129, FriendsS2CPayload> CODEC = class_9139.method_56434((class_9139)class_9135.field_48554, FriendsS2CPayload::json, FriendsS2CPayload::new);

    public class_8710.class_9154<? extends class_8710> method_56479() {
        return ID;
    }
}

