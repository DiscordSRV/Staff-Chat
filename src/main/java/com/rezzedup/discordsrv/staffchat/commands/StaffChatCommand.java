package com.rezzedup.discordsrv.staffchat.commands;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class StaffChatCommand implements CommandExecutor
{
    private final StaffChatPlugin plugin;
    
    public StaffChatCommand(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof ConsoleCommandSender)
        {
            sender.sendMessage("Only players may use this command: " + label);
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length <= 0)
        {
            plugin.data().getOrCreateProfile(player).toggleAutomaticStaffChat();
        }
        else
        {
            plugin.submitMessageFromInGame(player, String.join(" ", args));
        }
        
        return true;
    }
}
