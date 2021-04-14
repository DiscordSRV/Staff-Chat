package com.rezzedup.discordsrv.staffchat.events;

import com.rezzedup.discordsrv.staffchat.ChatService;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.Objects;

public abstract class StaffChatMessageEvent<A, M> extends Event implements Cancellable
{
    private final A author;
    private final M message;
    private String text;
    
    public StaffChatMessageEvent(A author, M message, String text)
    {
        this.author = Objects.requireNonNull(author, "author");
        this.message = Objects.requireNonNull(message, "message");
        this.text = Objects.requireNonNull(text, "text");
    }
    
    public abstract ChatService getSource();
    
    public abstract ChatService getDestination();
    
    public final A getAuthor() { return author; }
    
    public final M getMessage() { return message; }
    
    public final String getText() { return text; }
    
    public final void setText(String text) { this.text = Objects.requireNonNull(text, "text"); }
    
    //
    //  - - - Cancellable boilerplate - - -
    //
    
    private boolean isCancelled = false;
    
    @Override
    public final boolean isCancelled() { return isCancelled; }
    
    @Override
    public final void setCancelled(boolean cancelled) { isCancelled = cancelled; }
}
