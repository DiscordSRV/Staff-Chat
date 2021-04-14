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

@SuppressWarnings("unused")
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
            
            plugin.debug(getClass()).log("Toggle", () ->
                "Enabled automatic staff-chat for player: " + player.getName()
            );
        }
        else
        {
            autoChatToggles.remove(player.getUniqueId());
            player.sendMessage(Strings.colorful(plugin.getConfig().getString("disable-staff-chat")));
            
            plugin.debug(getClass()).log("Toggle", () ->
                "Disabled automatic staff-chat for player: " + player.getName()
            );
        }
    }
    
    public void toggle(Player player) { forceToggle(player, !isChatToggled(player)); }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameChat(AsyncPlayerChatEvent event)
    {
        if (!isChatToggled(event.getPlayer())) { return; }
        
        if (Permissions.ACCESS.isAllowedBy(event.getPlayer()))
        {
            plugin.debug(getClass()).log(event, () ->
                "Player " + event.getPlayer().getName() + " has automatic staff-chat enabled"
            );
            
            event.setCancelled(true); // Cancel this message from getting sent to global chat.
            
            // Handle this on the main thread next tick.
            plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.submitMessageFromInGame(event.getPlayer(), event.getMessage())
            );
        }
        else
        {
            plugin.debug(getClass()).log(event, () ->
                "Player " + event.getPlayer().getName() + " has automatic staff-chat enabled " +
                "but they don't have permission to use the staff chat"
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
        
        plugin.debug(getClass()).log(event, () ->
            "Player " + event.getPlayer().getName() + " joined: " +
            "reminding them that they have automatic staff-chat enabled"
        );
        
        plugin.getServer().getScheduler()
            .runTaskLater(
                plugin,
                () -> player.sendMessage(Strings.colorful(plugin.getConfig(), "staff-chat-enabled-notification")),
                10L
            );
    }
}
