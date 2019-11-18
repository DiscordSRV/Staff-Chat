package com.rezzedup.discordsrv.staffchat.events;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public class DiscordStaffChatMessageEvent extends Event implements Cancellable
{
    private final User author;
    private final Message message;
    private String text;
    
    public DiscordStaffChatMessageEvent(User author, Message message, String text)
    {
        this.author = Objects.requireNonNull(author, "author");
        this.message = Objects.requireNonNull(message, "message");
        setText(text);
    }
    
    public User getAuthor() { return author; }
    
    public Message getMessage() { return message; }
    
    public String getText() { return text; }
    
    public void setText(String text) { this.text = Objects.requireNonNull(text, "text"); }
    
    //
    //  - - - HandlerList boilerplate - - -
    //
    
    public static final HandlerList HANDLERS = new HandlerList();
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    
    public static HandlerList getHandlerList() { return HANDLERS; }
    
    //
    //  - - - Cancellable boilerplate - - -
    //
    
    private boolean isCancelled = false;
    
    @Override
    public boolean isCancelled() { return isCancelled; }
    
    @Override
    public void setCancelled(boolean cancelled) { isCancelled = cancelled; }
}
