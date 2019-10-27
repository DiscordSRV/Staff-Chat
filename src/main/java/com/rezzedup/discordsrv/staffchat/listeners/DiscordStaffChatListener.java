package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.api.MessageFromDiscordEvent;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import org.bukkit.Bukkit;

public class DiscordStaffChatListener
{
    private final StaffChatPlugin plugin;
    
    public DiscordStaffChatListener(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @Subscribe
    public void onDiscordChat(DiscordGuildMessagePreProcessEvent event)
    {
        if (event.getChannel().equals(plugin.getDiscordChannel()))
        {
            Bukkit.getServer().getPluginManager().callEvent(new MessageFromDiscordEvent(event.getAuthor(), event.getGuild(), event.getChannel(), event.getMessage().getContentStripped()));
            event.setCancelled(true);
        }
    }
}
