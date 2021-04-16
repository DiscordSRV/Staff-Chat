package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

@SuppressWarnings("unused")
public class DiscordSrvLoadedLaterListener implements Listener
{
    private final StaffChatPlugin plugin;
    
    public DiscordSrvLoadedLaterListener(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event)
    {
        if ("DiscordSRV".equals(event.getPlugin().getName()))
        {
            plugin.debug(getClass()).log(event, () -> "DiscordSRV loaded late: " + event.getPlugin());
            plugin.debugger().schedulePluginStatus(getClass(), "Loaded Late");
            plugin.subscribeToDiscordSrv(event.getPlugin());
        }
    }
}
