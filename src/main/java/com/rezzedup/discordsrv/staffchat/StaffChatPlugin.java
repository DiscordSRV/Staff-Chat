package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.events.DiscordStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.PlayerStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordSrvLoadedLaterListener;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordStaffChatListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerPrefixedMessageListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerStaffChatToggleListener;
import com.rezzedup.discordsrv.staffchat.util.Events;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import community.leaf.tasks.bukkit.BukkitTaskSource;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.emoji.EmojiParser;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.Objects;

import static com.rezzedup.discordsrv.staffchat.util.Strings.colorful;

@SuppressWarnings("NotNullFieldNotInitialized")
public class StaffChatPlugin extends JavaPlugin implements BukkitTaskSource, StaffChatAPI
{
    // https://bstats.org/plugin/bukkit/DiscordSRV-Staff-Chat/11056
    public static final int BSTATS = 11056;
    
    public static final String CHANNEL = "staff-chat";
    
    public static final String DISCORDSRV = "DiscordSRV";
         
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
    
        debug(getClass()).header(() -> "Starting Plugin: " + this);
        debugger().schedulePluginStatus(getClass(), "Enable");
    
        PluginManager plugins = getServer().getPluginManager();
        
        plugins.registerEvents(inGameToggles, this);
        plugins.registerEvents(new PlayerPrefixedMessageListener(this), this);
        
        saveDefaultConfig();
        
        @NullOr Plugin discordSrv = getServer().getPluginManager().getPlugin(DISCORDSRV);
        
        if (discordSrv != null)
        {
            debug(getClass()).log("Enable", () -> "DiscordSRV is enabled");
            subscribeToDiscordSrv(discordSrv);
        }
        else
        {
            debug(getClass()).log("Enable", () -> "DiscordSRV is not enabled: continuing without discord support");
            
            getLogger().warning("DiscordSRV is not currently enabled (messages will NOT be sent to Discord).");
            getLogger().warning("Staff chat messages will still work in-game, however.");
            
            // Subscribe to DiscordSRV later because it somehow wasn't enabled yet.
            getServer().getPluginManager().registerEvents(new DiscordSrvLoadedLaterListener(this), this);
        }
        
        startMetrics();
    }
    
    @Override
    public void onDisable()
    {
        debug(getClass()).log("Disable", () -> "Disabling plugin...");
        
        getServer().getOnlinePlayers().stream().filter(inGameToggles::isChatToggled).forEach(inGameToggles::toggle);
        
        if (isDiscordSrvHookEnabled)
        {
            debug(getClass()).log("Disable", () -> "Unsubscribing from DiscordSRV API (hook is enabled)");
            
            try
            {
                DiscordSRV.api.unsubscribe(discordChatListener);
                this.isDiscordSrvHookEnabled = false;
            }
            catch (RuntimeException ignored) {} // Don't show a user-facing error if DiscordSRV is already unloaded.
        }
        
        debug(getClass()).header(() -> "Disabled Plugin: " + this);
    }
    
    private void startMetrics()
    {
        if (!getConfig().getBoolean("metrics", true))
        {
            debug(getClass()).log("Metrics", () -> "Aborting: metrics are disabled in the config");
            return;
        }
        
        debug(getClass()).log("Metrics", () -> "Scheduling metrics to start one minute from now");
        
        // Start a minute later to get the most accurate data.
        sync().delay(1).minutes().run(() ->
        {
            Metrics metrics = new Metrics(this, BSTATS);
        
            metrics.addCustomChart(new SimplePie(
                "hooked_into_discordsrv", () -> String.valueOf(isDiscordSrvHookEnabled())
            ));
            
            metrics.addCustomChart(new SimplePie(
                "has_valid_staff-chat_channel", () -> String.valueOf(getDiscordChannelOrNull() != null)
            ));
            
            debug(getClass()).log("Metrics", () -> "Started bStats metrics");
        });
    }
    
    @Override
    public Plugin plugin() { return this; }
    
    public Debugger debugger() { return debugger; }
    
    public Debugger.DebugLogger debug(Class<?> clazz) { return debugger().debug(clazz); }
    
    public void subscribeToDiscordSrv(Plugin plugin)
    {
        debug(getClass()).log("Subscribe", () -> "Subscribing to DiscordSRV: " + plugin);
        
        if (!DISCORDSRV.equals(plugin.getName()) || !(plugin instanceof DiscordSRV))
        {
            throw debug(getClass()).failure("Subscribe", new IllegalArgumentException("Not DiscordSRV: " + plugin));
        }
        
        if (isDiscordSrvHookEnabled)
        {
            throw debug(getClass()).failure("Subscribe", new IllegalStateException(
                "Already subscribed to DiscordSRV. Did the server reload? ... If so, don't do that!"
            ));
        }
        
        this.isDiscordSrvHookEnabled = true;
        DiscordSRV.api.subscribe(discordChatListener);
        
        getLogger().info("Subscribed to DiscordSRV: messages will be sent to Discord");
    }
    
    @Override
    public boolean isDiscordSrvHookEnabled() { return isDiscordSrvHookEnabled; }
    
    @Override
    public @NullOr TextChannel getDiscordChannelOrNull()
    {
        return (isDiscordSrvHookEnabled) 
            ? DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(CHANNEL)
            : null;
    }
    
    private void inGameAnnounce(String message)
    {
        String content = colorful(message);
        
        getServer().getOnlinePlayers().stream()
            .filter(Permissions.ACCESS::allows)
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
        
        debug(getClass()).logMessageSubmissionFromInGame(author, message);
        
        PlayerStaffChatMessageEvent event =
            Events.call(new PlayerStaffChatMessageEvent(author, message));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            debug(getClass()).log(ChatService.MINECRAFT, "Message", () -> "Cancelled or text is empty");
            return;
        }
        
        String text = event.getText();
        String format = Strings.orEmpty(getConfig(), "in-game-message-format");
        
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
        {
            // Update format's PAPI placeholders before inserting the message
            // (which *could* contain arbitrary placeholders itself, ah placeholder injection).
            format = PlaceholderAPI.setPlaceholders(author, format);
        }
        
        MappedPlaceholder placeholders = new MappedPlaceholder();
        
        placeholders.map("message", "content", "text").to(() -> text);
        placeholders.map("user", "name", "username", "player", "sender").to(author::getName);
        placeholders.map("nickname", "displayname").to(author::getDisplayName);
        
        updatePlaceholdersThenAnnounceInGame(format, placeholders);
        
        if (getDiscordChannelOrNull() != null)
        {
            debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
                "Sending message to discord channel: " + CHANNEL + " => " + getDiscordChannelOrNull()
            );
            
            // Send to discord off the main thread (just like DiscordSRV does)
            getServer().getScheduler().runTaskAsynchronously(this, () -> 
                DiscordSRV.getPlugin().processChatMessage(author, message, CHANNEL, false)
            );
        }
        else
        {
            debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
                "Unable to send message to discord: " + CHANNEL + " => null"
            );
        }
    }
    
    @Override
    public void submitMessageFromDiscord(User author, Message message)
    {
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(message, "message");
        
        debug(getClass()).logMessageSubmissionFromDiscord(author, message);
        
        DiscordStaffChatMessageEvent event =
            Events.call(new DiscordStaffChatMessageEvent(author, message, message.getContentStripped()));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            debug(getClass()).log(ChatService.DISCORD, "Message", () -> "Cancelled or text is empty");
            return;
        }
    
        // Emoji Unicode -> Alias (library included with DiscordSRV)
        String text = EmojiParser.parseToAliases(event.getText());
        String format = Strings.orEmpty(getConfig(), "discord-message-format");
        
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
        {
            // Update format's PAPI placeholders before inserting the message.
            format = PlaceholderAPI.setPlaceholders(null, format);
        }
        
        MappedPlaceholder placeholders = new MappedPlaceholder();
        
        placeholders.map("message", "content", "text").to(() -> text);
        placeholders.map("user", "name", "username", "sender").to(author::getName);
        placeholders.map("discriminator", "discrim").to(author::getDiscriminator);
        
        placeholders.map("nickname", "displayname").to(() -> {
            @NullOr Member member = message.getGuild().getMember(author);
            return (member == null ) ? "" : member.getEffectiveName();
        });
        
        updatePlaceholdersThenAnnounceInGame(format, placeholders);
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
                    debug(getClass()).log(() -> "Reloading config...");
                    reloadConfig();
                    sender.sendMessage(colorful("&9&lDiscordSRV-Staff-Chat&f: Reloaded."));
                    break;
                }
                
                case "debug":
                {
                    boolean enabled = !debugger.isEnabled();
                    debugger.setEnabled(enabled);
                    
                    if (enabled)
                    {
                        debugger().schedulePluginStatus(getClass(), "Debug Toggle");
                        sender.sendMessage(colorful("&aEnabled debugging."));
                        
                        if (sender instanceof Player)
                        {
                            sender.sendMessage("[Debug] Sending a test message...");
                            getServer().getScheduler().runTaskLater(this, () -> getServer().dispatchCommand(sender, "staffchat Hello! Just testing things..."), 10L);
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
