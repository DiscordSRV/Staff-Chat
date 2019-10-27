package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.api.MessageFromGameEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MessageFromGameListener implements Listener {
	private StaffChatPlugin plugin;
	
	public MessageFromGameListener(StaffChatPlugin plugin) { this.plugin = plugin; }
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onMessageFromGame(MessageFromGameEvent event) {
		plugin.getApi().submitFromInGame(event.getPlayer(), event.getMessage());
	}
}
