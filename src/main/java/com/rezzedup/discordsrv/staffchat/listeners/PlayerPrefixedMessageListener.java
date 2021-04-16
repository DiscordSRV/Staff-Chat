package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@SuppressWarnings("unused")
public class PlayerPrefixedMessageListener implements Listener
{
    private final StaffChatPlugin plugin;
    
    public PlayerPrefixedMessageListener(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPrefixedChat(AsyncPlayerChatEvent event)
    {
        if (!plugin.getConfig().getBoolean("enable-prefixed-chat-messages", false)) { return; }
        
        Player player = event.getPlayer();
        if (Permissions.ACCESS.denies(player)) { return; }
        
        String identifier = Strings.orEmpty(plugin.getConfig(), "prefixed-chat-identifier");
        if (Strings.isEmptyOrNull(identifier))
        {
            plugin.debug(getClass()).log(event, () -> "Prefixed chat is enabled but identifier is undefined");
            return;
        }
        
        String message = event.getMessage();
        if (!message.startsWith(identifier)) { return; }
    
        String unprefixed = message.substring(identifier.length()).trim();
        
        plugin.debug(getClass()).log(event, () ->
            "Sending prefixed chat from player(" + player.getName() + ") identified " +
            "by prefix(\"" + identifier + "\"): message(\"" + unprefixed + "\")"
        );
        
        event.setCancelled(true); // Cancel this message from getting sent to global chat.
        
        // Handle this on the main thread next tick.
        plugin.getServer().getScheduler().runTask(plugin, () ->
            plugin.submitMessageFromInGame(player, (Strings.isEmptyOrNull(unprefixed)) ? message : unprefixed)
        );
    }
}
