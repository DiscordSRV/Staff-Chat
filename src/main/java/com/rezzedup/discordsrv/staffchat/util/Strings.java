/*
 * The MIT License
 * Copyright © 2017-2021 RezzedUp and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.rezzedup.discordsrv.staffchat.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.Objects;

public class Strings
{
    private Strings() {}
    
    public static boolean isEmptyOrNull(@NullOr String text) { return text == null || text.isEmpty(); }
    
    public static String requireNonEmpty(@NullOr String text, String message)
    {
        if (text == null) { throw new NullPointerException(message); }
        if (text.isEmpty()) { throw new IllegalArgumentException(message); }
        return text;
    }
    
    public static String orEmpty(@NullOr String text) { return (isEmptyOrNull(text)) ? "" : text; }
    
    public static String orEmpty(ConfigurationSection config, String key)
    {
        return orEmpty(config.getString(key));
    }
    
    public static @NullOr String valueOfOrNull(@NullOr Object object)
    {
        return (object == null) ? null : String.valueOf(object);
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
