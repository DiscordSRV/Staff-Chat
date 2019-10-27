package com.rezzedup.discordsrv.staffchat.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MessageFromGameEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private String message;
	
	public MessageFromGameEvent(Player player, String message) {
		super(true); // can be fired synchronous or asynchronous
		this.player = player;
		this.message = message;
	}
	
	@Override
	public HandlerList getHandlers() { return handlers; }
	
	public static HandlerList getHandlerList() { return handlers; }
	
	public String getMessage() { return message; }
	
	public void setMessage(String message) { this.message = message; }
	
	public Player getPlayer() { return player; }
}
