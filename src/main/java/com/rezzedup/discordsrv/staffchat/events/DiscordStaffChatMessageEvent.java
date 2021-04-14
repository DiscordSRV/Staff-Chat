package com.rezzedup.discordsrv.staffchat.events;

import com.rezzedup.discordsrv.staffchat.ChatService;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bukkit.event.HandlerList;

@SuppressWarnings("unused")
public class DiscordStaffChatMessageEvent extends StaffChatMessageEvent<User, Message>
{
    public DiscordStaffChatMessageEvent(User author, Message message, String text)
    {
        super(author, message, text);
    }
    
    @Override
    public final ChatService getSource() { return ChatService.DISCORD; }
    
    @Override
    public final ChatService getDestination() { return ChatService.MINECRAFT; }
    
    //
    //  - - - HandlerList boilerplate - - -
    //
    
    public static final HandlerList HANDLERS = new HandlerList();
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    
    public static HandlerList getHandlerList() { return HANDLERS; }
}
