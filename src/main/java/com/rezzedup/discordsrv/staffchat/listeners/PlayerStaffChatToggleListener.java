package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerStaffChatToggleListener implements Listener
{
    private final Set<UUID> autoChatToggles = new HashSet<>();
    
    private final StaffChatPlugin plugin;
        
    public PlayerStaffChatToggleListener(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    public boolean isChatToggled(Player player) { return autoChatToggles.contains(player.getUniqueId()); }
    
    private void forceToggle(Player player, boolean state)
    {
        if (state)
        {
            autoChatToggles.add(player.getUniqueId());
            player.sendMessage(Strings.colorful(plugin.getConfig().getString("enable-staff-chat")));
            plugin.getDebugger().debug("Enabled automatic staff-chat for player %s", player.getName());
        }
        else
        {
            autoChatToggles.remove(player.getUniqueId());
            player.sendMessage(Strings.colorful(plugin.getConfig().getString("disable-staff-chat")));
            plugin.getDebugger().debug("Disabled automatic staff-chat for player %s", player.getName());
        }
    }
    
    public void toggle(Player player) { forceToggle(player, !isChatToggled(player)); }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameChat(AsyncPlayerChatEvent event)
    {
        if (!isChatToggled(event.getPlayer())) { return; }
        
        if (Permissions.ACCESS.isAllowedBy(event.getPlayer()))
        {
            plugin.getDebugger().debug("Player %s has automatic staff-chat enabled.", event.getPlayer().getName());
            
            plugin.submitFromInGame(event.getPlayer(), event.getMessage());
            event.setCancelled(true);
        }
        else
        {
            plugin.getDebugger().debug(
                "Player %s has automatic staff-chat enabled but they don't have permission to use the staff chat.",
                event.getPlayer().getName()
            );
            
            forceToggle(event.getPlayer(), false);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        
        boolean isNotifiable = plugin.getConfig().getBoolean("notify-staff-chat-enabled-on-join")
            && Permissions.ACCESS.isAllowedBy(player)
            && autoChatToggles.contains(player.getUniqueId());
        
        if (!isNotifiable) { return; }
        
        plugin.getDebugger().debug(
            "Player %s joined: reminding them that they have automatic staff-chat enabled.", 
            event.getPlayer().getName()
        );
        
        plugin.getServer().getScheduler()
            .runTaskLater(
                plugin,
                () -> player.sendMessage(Strings.colorful(
                    plugin.getConfig().getString("staff-chat-enabled-notification")
                )),
                10L
            );
    }
}
