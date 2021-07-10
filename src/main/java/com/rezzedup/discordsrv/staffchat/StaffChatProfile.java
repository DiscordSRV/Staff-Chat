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
    
    void automaticStaffChat(boolean enabled);
    
    Optional<Instant> sinceLeftStaffChat();
    
    void receivesStaffChatMessages(boolean enabled);
    
    default boolean automaticStaffChat()
    {
        return sinceEnabledAutoChat().isPresent();
    }
    
    default void toggleAutomaticStaffChat()
    {
        automaticStaffChat(!automaticStaffChat());
    }
    
    default boolean receivesStaffChatMessages()
    {
        return sinceLeftStaffChat().isEmpty();
    }
    
    default Optional<Player> toPlayer()
    {
        return Optional.ofNullable(Bukkit.getPlayer(uuid()));
    }
}
