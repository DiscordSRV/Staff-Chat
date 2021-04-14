package com.rezzedup.discordsrv.staffchat.util;

import net.md_5.bungee.api.ChatColor;
import pl.tlinkowski.annotation.basic.NullOr;

public class Strings
{
    private Strings() {}
    
    public static String colorful(String text) { return ChatColor.translateAlternateColorCodes('&', text); }
    
    public static boolean isEmptyOrNull(@NullOr String text) { return text == null || text.isEmpty(); }
}
