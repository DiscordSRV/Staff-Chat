package com.rezzedup.discordsrv.staffchat.events;

import com.rezzedup.discordsrv.staffchat.StaffChatProfile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.Objects;

public abstract class ProfileToggleEvent extends Event implements Cancellable
{
    private final StaffChatProfile profile;
    private final boolean toggleState;
    
    private boolean isQuiet = false;
    
    public ProfileToggleEvent(StaffChatProfile profile, boolean toggleState)
    {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.toggleState = toggleState;
    }
    
    public StaffChatProfile getProfile() { return profile; }
    
    public boolean getToggleState() { return toggleState; }
    
    public boolean isQuiet() { return isQuiet; }
    
    public void setQuiet(boolean quiet) { this.isQuiet = quiet; }
    
    //
    //  - - - Cancellable boilerplate - - -
    //
    
    private boolean isCancelled = false;
    
    @Override
    public final boolean isCancelled() { return isCancelled; }
    
    @Override
    public final void setCancelled(boolean cancelled) { isCancelled = cancelled; }
}
