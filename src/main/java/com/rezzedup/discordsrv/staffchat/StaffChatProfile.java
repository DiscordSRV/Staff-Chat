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
    
    void enableAutoChat(boolean enabled);
    
    Optional<Instant> sinceLeftStaffChat();
    
    void receiveStaffChat(boolean enabled);
    
    default boolean enableAutoChat()
    {
        return sinceEnabledAutoChat().isPresent();
    }
    
    default boolean receiveStaffChat()
    {
        return sinceLeftStaffChat().isEmpty();
    }
    
    default Optional<Player> toPlayer()
    {
        return Optional.ofNullable(Bukkit.getPlayer(uuid()));
    }
}
