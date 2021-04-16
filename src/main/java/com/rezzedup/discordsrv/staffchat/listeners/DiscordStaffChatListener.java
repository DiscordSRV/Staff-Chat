package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;

@SuppressWarnings("unused")
public class DiscordStaffChatListener
{
    private final StaffChatPlugin plugin;
    
    public DiscordStaffChatListener(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @Subscribe
    public void onDiscordChat(DiscordGuildMessagePreProcessEvent event)
    {
        if (event.getChannel().equals(plugin.getDiscordChannelOrNull()))
        {
            event.setCancelled(true); // Cancel this message from getting sent to global chat.
            
            // Handle this on the main thread next tick.
            plugin.sync().run(() -> plugin.submitMessageFromDiscord(event.getAuthor(), event.getMessage()));
        }
    }
}
