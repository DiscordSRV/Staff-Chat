package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.placeholders.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.placeholders.Placeholder;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
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
    
    private final Set<UUID> toggles = new HashSet<>();
    
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
        
        toggles.stream()
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
    
    public void updateThenAnnounce(String format, Placeholder placeholder)
    {
        // If the value of %message% doesn't exist for some reason, don't announce.
        if ("%message%".equals(placeholder.update("%message%"))) { return; }
        announce(placeholder.update(format));
    }
    
    public void submitFromInGame(Player player, String message)
    {
        updateThenAnnounce(getConfig().getString("in-game-message-format"), new MessagePlaceholder(player, message));
    
        if (getDiscordChannel() != null)
        {
            DiscordSRV.getPlugin().processChatMessage(player, message, CHANNEL, false);
        }
    }
    
    public void submitFromDiscord(User user, String message)
    {
        updateThenAnnounce(getConfig().getString("discord-message-format"), new MessagePlaceholder(user, message));
    }
    
    private void forceToggle(Player player, boolean state)
    {
        if (state)
        {
            toggles.add(player.getUniqueId());
            player.sendMessage("Enabled automatic staff chat");
        }
        else if (toggles.remove(player.getUniqueId()))
        {
            player.sendMessage("Disabled automatic staff chat");
        }
    }
    
    private void toggle(Player player)
    {
        forceToggle(player, !toggles.contains(player.getUniqueId()));
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
            submitFromInGame(player, String.join(" ", args));
        }
        
        return true;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameChat(AsyncPlayerChatEvent event)
    {
        if (toggles.contains(event.getPlayer().getUniqueId()))
        {
            submitFromInGame(event.getPlayer(), event.getMessage());
            event.setCancelled(true);
        }
    }
    
    @Subscribe
    public void onDiscordChat(DiscordGuildMessagePreProcessEvent event)
    {
        if (event.getChannel().equals(getDiscordChannel()))
        {
            event.setCancelled(true);
            
            if (StringUtils.isNotBlank(event.getMessage().getRawContent()))
            {
                submitFromDiscord(event.getAuthor(), event.getMessage().getContent());
            }
        }
    }
    
    public class MessagePlaceholder extends MappedPlaceholder
    {
        MessagePlaceholder(Player player, String message)
        {
            map("message", "content", "text").to(() -> message);
            map("user", "name", "username", "player", "sender").to(player::getName);
            map("nickname", "displayname").to(player::getDisplayName);
        }
        
        MessagePlaceholder(User user, String message)
        {
            map("message", "content", "text").to(() -> message);
            map("user", "name", "username", "sender").to(user::getName);
            map("discriminator", "discrim").to(user::getDiscriminator);
        }
    }
}
