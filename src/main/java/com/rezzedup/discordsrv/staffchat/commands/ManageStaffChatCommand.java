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
package com.rezzedup.discordsrv.staffchat.commands;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.util.constants.Aggregates;
import com.rezzedup.util.constants.annotations.AggregatedResult;
import com.rezzedup.util.constants.types.TypeCapture;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rezzedup.discordsrv.staffchat.util.Strings.colorful;

public class ManageStaffChatCommand implements CommandExecutor, TabCompleter
{
    private static final Set<String> RELOAD_ALIASES = Set.of("reload");
    private static final Set<String> DEBUG_ALIASES = Set.of("debug");
    private static final Set<String> HELP_ALIASES = Set.of("help", "usage", "?");
    
    @AggregatedResult
    private static final Set<String> ALL_OPTION_ALIASES =
        Aggregates.set(
            ManageStaffChatCommand.class,
            TypeCapture.type(String.class),
            Aggregates.matching().all("ALIAS").collections(true)
        );
    
    private final StaffChatPlugin plugin;
    
    public ManageStaffChatCommand(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        @NullOr String option = (args.length >= 1) ? args[0].toLowerCase(Locale.ROOT) : null;
        
        if (option == null || HELP_ALIASES.contains(option)) { usage(sender, label); }
        else if (RELOAD_ALIASES.contains(option)) { reload(sender); }
        else if (DEBUG_ALIASES.contains(option)) { debug(sender); }
        else
        {
            sender.sendMessage(colorful(
                "&9&lDiscordSRV-Staff-Chat&f: &7&oUnknown arguments: " + String.join(" ", args)
            ));
        }
        
        return true;
    }
    
    @Override
    public @NullOr List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        @NullOr List<String> suggestions = null;
        
        if (args.length <= 0)
        {
            suggestions = new ArrayList<>(ALL_OPTION_ALIASES);
        }
        else if (args.length == 1)
        {
            String last = args[0].toLowerCase(Locale.ROOT);
            
            suggestions =
                ALL_OPTION_ALIASES.stream()
                    .filter(option -> option.contains(last))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        
        if (suggestions != null) { suggestions.sort(String.CASE_INSENSITIVE_ORDER); }
        return suggestions;
    }
    
    private void usage(CommandSender sender, String label)
    {
        sender.sendMessage(colorful(
            "&9&lDiscordSRV-Staff-Chat &fv" + plugin.getDescription().getVersion() + " Usage:"
        ));
        
        sender.sendMessage(colorful("&f- &7/staffchat &9Toggle automatic staff chat"));
        sender.sendMessage(colorful("&f- &7/staffchat <message> &9Send a message to staff chat"));
        sender.sendMessage(colorful("&f- &7/" + label.toLowerCase() + " reload &9Reload the config"));
        sender.sendMessage(colorful("&f- &7/" + label.toLowerCase() + " debug &9Toggle debugging"));
        
        if (plugin.debugger().isEnabled()) { sender.sendMessage(colorful("&aDebugging is currently enabled.")); }
        else { sender.sendMessage(colorful("&cDebugging is currently disabled.")); }
    }
    
    private void reload(CommandSender sender)
    {
        plugin.debug(getClass()).log("Reload", () -> "Reloading configs and data...");
        
        plugin.config().reload();
        plugin.messages().reload();
        plugin.data().reload();
        plugin.updater().reload();
        
        sender.sendMessage(colorful("&9&lDiscordSRV-Staff-Chat&f: Reloaded."));
    }
    
    private void debug(CommandSender sender)
    {
        boolean enabled = !plugin.debugger().isEnabled();
        plugin.debugger().setEnabled(enabled);
        
        if (enabled)
        {
            plugin.debugger().schedulePluginStatus(getClass(), "Debug Toggle");
            sender.sendMessage(colorful("&aEnabled debugging."));
            
            if (sender instanceof Player)
            {
                sender.sendMessage("[Debug] Sending a test message...");
                plugin.sync().delay(10).ticks().run(() ->
                    plugin.getServer().dispatchCommand(sender, "staffchat Hello! Just testing things...")
                );
            }
        }
        else
        {
            sender.sendMessage(colorful("&cDisabled debugging."));
        }
    }
}
