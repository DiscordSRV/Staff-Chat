package com.rezzedup.discordsrv.staffchat.api;

import github.scarsz.discordsrv.dependencies.jda.core.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.core.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.core.entities.User;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MessageFromDiscordEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final User author;
	private final Guild guild;
	private final TextChannel channel;
	private String message;
	
	public MessageFromDiscordEvent(User author, Guild guild, TextChannel channel, String message) {
		super(true); // can be fired synchronous or asynchronous
		this.author = author;
		this.guild = guild;
		this.channel = channel;
		this.message = message;
	}
	
	@Override
	public HandlerList getHandlers() { return handlers; }
	
	public static HandlerList getHandlerList() { return handlers; }
	
	public String getMessage() { return message; }
	
	public void setMessage(String message) { this.message = message; }
	
	public User getAuthor() { return author; }
	
	public Guild getGuild() { return guild;	}
	
	public TextChannel getChannel() { return channel; }
}
