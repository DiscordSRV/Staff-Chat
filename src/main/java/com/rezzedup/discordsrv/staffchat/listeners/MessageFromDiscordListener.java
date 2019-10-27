package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.api.MessageFromDiscordEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MessageFromDiscordListener implements Listener {
	private final StaffChatPlugin plugin;
	
	public MessageFromDiscordListener(StaffChatPlugin plugin) { this.plugin = plugin; }
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onMessageFromDiscord(MessageFromDiscordEvent event) {
		plugin.getApi().submitFromDiscord(event.getAuthor(), event.getGuild(), event.getChannel(), event.getMessage());
	}
}
