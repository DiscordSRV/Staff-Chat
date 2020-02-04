package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.events.DiscordStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.PlayerStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordStaffChatListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerStaffChatToggleListener;
import com.rezzedup.discordsrv.staffchat.util.Events;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import com.vdurmont.emoji.EmojiParser;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

import static com.rezzedup.discordsrv.staffchat.util.Strings.colorful;

public class StaffChatPlugin extends JavaPlugin implements StaffChatAPI
{
    public static final String CHANNEL = "staff-chat";
         
    private boolean isDiscordSrvHookEnabled = false;
    private Debugger debugger;
    private DiscordStaffChatListener discordChatListener;
    private PlayerStaffChatToggleListener inGameToggles;
    
    @Override
    public void onEnable()
    {
        this.debugger = new Debugger(this);
        this.discordChatListener = new DiscordStaffChatListener(this);
        this.inGameToggles = new PlayerStaffChatToggleListener(this);
    
        debugger.debug("----- Starting Plugin: v%s -----", getDescription().getVersion());
        
        getServer().getPluginManager().registerEvents(inGameToggles, this);
        saveDefaultConfig();
        checkForDiscordSrvThenSubscribe();
    }
    
    @Override
    public void onDisable()
    {
        debugger.debug("Disabling plugin...");
        
        getServer().getOnlinePlayers().stream().filter(inGameToggles::isChatToggled).forEach(inGameToggles::toggle);
        
        if (isDiscordSrvHookEnabled)
        {
            debugger.debug("Unsubscribing from DiscordSRV's API.");
            
            try { DiscordSRV.api.unsubscribe(discordChatListener); }
            catch (RuntimeException ignored) {} // Don't show a user-facing error if DiscordSRV is already unloaded.
        }
        
        debugger.debug("----- Disabled. -----");
    }
    
    public Debugger getDebugger() { return debugger; }
    
    private void checkForDiscordSrvThenSubscribe()
    {
        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV"))
        {
            debugger.debug("DiscordSRV is enabled.");
        
            if (!isDiscordSrvHookEnabled)
            {
                debugger.debug("Subscribing to DiscordSRV's API...");
                
                this.isDiscordSrvHookEnabled = true;
                DiscordSRV.api.subscribe(discordChatListener);
            }
        }
        else
        {
            debugger.debug("DiscordSRV is not enabled. Will continue without discord support.");
            
            getLogger().warning("DiscordSRV is not currently enabled (messages will not be sent to Discord).");
            getLogger().warning("Staff chat messages will still work in-game, however.");
        }
    }
    
    @Override
    public boolean isDiscordSrvHookEnabled() { return isDiscordSrvHookEnabled; }
    
    @Override
    public TextChannel getDiscordChannelOrNull()
    {
        return (isDiscordSrvHookEnabled) 
            ? DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(CHANNEL) : null;
    }
    
    private void inGameAnnounce(String message)
    {
        String content = colorful(message);
        
        getServer().getOnlinePlayers().stream()
            .filter(Permissions.ACCESS::isAllowedBy)
            .forEach(staff -> staff.sendMessage(content));
        
        getServer().getConsoleSender().sendMessage(content);
    }
    
    private void updatePlaceholdersThenAnnounceInGame(String format, MappedPlaceholder placeholders)
    {
        // If the value of %message% doesn't exist for some reason, don't announce.
        if (Strings.isEmptyOrNull(placeholders.get("message"))) { return; }
        inGameAnnounce(placeholders.update(format));
    }
    
    @Override
    public void submitMessageFromInGame(Player author, String message)
    {
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(message, "message");
        
        debugger.debug("[In-Game-Message] From:\"%s\" Message:\"%s\"", author.getName(), message);
    
        PlayerStaffChatMessageEvent event =
            Events.call(new PlayerStaffChatMessageEvent(author, message));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            debugger.debug("[In-Game-Message] Cancelled or text is empty.");
            return;
        }
        
        String text = event.getText();
        String format = getConfig().getString("in-game-message-format");
        
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
        {
            // Update format's PAPI placeholders before inserting the message
            // (which *could* contain arbitrary placeholders itself, ah placeholder injection).
            format = PlaceholderAPI.setPlaceholders(author, format);
        }
        
        MappedPlaceholder placholders = new MappedPlaceholder();
        
        placholders.map("message", "content", "text").to(() -> text);
        placholders.map("user", "name", "username", "player", "sender").to(author::getName);
        placholders.map("nickname", "displayname").to(author::getDisplayName);
        
        updatePlaceholdersThenAnnounceInGame(format, placholders);
        
        if (getDiscordChannelOrNull() != null)
        {
            debugger.debug("Sending message to discord channel: %s => %s", CHANNEL, getDiscordChannelOrNull());
            
            // Send to discord off the main thread (just like DiscordSRV does)
            getServer().getScheduler().runTaskAsynchronously(this, () -> 
                DiscordSRV.getPlugin().processChatMessage(author, message, CHANNEL, false)
            );
        }
        else { debugger.debug("Unable to send message to discord: %s => null", CHANNEL); }
    }
    
    @Override
    public void submitMessageFromDiscord(User author, Message message)
    {
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(message, "message");
        
        debugger.debug(
            "[Discord-Message] From:\"%s#%s\" Channel:\"%s\" Message:\"%s\"",
            author.getName(), author.getDiscriminator(), message.getChannel(), message
        );
        
        DiscordStaffChatMessageEvent event =
            Events.call(new DiscordStaffChatMessageEvent(author, message, message.getContentStripped()));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            debugger.debug("[Discord-Message] Cancelled or text is empty.");
            return;
        }
    
        // Emoji Unicode -> Alias (library included with DiscordSRV)
        String text = EmojiParser.parseToAliases(event.getText());
        String format = getConfig().getString("discord-message-format");
        
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
        {
            // Update format's PAPI placeholders before inserting the message.
            format = PlaceholderAPI.setPlaceholders(null, format);
        }
        
        MappedPlaceholder placholders = new MappedPlaceholder();
        
        placholders.map("message", "content", "text").to(() -> text);
        placholders.map("user", "name", "username", "sender").to(author::getName);
        placholders.map("nickname", "displayname").to(message.getGuild().getMember(author)::getNickname);
        placholders.map("discriminator", "discrim").to(author::getDiscriminator);
        
        updatePlaceholdersThenAnnounceInGame(format, placholders);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if ("staffchat".equals(command.getName()))
        {
            if (sender instanceof ConsoleCommandSender)
            {
                sender.sendMessage("Only players may use this command.");
                return true;
            }
            
            Player player = (Player) sender;
            
            if (args.length <= 0) { inGameToggles.toggle(player); }
            else { submitMessageFromInGame(player, String.join(" ", args)); }
        }
        else if ("managestaffchat".equals(command.getName()))
        {
            if (args.length <= 0)
            {
                sender.sendMessage(colorful(
                    "&9&lDiscordSRV-Staff-Chat &fv" + getDescription().getVersion() + " Usage:"
                ));
                
                sender.sendMessage(colorful("&f- &7/staffchat &9Toggle automatic staff chat"));
                sender.sendMessage(colorful("&f- &7/staffchat <message> &9Send a message to staff chat"));
                sender.sendMessage(colorful("&f- &7/" + label.toLowerCase() + " reload &9Reload the config"));
                sender.sendMessage(colorful("&f- &7/" + label.toLowerCase() + " debug &9Toggle debugging"));
                
                if (debugger.isEnabled()) { sender.sendMessage(colorful("&aDebugging is currently enabled.")); }
                else { sender.sendMessage(colorful("&cDebugging is currently disabled.")); }
                return true;
            }
            
            switch (args[0].toLowerCase())
            {
                case "reload": case "refresh": case "restart":
                {
                    debugger.debug("Reloading config...");
                    reloadConfig();
                    checkForDiscordSrvThenSubscribe();
                    sender.sendMessage(colorful("&9&lDiscordSRV-Staff-Chat&f: Reloaded."));
                    break;
                }
                
                case "debug":
                {
                    debugger.toggle();
                    
                    if (debugger.isEnabled())
                    {
                        sender.sendMessage(colorful("&aEnabled debugging."));
                        
                        if (sender instanceof Player)
                        {
                            sender.sendMessage("Sending a test message...");
                            getServer().dispatchCommand(sender, "staffchat Hello! Just testing things...");
                        }
                    }
                    else { sender.sendMessage(colorful("&cDisabled debugging.")); }
                    break;
                }
                
                case "help": case "?":
                {
                    onCommand(sender, command, label, new String[0]);
                    break;
                }
                
                default:
                {
                    sender.sendMessage(colorful("&9&lDiscordSRV-Staff-Chat&f: &7&oUnknown arguments."));
                    break;
                }
            }
        }
        return true;
    }
}
