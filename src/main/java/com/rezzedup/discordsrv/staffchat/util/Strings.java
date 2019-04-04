package com.rezzedup.discordsrv.staffchat.util;

import org.bukkit.ChatColor;

public class Strings
{
    private Strings() {}
    
    public static String colorful(String text) { return ChatColor.translateAlternateColorCodes('&', text); }
    
    public static boolean isEmptyOrNull(String text) { return text == null || text.isEmpty(); }
}
