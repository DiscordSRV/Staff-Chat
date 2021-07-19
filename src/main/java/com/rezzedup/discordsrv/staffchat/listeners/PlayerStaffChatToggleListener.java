/*
 * The MIT License
 * Copyright © 2017-2021 RezzedUp and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingStaffChatToggleEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
        if (!plugin.data().isAutomaticStaffChatEnabled(player)) { return; }
        
        event.setCancelled(true); // Cancel this message from getting sent to global chat.
        
        if (Permissions.ACCESS.allows(player))
        {
            plugin.debug(getClass()).log(event, () ->
                "Player " + player.getName() + " has automatic staff-chat enabled"
            );
            
            // Handle this on the main thread next tick.
            plugin.sync().run(() -> plugin.submitMessageFromPlayer(event.getPlayer(), event.getMessage()));
        }
        else
        {
            plugin.debug(getClass()).log(event, () ->
                "Player " + player.getName() + " has automatic staff-chat enabled " +
                "but they don't have permission to use the staff chat"
            );
            
            // Remove this non-staff profile (but in sync 'cus it calls an event).
            plugin.sync().run(() -> {
                plugin.data().updateProfile(player);
                player.chat(event.getMessage());
            });
        }
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
    
        if (event.isEnablingAutomaticChat()) { plugin.messages().notifyAutoChatEnabled(player); }
        else { plugin.messages().notifyAutoChatDisabled(player); }
    }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLeavingStaffChatIsDisabled(ReceivingStaffChatToggleEvent event)
    {
        if (event.isJoiningStaffChat()) { return; }
        if (plugin.config().getOrDefault(StaffChatConfig.LEAVING_STAFFCHAT_ENABLED)) { return; }
        
        // Leaving is disabled, cancel the event.
        event.setCancelled(true);
        
        @NullOr Player player = event.getProfile().toPlayer().orElse(null);
        
        plugin.debug(getClass()).log(event, () -> {
            String name = (player == null) ? "<Offline>" : player.getName();
            return "Player: " + name + " (" + event.getProfile().uuid() + ") " +
                   "tried to leave the staff chat, but leaving is disabled in the config";
        });
        
        if (player == null || event.isQuiet()) { return; }
        
        plugin.messages().notifyLeavingChatIsDisabled(player);
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
        
        boolean broadcastToEveryone =
            event.getProfile().sinceLeftStaffChat().isPresent() != event.isLeavingStaffChat();
        
        if (event.isLeavingStaffChat()) { plugin.messages().notifyLeaveChat(player, broadcastToEveryone); }
        else { plugin.messages().notifyJoinChat(player, broadcastToEveryone); }
    }
}
