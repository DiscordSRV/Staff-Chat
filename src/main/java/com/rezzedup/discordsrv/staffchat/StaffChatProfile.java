package com.rezzedup.discordsrv.staffchat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StaffChatProfile
{
    UUID uuid();
    
    Optional<Instant> sinceEnabledAutoChat();
    
    boolean automaticStaffChat();
    
    void automaticStaffChat(boolean enabled);
    
    Optional<Instant> sinceLeftStaffChat();
    
    boolean receivesStaffChatMessages();
    
    void receivesStaffChatMessages(boolean enabled);
    
    default void toggleAutomaticStaffChat()
    {
        automaticStaffChat(!automaticStaffChat());
    }
    
    default Optional<Player> toPlayer()
    {
        return Optional.ofNullable(Bukkit.getPlayer(uuid()));
    }
}
