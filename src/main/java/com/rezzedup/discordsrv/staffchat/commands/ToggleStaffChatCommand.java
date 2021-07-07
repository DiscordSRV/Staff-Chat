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

public class ToggleStaffChatCommand implements CommandExecutor, TabCompleter
{
    private static final Set<String> TOGGLE_AUTO_ALIASES = Set.of("auto", "automatic");
    private static final Set<String> TOGGLE_ON_ALIASES = Set.of("on", "enable", "enabled");
    private static final Set<String> TOGGLE_OFF_ALIASES = Set.of("off", "disable", "disabled");
    private static final Set<String> CHECK_ALIASES = Set.of("check");
    private static final Set<String> HELP_ALIASES = Set.of("help", "usage", "?");
    
    @AggregatedResult
    private static final Set<String> ALL_OPTION_ALIASES =
        Aggregates.set(
            ToggleStaffChatCommand.class,
            TypeCapture.type(String.class),
            Aggregates.matching().all("ALIAS").collections(true)
        );
    
    private final StaffChatPlugin plugin;
    
    public ToggleStaffChatCommand(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        @NullOr String option = (args.length >= 1) ? args[0].toLowerCase(Locale.ROOT) : null;
        
        if (option == null || HELP_ALIASES.contains(option)) { usage(sender, label); }
        else if (TOGGLE_AUTO_ALIASES.contains(option)) { auto(sender); }
        else if (TOGGLE_ON_ALIASES.contains(option)) { on(sender); }
        else if (TOGGLE_OFF_ALIASES.contains(option)) { off(sender); }
        else if (CHECK_ALIASES.contains(option)) { check(sender, args); }
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
    
    }
    
    private @NullOr Player onlyIfPlayer(CommandSender sender)
    {
        if (sender instanceof Player) { return (Player) sender; }
        sender.sendMessage("Only players may run this commands.");
        return null;
    }
    
    private void auto(CommandSender sender)
    {
        @NullOr Player player = onlyIfPlayer(sender);
        if (player == null) { return; }
    }
    
    private void on(CommandSender sender)
    {
        @NullOr Player player = onlyIfPlayer(sender);
        if (player == null) { return; }
    }
    
    private void off(CommandSender sender)
    {
        @NullOr Player player = onlyIfPlayer(sender);
        if (player == null) { return; }
    }
    
    private void check(CommandSender sender, String[] args)
    {
    
    }
}
