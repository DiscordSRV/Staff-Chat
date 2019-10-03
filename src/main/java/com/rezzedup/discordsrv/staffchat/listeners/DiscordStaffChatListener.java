package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;

public class DiscordStaffChatListener
{
    private final StaffChatPlugin plugin;
    
    public DiscordStaffChatListener(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @Subscribe(priority = ListenerPriority.HIGHEST)
    public void onDiscordChat(DiscordGuildMessagePostProcessEvent event)
    {
        if (event.getChannel().equals(plugin.getDiscordChannel()))
        {
            plugin.submitFromDiscord(event.getAuthor(), event.getMessage(), event.getProcessedMessage());
            event.setCancelled(true);
        }
    }
}
