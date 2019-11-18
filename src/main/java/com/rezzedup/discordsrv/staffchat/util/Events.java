package com.rezzedup.discordsrv.staffchat.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public class Events
{
    private Events() {}
    
    public static <E extends Event> E call(E event)
    {
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }
}
