package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.placeholders.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.placeholders.Placeholder;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.core.entities.Message;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class StaffChatPlugin extends JavaPlugin implements Listener
{
    public static final String CHANNEL = "staff-chat";
    
    public static String color(String message)
    {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
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
        toggles.stream().map(getServer()::getPlayer).filter(Objects::nonNull).forEach(this::toggle);
        DiscordSRV.api.unsubscribe(this);
    }
    
    public TextChannel getDiscordChannel()
    {
        return DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(CHANNEL);
    }
    
    public void announce(String message)
    {
        String content = color(message);
    
        getServer().getConsoleSender().sendMessage(content);
        
        getServer().getOnlinePlayers().stream()
            .filter(Permissions.any(Permissions.ALL, Permissions.ACCESS)).forEach(p -> p.sendMessage(content));
    }
    
    public void updateThenAnnounce(String format, MappedPlaceholder placeholder)
    {
        // If the value of %message% doesn't exist for some reason, don't announce.
        if (Placeholder.isValid(placeholder.get("message"))) 
        {
            announce(placeholder.update(format));
        }
    }
    
    public void submitFromInGame(Player player, String message)
    {
        updateThenAnnounce(getConfig().getString("in-game-message-format"), new MessagePlaceholder(player, message));
    
        if (getDiscordChannel() != null)
        {
            DiscordSRV.getPlugin().processChatMessage(player, message, CHANNEL, false);
        }
    }
    
    public void submitFromDiscord(User user, Message message)
    {
        updateThenAnnounce(getConfig().getString("discord-message-format"), new MessagePlaceholder(user, message));
    }
    
    private void forceToggle(Player player, boolean state)
    {
        if (state)
        {
            toggles.add(player.getUniqueId());
            player.sendMessage(color(getConfig().getString("enable-staff-chat")));
        }
        else
        {
            toggles.remove(player.getUniqueId());
            player.sendMessage(color(getConfig().getString("disable-staff-chat")));
        }
    }
    
    private void toggle(Player player)
    {
        forceToggle(player, !toggles.contains(player.getUniqueId()));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // /staffchat
        if ("staffchat".equals(command.getName()))
        {
            if (sender instanceof ConsoleCommandSender)
            {
                sender.sendMessage("Only players may use this command.");
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
        }
        // /managestaffchat
        else
        {
            if (!Permissions.any(Permissions.ALL, Permissions.MANAGE).test(sender))
            {
                sender.sendMessage("You don't have permission to use that.");
            }
            else if (args.length <= 0)
            {
                sender.sendMessage(color("&9&lDiscord-Staff-Chat &fv" + getDescription().getVersion() + " Usage:"));
                sender.sendMessage(color("&f- &7/staffchat &9Toggle automatic staff chat"));
                sender.sendMessage(color("&f- &7/staffchat <message> &9Send a message to staff chat"));
                sender.sendMessage(color("&f- &7/" + label.toLowerCase() + " reload &9Reload the config"));
            }
            else 
            {
                switch (args[0].toLowerCase())
                {
                    case "reload":
                    case "refresh":
                    case "restart":
                        reloadConfig();
                        sender.sendMessage(color("&dReloaded."));
                        break;
                        
                    case "help":
                    case "?":
                        onCommand(sender, command, label, new String[0]);
                        break;
                        
                    default:
                        sender.sendMessage(color("&6Unknown arguments."));
                        break;
                }
            }
        }
        return true;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameChat(AsyncPlayerChatEvent event)
    {
        if (toggles.contains(event.getPlayer().getUniqueId()))
        {
            if (Permissions.any(Permissions.ALL, Permissions.ACCESS).test(event.getPlayer()))
            {
                submitFromInGame(event.getPlayer(), event.getMessage());
                event.setCancelled(true);
            }
            else 
            {
                forceToggle(event.getPlayer(), false);
            }
        }
    }
    
    @Subscribe
    public void onDiscordChat(DiscordGuildMessagePreProcessEvent event)
    {
        if (event.getChannel().equals(getDiscordChannel()))
        {
            submitFromDiscord(event.getAuthor(), event.getMessage());
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        
        boolean isNotifiable = getConfig().getBoolean("notify-staff-chat-enabled-on-join")
            && Permissions.any(Permissions.ALL, Permissions.ACCESS).test(player)
            && toggles.contains(player.getUniqueId());
        
        if (!isNotifiable) { return; }
        
        getServer().getScheduler()
            .runTaskLater(this, () -> player.sendMessage(color(getConfig().getString("staff-chat-enabled-notification"))), 10L);
    }
    
    public class MessagePlaceholder extends MappedPlaceholder
    {
        MessagePlaceholder(Player player, String message)
        {
            map("message", "content", "text").to(() -> message);
            map("user", "name", "username", "player", "sender").to(player::getName);
            map("nickname", "displayname").to(player::getDisplayName);
        }
        
        MessagePlaceholder(User user, Message message)
        {
            map("message", "content", "text").to(message::getContent);
            map("user", "name", "username", "sender").to(user::getName);
            map("nickname", "displayname").to(message.getGuild().getMember(user)::getNickname);
            map("discriminator", "discrim").to(user::getDiscriminator);
        }
    }
}
