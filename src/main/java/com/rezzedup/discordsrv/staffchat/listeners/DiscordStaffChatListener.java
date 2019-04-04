package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;

public class DiscordStaffChatListener
{
    private final StaffChatPlugin plugin;
    
    public DiscordStaffChatListener(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @Subscribe
    public void onDiscordChat(DiscordGuildMessagePreProcessEvent event)
    {
        if (event.getChannel().equals(plugin.getDiscordChannel()))
        {
            plugin.submitFromDiscord(event.getAuthor(), event.getMessage());
            event.setCancelled(true);
        }
    }
}
