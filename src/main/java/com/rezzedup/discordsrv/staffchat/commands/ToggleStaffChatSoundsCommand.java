/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
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
package com.rezzedup.discordsrv.staffchat.commands;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.StaffChatProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleStaffChatSoundsCommand implements CommandExecutor
{
    private final StaffChatPlugin plugin;
    
    public ToggleStaffChatSoundsCommand(StaffChatPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            StaffChatProfile profile = plugin.data().getOrCreateProfile(player);
            boolean toggle = !profile.receivesStaffChatSounds();
            profile.receivesStaffChatSounds(toggle);
            
            plugin.debug(getClass()).log(() -> String.format(
                "Player: %s (%s) has %s receiving staff chat sounds",
                player.getName(), profile.uuid(), ((toggle) ? "enabled (unmuted)" : "disabled (muted)")
            ));
            
            if (toggle) { plugin.messages().notifySoundsUnmuted(player); }
            else { plugin.messages().notifySoundsMuted(player); }
        }
        else
        {
            sender.sendMessage("Only players may run this command.");
        }
        
        return true;
    }
}
