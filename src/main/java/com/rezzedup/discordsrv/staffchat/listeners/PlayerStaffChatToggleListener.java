package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.MessagesConfig;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import community.leaf.configvalues.bukkit.DefaultYamlValue;
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
    public void onAutomaticChat(AsyncPlayerChatEvent event)
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
        
        plugin.debug(getClass()).log(event, () -> {
            String name = (player == null) ? "<Offline>" : player.getName();
            String enabled = (event.isEnablingAutomaticChat()) ? "Enabled" : "Disabled";
            return enabled + " automatic staff-chat for player: " + name + " (" + event.getProfile().uuid() + ")";
        });
        
        if (player == null || event.isQuiet()) { return; }
    
        DefaultYamlValue<String> message =
            (event.isEnablingAutomaticChat())
                ? MessagesConfig.AUTO_ENABLED_NOTIFICATION
                : MessagesConfig.AUTO_DISABLED_NOTIFICATION;
        
        player.sendMessage(Strings.colorful(plugin.messages().getOrDefault(message)));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggleReceivingMessages(ReceivingStaffChatToggleEvent event)
    {
        @NullOr Player player = event.getProfile().toPlayer().orElse(null);
        
        plugin.debug(getClass()).log(event, () -> {
            String name = (player == null) ? "<Offline>" : player.getName();
            String left = (event.isLeavingStaffChat()) ? "left" : "joined";
            return "Player: " + name + " (" + event.getProfile().uuid() + ") " + left + " the staff-chat";
        });
        
        if (player == null || event.isQuiet()) { return; }
        
        DefaultYamlValue<String> self =
            (event.isLeavingStaffChat())
                ? MessagesConfig.LEFT_CHAT_NOTIFICATION_SELF
                : MessagesConfig.JOIN_CHAT_NOTIFICATION_SELF;
        
        player.sendMessage(Strings.colorful(plugin.messages().getOrDefault(self)));
        
        DefaultYamlValue<String> others =
            (event.isLeavingStaffChat())
                ? MessagesConfig.LEFT_CHAT_NOTIFICATION_OTHERS
                : MessagesConfig.JOIN_CHAT_NOTIFICATION_OTHERS;
        
        String notification = Strings.colorful(plugin.messages().getOrDefault(others));
        
        plugin.getServer().getOnlinePlayers().stream()
            .filter(Permissions.ACCESS::allows)
            .filter(p -> !p.equals(player))
            .forEach(staff -> staff.sendMessage(notification));
    }
}
