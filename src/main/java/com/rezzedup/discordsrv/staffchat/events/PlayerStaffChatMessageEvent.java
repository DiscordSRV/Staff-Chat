package com.rezzedup.discordsrv.staffchat.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

@SuppressWarnings("unused")
public class PlayerStaffChatMessageEvent extends Event implements Cancellable
{
    private final Player author;
    private String text;
    
    public PlayerStaffChatMessageEvent(Player author, String text)
    {
        this.author = Objects.requireNonNull(author, "author");
        this.text = Objects.requireNonNull(text, "text");
    }
    
    public Player getAuthor() { return author; }
    
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
