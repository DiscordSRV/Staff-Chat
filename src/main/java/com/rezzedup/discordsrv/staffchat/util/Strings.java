package com.rezzedup.discordsrv.staffchat.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import pl.tlinkowski.annotation.basic.NullOr;

public class Strings
{
    private Strings() {}
    
    public static boolean isEmptyOrNull(@NullOr String text) { return text == null || text.isEmpty(); }
    
    public static String orEmpty(@NullOr String text) { return (isEmptyOrNull(text)) ? "" : text; }
    
    public static String orEmpty(ConfigurationSection config, String key)
    {
        return orEmpty(config.getString(key));
    }
    
    public static String colorful(@NullOr String text)
    {
        return ChatColor.translateAlternateColorCodes('&', orEmpty(text));
    }
    
    public static String colorful(ConfigurationSection config, String key)
    {
        @NullOr String text = config.getString(key);
        if (text == null) { return colorful("&r" + key + " &c&o(&nmissing&c&o config value)"); }
        return colorful(text);
    }
}
