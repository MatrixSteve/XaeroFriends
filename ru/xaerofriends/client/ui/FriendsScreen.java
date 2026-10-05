/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_11909
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
package ru.xaerofriends.client.ui;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5348;
import ru.xaerofriends.client.XaeroFriendsClient;
import ru.xaerofriends.network.FriendsC2SPayload;

@Environment(value=EnvType.CLIENT)
public final class FriendsScreen
extends class_437 {
    private static final int ROW_HEIGHT = 32;
    private static final int REQUEST_ROW_HEIGHT = 54;
    private final class_437 parent;
    private class_342 playerName;
    private int friendScroll;
    private int requestScroll;
    private int leftX;
    private int rightX;
    private int leftWidth;
    private int rightWidth;
    private int listTop;
    private int requestsTop;
    private int visibleFriendRows;
    private int visibleRequestRows;

    public FriendsScreen(class_437 parent) {
        super((class_2561)class_2561.method_43471((String)"xaerofriends.screen.title"));
        this.parent = parent;
    }

    protected void method_25426() {
        super.method_25426();
        int outer = Math.max(16, this.field_22789 / 32);
        int gap = 18;
        int columnWidth = (this.field_22789 - outer * 2 - gap) / 2;
        this.leftX = outer;
        this.leftWidth = columnWidth;
        this.rightX = outer + columnWidth + gap;
        this.rightWidth = columnWidth;
        this.listTop = 82;
        this.requestsTop = 158;
        this.visibleFriendRows = Math.max(1, (this.field_22790 - this.listTop - 54) / 32);
        this.visibleRequestRows = Math.max(1, (this.field_22790 - this.requestsTop - 54) / 54);
        int inputWidth = Math.max(70, this.rightWidth - 96);
        this.playerName = new class_342(this.field_22793, this.rightX, 82, inputWidth, 20, (class_2561)class_2561.method_43471((String)"xaerofriends.screen.username"));
        this.playerName.method_1880(16);
        this.playerName.method_47404((class_2561)class_2561.method_43471((String)"xaerofriends.screen.username"));
        this.method_37063((class_364)this.playerName);
        this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43471((String)"xaerofriends.screen.send"), button -> {
            String name = this.playerName.method_1882().trim();
            if (!name.isEmpty()) {
                XaeroFriendsClient.sendFriendRequest(name);
                this.playerName.method_1852("");
            }
        }).method_46434(this.rightX + inputWidth + 6, 82, Math.min(82, this.rightWidth - inputWidth - 6), 20).method_46431());
        this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43471((String)"gui.done"), button -> this.method_25419()).method_46434(this.field_22789 / 2 - 52, this.field_22790 - 32, 104, 20).method_46431());
        this.friendScroll = FriendsScreen.clampScroll(this.friendScroll, XaeroFriendsClient.friends().size(), this.visibleFriendRows);
        this.requestScroll = FriendsScreen.clampScroll(this.requestScroll, XaeroFriendsClient.incomingRequests().size(), this.visibleRequestRows);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        int y;
        int index;
        int row;
        this.method_25420(context, mouseX, mouseY, delta);
        List<XaeroFriendsClient.FriendEntry> friends = XaeroFriendsClient.friends();
        List<XaeroFriendsClient.RequestEntry> requests = XaeroFriendsClient.incomingRequests();
        int paneTop = 48;
        int paneBottom = this.field_22790 - 43;
        context.method_25294(this.leftX - 8, paneTop, this.leftX + this.leftWidth + 4, paneBottom, -1190127836);
        context.method_25294(this.rightX - 4, paneTop, this.rightX + this.rightWidth + 8, paneBottom, -1190127836);
        context.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 18, 0xFFFFFF);
        context.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"xaerofriends.screen.friends"), this.leftX, 56, 11065599);
        context.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"xaerofriends.screen.requests"), this.rightX, 122, 11065599);
        if (!ClientPlayNetworking.canSend(FriendsC2SPayload.ID)) {
            context.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"xaerofriends.screen.server_missing"), this.rightX, 146, 16757629);
        }
        if (friends.isEmpty()) {
            context.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"xaerofriends.screen.empty.friends"), this.leftX, this.listTop + 4, 12108496);
        } else {
            for (row = 0; row < this.visibleFriendRows && (index = this.friendScroll + row) < friends.size(); ++row) {
                XaeroFriendsClient.FriendEntry friend = friends.get(index);
                y = this.listTop + row * 32;
                context.method_25294(this.leftX - 4, y - 3, this.leftX + this.leftWidth, y + 25, mouseX >= this.leftX - 4 && mouseX <= this.leftX + this.leftWidth && mouseY >= y - 3 && mouseY <= y + 25 ? -952611741 : -1473891256);
                class_5250 trustLabel = class_2561.method_43471((String)(friend.trusted() ? "xaerofriends.screen.trusted" : "xaerofriends.screen.regular"));
                int trustWidth = this.field_22793.method_27525((class_5348)trustLabel) + 12;
                int trustX = this.leftX + this.leftWidth - trustWidth - 5;
                context.method_25294(trustX, y + 1, trustX + trustWidth, y + 21, friend.trusted() ? -14395583 : -12891556);
                context.method_27534(this.field_22793, (class_2561)trustLabel, trustX + trustWidth / 2, y + 6, 0xFFFFFF);
                String name = this.field_22793.method_27523(friend.name(), Math.max(24, trustX - this.leftX - 10));
                context.method_25303(this.field_22793, name, this.leftX + 4, y + 5, 0xFFFFFF);
            }
        }
        if (requests.isEmpty()) {
            context.method_27535(this.field_22793, (class_2561)class_2561.method_43471((String)"xaerofriends.screen.empty.requests"), this.rightX, this.requestsTop + 4, 12108496);
        } else {
            for (row = 0; row < this.visibleRequestRows && (index = this.requestScroll + row) < requests.size(); ++row) {
                XaeroFriendsClient.RequestEntry request = requests.get(index);
                y = this.requestsTop + row * 54;
                context.method_25294(this.rightX - 4, y - 4, this.rightX + this.rightWidth, y + 46, -1473891256);
                String name = this.field_22793.method_27523(request.name(), Math.max(40, this.rightWidth - 12));
                context.method_25303(this.field_22793, name, this.rightX + 4, y + 2, 0xFFFFFF);
                int actionWidth = this.requestActionWidth();
                int declineX = this.rightX + this.rightWidth - actionWidth - 3;
                int acceptX = declineX - actionWidth - 5;
                this.drawAction(context, (class_2561)class_2561.method_43471((String)"xaerofriends.screen.accept"), acceptX, y + 21, actionWidth, mouseX, mouseY);
                this.drawAction(context, (class_2561)class_2561.method_43471((String)"xaerofriends.screen.reject"), declineX, y + 21, actionWidth, mouseX, mouseY);
            }
        }
        if (!XaeroFriendsClient.outgoingRequests().isEmpty()) {
            context.method_27535(this.field_22793, (class_2561)class_2561.method_43469((String)"xaerofriends.screen.outgoing", (Object[])new Object[]{XaeroFriendsClient.outgoingRequests().size()}), this.rightX, paneBottom - 17, 10333892);
        }
        super.method_25394(context, mouseX, mouseY, delta);
    }

    private void drawAction(class_332 context, class_2561 label, int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 20;
        context.method_25294(x, y, x + width, y + 20, hovered ? -11176555 : -12891556);
        context.method_27534(this.field_22793, label, x + width / 2, y + 6, 0xFFFFFF);
    }

    public boolean method_25402(class_11909 click, boolean doubled) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        if (click.method_74245() == 0) {
            int row;
            int index;
            int row2;
            int index2;
            List<XaeroFriendsClient.FriendEntry> friends = XaeroFriendsClient.friends();
            if (mouseX >= (double)(this.leftX - 4) && mouseX <= (double)(this.leftX + this.leftWidth) && mouseY >= (double)(this.listTop - 3) && mouseY < (double)(this.listTop + this.visibleFriendRows * 32) && (index2 = this.friendScroll + (row2 = (int)((mouseY - (double)this.listTop) / 32.0))) >= 0 && index2 < friends.size()) {
                XaeroFriendsClient.toggleTrusted(friends.get(index2));
                return true;
            }
            List<XaeroFriendsClient.RequestEntry> requests = XaeroFriendsClient.incomingRequests();
            if (mouseX >= (double)(this.rightX - 4) && mouseX <= (double)(this.rightX + this.rightWidth) && mouseY >= (double)(this.requestsTop - 4) && mouseY < (double)(this.requestsTop + this.visibleRequestRows * 54) && (index = this.requestScroll + (row = (int)((mouseY - (double)this.requestsTop) / 54.0))) >= 0 && index < requests.size()) {
                int y = this.requestsTop + row * 54;
                int actionWidth = this.requestActionWidth();
                int declineX = this.rightX + this.rightWidth - actionWidth - 3;
                int acceptX = declineX - actionWidth - 5;
                if (mouseY >= (double)(y + 21) && mouseY < (double)(y + 41)) {
                    if (mouseX >= (double)acceptX && mouseX < (double)(acceptX + actionWidth)) {
                        XaeroFriendsClient.acceptRequest(requests.get(index).uuid());
                        return true;
                    }
                    if (mouseX >= (double)declineX && mouseX < (double)(declineX + actionWidth)) {
                        XaeroFriendsClient.rejectRequest(requests.get(index).uuid());
                        return true;
                    }
                }
            }
        }
        return super.method_25402(click, doubled);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= (double)(this.leftX - 8) && mouseX <= (double)(this.leftX + this.leftWidth + 4) && mouseY >= (double)(this.listTop - 8) && mouseY < (double)(this.field_22790 - 40)) {
            this.friendScroll = FriendsScreen.clampScroll(this.friendScroll + (verticalAmount < 0.0 ? 1 : -1), XaeroFriendsClient.friends().size(), this.visibleFriendRows);
            return true;
        }
        if (mouseX >= (double)(this.rightX - 4) && mouseX <= (double)(this.rightX + this.rightWidth + 8) && mouseY >= (double)(this.requestsTop - 8) && mouseY < (double)(this.field_22790 - 40)) {
            this.requestScroll = FriendsScreen.clampScroll(this.requestScroll + (verticalAmount < 0.0 ? 1 : -1), XaeroFriendsClient.incomingRequests().size(), this.visibleRequestRows);
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void method_25419() {
        class_310.method_1551().method_1507(this.parent);
    }

    private static int clampScroll(int value, int itemCount, int visibleCount) {
        return Math.max(0, Math.min(value, Math.max(0, itemCount - visibleCount)));
    }

    private int requestActionWidth() {
        return Math.min(70, Math.max(40, (this.rightWidth - 12) / 2));
    }
}

