package com.connorlinfoot.actionbarapi;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;


public class ActionBarAPI extends JavaPlugin {

    @Getter
    private static Plugin instance;

    private static String nmsVersion;
    private static boolean useOldMethods = false;

    @Override
    public void onEnable() {
        instance = this;

        nmsVersion = Bukkit.getServer().getClass().getPackage().getName();
        nmsVersion = nmsVersion.substring(nmsVersion.lastIndexOf(".") + 1);

        if (nmsVersion.equalsIgnoreCase("v1_8_R1") || nmsVersion.startsWith("v1_7_")) {
            useOldMethods = true;
        }

        getLogger().info("ActionBarAPI has been enabled!");
    }

    @SneakyThrows
    public static void sendActionBar(Player player, String message) {
        if (!player.isOnline()) {
            return;
        }

        Object packet = createActionBarPacket(message);
        sendPacket(player, packet);
    }

    private static Object createActionBarPacket(String message) throws Exception {
        Class<?> packetPlayOutChatClass = getNMSClass("PacketPlayOutChat");

        Object packet;

        if (useOldMethods) {
            packet = createOldActionBarPacket(message, packetPlayOutChatClass);
        } else {
            packet = createNewActionBarPacket(message, packetPlayOutChatClass);
        }

        return packet;
    }

    private static Object createOldActionBarPacket(String message, Class<?> packetPlayOutChatClass) throws Exception {
        Class<?> chatSerializerClass = getNMSClass("ChatSerializer");
        Class<?> iChatBaseComponentClass = getNMSClass("IChatBaseComponent");

        Method chatSerializerMethod = chatSerializerClass.getDeclaredMethod("a", String.class);
        Object cbc = iChatBaseComponentClass.cast(chatSerializerMethod.invoke(chatSerializerClass, "{\"text\": \"" + message + "\"}"));

        return packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class).newInstance(cbc, (byte) 2);
    }

    private static Object createNewActionBarPacket(String message, Class<?> packetPlayOutChatClass) throws Exception {
        Class<?> chatComponentTextClass = getNMSClass("ChatComponentText");
        Class<?> iChatBaseComponentClass = getNMSClass("IChatBaseComponent");

        try {
            Class<?> chatMessageTypeClass = getNMSClass("ChatMessageType");
            Object chatMessageType = getChatMessageType(chatMessageTypeClass);

            Object chatCompontentText = chatComponentTextClass.getConstructor(String.class).newInstance(message);
            return packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, chatMessageTypeClass).newInstance(chatCompontentText, chatMessageType);
        } catch (ClassNotFoundException exception) {
            Object chatCompontentText = chatComponentTextClass.getConstructor(String.class).newInstance(message);
            return packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class).newInstance(chatCompontentText, (byte) 2);
        }
    }

    private static Object getChatMessageType(Class<?> chatMessageTypeClass) {
        Object[] chatMessageTypes = chatMessageTypeClass.getEnumConstants();
        Object chatMessageType = null;
        for (Object obj : chatMessageTypes) {
            if (obj.toString().equals("GAME_INFO")) {
                chatMessageType = obj;
            }
        }
        return chatMessageType;
    }

    private static Object getCraftPlayerHandle(Object craftPlayer) throws Exception {
        Method craftPlayerHandleMethod = craftPlayer.getClass().getDeclaredMethod("getHandle");
        return craftPlayerHandleMethod.invoke(craftPlayer);
    }

    private static Class<?> getCraftPlayerClass() throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".entity.CraftPlayer");
    }

    private static Class<?> getNMSClass(String className) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + nmsVersion + "." + className);
    }

    private static void sendPacket(Player player, Object packet) throws Exception {
        Class<?> craftPlayerClass = getCraftPlayerClass();
        Object craftPlayer = craftPlayerClass.cast(player);
        Object craftPlayerHandle = getCraftPlayerHandle(craftPlayer);
        Object playerConnection = craftPlayerHandle.getClass().getDeclaredField("playerConnection").get(craftPlayerHandle);

        Method sendPacketMethod = playerConnection.getClass().getDeclaredMethod("sendPacket", getNMSClass("Packet"));
        sendPacketMethod.invoke(playerConnection, packet);
    }

    public static void sendActionBar(Player player, String message, int duration) {
        sendActionBar(player, message);

        if (duration >= 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendActionBar(player, "");
                }
            }.runTaskLater(instance, duration + 1);
        }

        while (duration > 40) {
            duration -= 40;
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendActionBar(player, message);
                }
            }.runTaskLater(instance, duration);
        }
    }

    public static void sendActionBarToAllPlayers(String message) {
        sendActionBarToAllPlayers(message, -1);
    }

    public static void sendActionBarToAllPlayers(String message, int duration) {
        Bukkit.getOnlinePlayers().forEach(player -> sendActionBar(player, message, duration));
    }
}