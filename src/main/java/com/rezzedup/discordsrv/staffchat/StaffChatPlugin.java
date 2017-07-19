package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.placeholders.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.placeholders.Placeholder;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.core.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.core.entities.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class StaffChatPlugin extends JavaPlugin implements Listener
{
    public static final String CHANNEL = "staff-chat"; 
    
    private final Set<UUID> automaticStaffChat = new HashSet<>();
    
    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        
        getServer().getPluginManager().registerEvents(this, this);
        
        DiscordSRV.api.subscribe(this);
    }
    
    @Override
    public void onDisable()
    {
        DiscordSRV.api.unsubscribe(this);
        
        automaticStaffChat.stream()
            .map(id -> getServer().getPlayer(id))
            .filter(Objects::nonNull)
            .forEach(p -> p.sendMessage("Disabling staff chat..."));
    }
    
    public TextChannel getDiscordChannel()
    {
        return DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(CHANNEL);
    }
    
    public void announce(String message)
    {
        String content = ChatColor.translateAlternateColorCodes('&', message);
    
        getServer().getConsoleSender().sendMessage(content);
        
        getServer().getOnlinePlayers().stream()
            .filter(Permissions.any(Permissions.ALL, Permissions.ACCESS))
            .forEach(p -> p.sendMessage(content));
    }
    
    public void updateThenAnnounce(Placeholder placeholder)
    {
        announce(placeholder.update(getConfig().getString("message-format")));
    }
    
    public void submit(Player player, String message)
    {
        updateThenAnnounce(new MessagePlaceholder(player, message));
    
        if (getDiscordChannel() != null)
        {
            DiscordSRV.getPlugin().processChatMessage(player, message, CHANNEL, false);
        }
    }
    
    public void submit(User user, String message)
    {
        updateThenAnnounce(new MessagePlaceholder(user, message));
    }
    
    private void toggle(Player player)
    {
        if (automaticStaffChat.remove(player.getUniqueId()))
        {
            player.sendMessage("Disabled automatic staff chat");
        }
        else 
        {
            automaticStaffChat.add(player.getUniqueId());
            player.sendMessage("Enabled automatic staff chat");
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof ConsoleCommandSender)
        {
            sender.sendMessage("Only players may use this command.");
            return true;
        }
        if (!Permissions.any(Permissions.ALL, Permissions.ACCESS).test(sender))
        {
            sender.sendMessage("You don't have permission to use that.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length <= 0)
        {
            toggle(player);
        }
        else
        {
            submit(player, String.join(" ", args));
        }
        
        return true;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameChat(AsyncPlayerChatEvent event)
    {
        if (automaticStaffChat.contains(event.getPlayer().getUniqueId()))
        {
            submit(event.getPlayer(), event.getMessage());
            event.setCancelled(true);
        }
    }
    
    @Subscribe
    public void onDiscordChat(DiscordGuildMessageReceivedEvent event)
    {
        if (event.getChannel().equals(getDiscordChannel()))
        {
            submit(event.getAuthor(), event.getMessage().getContent());
        }
    }
    
    public class MessagePlaceholder extends MappedPlaceholder
    {
        MessagePlaceholder(Player player, String message)
        {
            setup(message);
            
            map("prefix").to(() -> getConfig().getString("prefix"));
            map("user", "name", "username").to(player::getName);
            map("nickname", "displayname").to(player::getDisplayName);
        }
        
        MessagePlaceholder(User user, String message)
        {
            setup(message);
    
            map("prefix").to(() -> getConfig().getString("discord-prefix"));
            map("user", "name", "username", "nickname", "displayname").to(user::getName);
        }
    
        private void setup(String message)
        {
            map("message", "content", "msg", "text").to(() -> message);
        }
    }
}
