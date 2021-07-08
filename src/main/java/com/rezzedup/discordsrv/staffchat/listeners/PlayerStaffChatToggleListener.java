package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.ToggleData;
import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.MessagesConfig;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

@SuppressWarnings("unused")
public class PlayerStaffChatToggleListener implements Listener
{
    private final StaffChatPlugin plugin;
    private final ToggleData toggles;
    
    public PlayerStaffChatToggleListener(StaffChatPlugin plugin)
    {
        this.plugin = plugin;
        this.toggles = plugin.toggles();
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onToggledChat(AsyncPlayerChatEvent event)
    {
        Player player = event.getPlayer();
        if (!toggles.isChatAutomatic(player)) { return; }
        
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
            
            toggles.setAutoChatToggle(event.getPlayer(), false);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        
        boolean isNotifiable =
            plugin.config().getOrDefault(StaffChatConfig.NOTIFY_IF_TOGGLE_ENABLED)
                && Permissions.ACCESS.allows(player)
                && toggles.isChatAutomatic(player);
        
        if (!isNotifiable) { return; }
        
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
}
