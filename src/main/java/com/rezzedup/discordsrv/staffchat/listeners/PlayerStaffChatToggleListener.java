package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.MessagesConfig;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.tlinkowski.annotation.basic.NullOr;

@SuppressWarnings("unused")
public class PlayerStaffChatToggleListener implements Listener
{
    private final StaffChatPlugin plugin;
    
    public PlayerStaffChatToggleListener(StaffChatPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onToggledChat(AsyncPlayerChatEvent event)
    {
        Player player = event.getPlayer();
        if (!plugin.data().isChatAutomatic(player)) { return; }
        
        if (Permissions.ACCESS.allows(player))
        {
            plugin.debug(getClass()).log(event, () ->
                "Player " + player.getName() + " has automatic staff-chat enabled"
            );
            
            event.setCancelled(true); // Cancel this message from getting sent to global chat.
            
            // Handle this on the main thread next tick.
            plugin.sync().run(() -> plugin.submitMessageFromInGame(event.getPlayer(), event.getMessage()));
        }
        else
        {
            plugin.debug(getClass()).log(event, () ->
                "Player " + player.getName() + " has automatic staff-chat enabled " +
                "but they don't have permission to use the staff chat"
            );
            
            // Remove this non-staff profile.
            plugin.data().updateProfile(player);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        plugin.data().updateProfile(player);
        
        if (!plugin.config().getOrDefault(StaffChatConfig.NOTIFY_IF_TOGGLE_ENABLED)) { return; }
        if (!Permissions.ACCESS.allows(player)) { return; }
        if (!plugin.data().isChatAutomatic(player)) { return; }
        
        plugin.debug(getClass()).log(event, () ->
            "Player " + event.getPlayer().getName() + " joined: " +
            "reminding them that they have automatic staff-chat enabled"
        );
        
        plugin.sync().delay(10).ticks().run(() ->
            player.sendMessage(Strings.colorful(
                plugin.messages().getOrDefault(MessagesConfig.AUTO_ENABLED_NOTIFICATION)
            ))
        );
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggleAutoChat(AutoStaffChatToggleEvent event)
    {
        @NullOr Player player = event.getProfile().toPlayer().orElse(null);
        String name = (player == null) ? "<Offline>" : player.getName();
        
        if (event.isEnablingAutomaticChat())
        {
            plugin.debug(getClass()).log(event, () ->
                "Enabled automatic staff-chat for player: " + name + " (" + event.getProfile().uuid() + ")"
            );
            
            if (player != null)
            {
                player.sendMessage(Strings.colorful(
                    plugin.messages().getOrDefault(MessagesConfig.AUTO_ENABLED_NOTIFICATION)
                ));
            }
        }
        else
        {
            plugin.debug(getClass()).log(event, () ->
                "Disabled automatic staff-chat for player: " + name + " (" + event.getProfile().uuid() + ")"
            );
            
            if (player != null)
            {
                player.sendMessage(Strings.colorful(
                    plugin.messages().getOrDefault(MessagesConfig.AUTO_DISABLED_NOTIFICATION)
                ));
            }
        }
    }
}
