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
import community.leaf.eventful.bukkit.ListenerOrder;
import community.leaf.eventful.bukkit.annotations.EventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Deque;

public class JoinNotificationListener implements Listener
{
    private final StaffChatPlugin plugin;
    
    public JoinNotificationListener(StaffChatPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    @EventListener(ListenerOrder.EARLY)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        plugin.data().updateProfile(player);
        
        Deque<Runnable> reminders = new ArrayDeque<>();
        
        if (plugin.config().getOrDefault(StaffChatConfig.NOTIFY_IF_TOGGLE_ENABLED))
        {
            if (Permissions.ACCESS.allows(player))
            {
                if (plugin.data().isAutomaticStaffChatEnabled(player))
                {
                    plugin.debug(getClass()).log(event, () ->
                        "Player " + event.getPlayer().getName() + " joined: " +
                        "reminding them that they have automatic staff-chat enabled"
                    );
                    
                    reminders.add(() -> plugin.messages().notifyAutoChatEnabled(player));
                }
                
                if (!plugin.data().isReceivingStaffChatMessages(player))
                {
                    plugin.debug(getClass()).log(event, () ->
                        "Player " + event.getPlayer().getName() + " joined: " +
                        "reminding them that they previously left the staff-chat"
                    );
                    
                    reminders.add(() -> plugin.messages().notifyLeaveChat(player, false));
                }
            }
        }
        
        if (plugin.config().getOrDefault(StaffChatConfig.NOTIFY_IF_UPDATE_AVAILABLE))
        {
            if (Permissions.MANAGE.allows(player))
            {
                plugin.updater().latestUpdateVersion().ifPresent(version ->
                {
                    plugin.debug(getClass()).log(event, () ->
                        "Player " + event.getPlayer().getName() + " joined: " +
                        "notifying them that a new update is available (" + version + ")"
                    );
                    
                    reminders.add(() -> plugin.messages().notifyUpdateAvailable(player, version));
                });
            }
        }
        
        if (reminders.isEmpty()) { return; }
        
        plugin.sync().delay(10).ticks().every(10).ticks().run(task ->
        {
            if (reminders.isEmpty()) { task.cancel(); }
            else { reminders.pop().run(); }
        });
    }
    
    @EventListener(ListenerOrder.EARLY)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        // Might as well update the profile (cleanup)
        plugin.data().updateProfile(event.getPlayer());
    }
}
