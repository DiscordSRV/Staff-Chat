/*
 * The MIT License
 * Copyright © 2017-2022 RezzedUp and Contributors
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
import com.rezzedup.discordsrv.staffchat.util.Strings;
import community.leaf.eventful.bukkit.CancellationPolicy;
import community.leaf.eventful.bukkit.ListenerOrder;
import community.leaf.eventful.bukkit.annotations.CancelledEvents;
import community.leaf.eventful.bukkit.annotations.EventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@SuppressWarnings("unused")
public class PlayerPrefixedMessageListener implements Listener
{
    private final StaffChatPlugin plugin;
    
    public PlayerPrefixedMessageListener(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @EventListener(ListenerOrder.EARLY)
    @CancelledEvents(CancellationPolicy.REJECT)
    public void onPrefixedChatEarly(AsyncPlayerChatEvent event)
    {
        if (!plugin.config().getOrDefault(StaffChatConfig.PREFIXED_CHAT_ENABLED)) { return; }
        
        Player player = event.getPlayer();
        if (Permissions.ACCESS.denies(player)) { return; }
        
        String identifier = plugin.config().getOrDefault(StaffChatConfig.PREFIXED_CHAT_IDENTIFIER);
        if (Strings.isEmptyOrNull(identifier))
        {
            plugin.debug(getClass()).log(event, () -> "Early Listener: Prefixed chat is enabled but identifier is undefined");
            return;
        }
        
        String message = event.getMessage();
        if (!message.startsWith(identifier)) { return; }
        
        plugin.debug(getClass()).log(event, () ->
            "Early Listener: Identified prefixed chat from player(" + player.getName() + ") identified " +
            "by prefix(\"" + identifier + "\"): full-message(\"" + message + "\")"
        );
        
        event.setCancelled(true); // Cancel this message from getting sent to global chat.
        // Handle message in a later listener order, allowing other plugins to modify the message.
    }

    @EventListener(ListenerOrder.MONITOR)
    public void onPrefixedChatMonitor(AsyncPlayerChatEvent event)
    {
        // Event should already be cancelled in the early listener.
        if (!event.isCancelled()) { return; }
        
        if (!plugin.config().getOrDefault(StaffChatConfig.PREFIXED_CHAT_ENABLED)) { return; }

        Player player = event.getPlayer();
        if (Permissions.ACCESS.denies(player)) { return; }

        String identifier = plugin.config().getOrDefault(StaffChatConfig.PREFIXED_CHAT_IDENTIFIER);
        if (Strings.isEmptyOrNull(identifier))
        {
            plugin.debug(getClass()).log(event, () -> "Monitor Listener: Prefixed chat is enabled but identifier is undefined");
            return;
        }

        String message = event.getMessage();
        if (!message.startsWith(identifier)) { return; }

        String unprefixed = message.substring(identifier.length()).trim();
        String submission = (Strings.isEmptyOrNull(unprefixed)) ? message : unprefixed;

        plugin.debug(getClass()).log(event, () ->
            "Monitor Listener: Sending prefixed chat from player(" + player.getName() + ") identified " +
            "by prefix(\"" + identifier + "\"): message(\"" + submission + "\")"
        );

        // Handle this on the main thread next tick.
        plugin.sync().run(() -> plugin.submitMessageFromPlayer(player, submission));
    }
}
