package com.rezzedup.discordsrv.staffchat.events;

import com.rezzedup.discordsrv.staffchat.ChatService;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@SuppressWarnings("unused")
public class PlayerStaffChatMessageEvent extends StaffChatMessageEvent<Player, String>
{
    public PlayerStaffChatMessageEvent(Player author, String text)
    {
        super(author, text, text);
    }
    
    @Override
    public final ChatService getSource() { return ChatService.MINECRAFT; }
    
    @Override
    public final ChatService getDestination() { return ChatService.DISCORD; }
    
    //
    //  - - - HandlerList boilerplate - - -
    //
    
    public static final HandlerList HANDLERS = new HandlerList();
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    
    public static HandlerList getHandlerList() { return HANDLERS; }
}
