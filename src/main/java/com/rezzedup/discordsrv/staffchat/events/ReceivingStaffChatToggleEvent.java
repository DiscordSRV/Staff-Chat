package com.rezzedup.discordsrv.staffchat.events;

import com.rezzedup.discordsrv.staffchat.StaffChatProfile;
import org.bukkit.event.HandlerList;

public class ReceivingStaffChatToggleEvent extends ProfileToggleEvent
{
    public ReceivingStaffChatToggleEvent(StaffChatProfile profile, boolean toggleState)
    {
        super(profile, toggleState);
    }
    
    public boolean isJoiningStaffChat() { return getToggleState(); }
    
    public boolean isLeavingStaffChat() { return !getToggleState(); }
    
    //
    //  - - - HandlerList boilerplate - - -
    //
    
    public static final HandlerList HANDLERS = new HandlerList();
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    
    public static HandlerList getHandlerList() { return HANDLERS; }
}