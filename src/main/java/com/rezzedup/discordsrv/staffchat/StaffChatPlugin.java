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
package com.rezzedup.discordsrv.staffchat;

import com.github.zafarkhaja.semver.Version;
import com.rezzedup.discordsrv.staffchat.commands.ManageStaffChatCommand;
import com.rezzedup.discordsrv.staffchat.commands.StaffChatCommand;
import com.rezzedup.discordsrv.staffchat.commands.ToggleStaffChatCommand;
import com.rezzedup.discordsrv.staffchat.config.MessagesConfig;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.DiscordStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.PlayerStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordSrvLoadedLaterListener;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordStaffChatListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerPrefixedMessageListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerStaffChatToggleListener;
import com.rezzedup.discordsrv.staffchat.util.FileIO;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import community.leaf.eventful.bukkit.EventSource;
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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tlinkowski.annotation.basic.NullOr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.rezzedup.discordsrv.staffchat.util.Strings.colorful;

public class StaffChatPlugin extends JavaPlugin implements BukkitTaskSource, EventSource, StaffChatAPI
{
    // https://bstats.org/plugin/bukkit/DiscordSRV-Staff-Chat/11056
    public static final int BSTATS = 11056;
    
    public static final String CHANNEL = "staff-chat";
    
    public static final String DISCORDSRV = "DiscordSRV";
    
    private @NullOr Debugger debugger;
    private @NullOr Version version;
    private @NullOr Path pluginDirectoryPath;
    private @NullOr Path backupsDirectoryPath;
    private @NullOr StaffChatConfig config;
    private @NullOr MessagesConfig messages;
    private @NullOr StaffChatData data;
    private @NullOr DiscordStaffChatListener discordSrvHook;
    
    @Override
    public void onEnable()
    {
        // First and foremost, setup debugging
        this.debugger = new Debugger(this);
        this.version = Version.valueOf(getDescription().getVersion());
    
        debug(getClass()).header(() -> "Starting Plugin: " + this);
        debugger().schedulePluginStatus(getClass(), "Enable");
        
        // Setup files
        this.pluginDirectoryPath = getDataFolder().toPath();
        this.backupsDirectoryPath = pluginDirectoryPath.resolve("backups");
        this.config = new StaffChatConfig(this);
        this.messages = new MessagesConfig(this);
        this.data = new StaffChatData(this);
        
        upgradeLegacyConfig();
        
        events().register(new PlayerPrefixedMessageListener(this));
        events().register(new PlayerStaffChatToggleListener(this));
        
        command("staffchat", new StaffChatCommand(this));
        command("togglestaffchat", new ToggleStaffChatCommand(this));
        command("managestaffchat", new ManageStaffChatCommand(this));
        
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
        
        data().end();
        
        // Display toggle message so that auto staff-chat users are aware that their chat is public again.
        //getServer().getOnlinePlayers().stream().filter(data()::isChatAutomatic).forEach(data()::toggleAutoChat);
        
        if (isDiscordSrvHookEnabled())
        {
            debug(getClass()).log("Disable", () -> "Unsubscribing from DiscordSRV API (hook is enabled)");
            
            try { DiscordSRV.api.unsubscribe(discordSrvHook); }
            catch (RuntimeException ignored) {} // Don't show a user-facing error if DiscordSRV is already unloaded.
        }
        
        debug(getClass()).header(() -> "Disabled Plugin: " + this);
    }
    
    private <T> T initialized(@NullOr T thing, String name)
    {
        if (thing != null) { return thing; }
        throw new IllegalStateException(name + " isn't initialized (plugin unloaded?)");
    }
    
    @Override
    public Plugin plugin() { return this; }
    
    public Debugger debugger() { return initialized(debugger, "debugger"); }
    
    public Debugger.DebugLogger debug(Class<?> clazz) { return debugger().debug(clazz); }
    
    public Version version() { return initialized(version, "version"); }
    
    public Path directory() { return initialized(pluginDirectoryPath, "pluginDirectoryPath"); }
    
    public Path backups() { return initialized(backupsDirectoryPath, "backupsDirectoryPath"); }
    
    public StaffChatConfig config() { return initialized(config, "config"); }
    
    public MessagesConfig messages() { return initialized(messages, "messages"); }
    
    public StaffChatData data() { return initialized(data, "data"); }
    
    @Override
    public boolean isDiscordSrvHookEnabled() { return discordSrvHook != null; }
    
    public void subscribeToDiscordSrv(Plugin plugin)
    {
        debug(getClass()).log("Subscribe", () -> "Subscribing to DiscordSRV: " + plugin);
        
        if (!DISCORDSRV.equals(plugin.getName()) || !(plugin instanceof DiscordSRV))
        {
            throw debug(getClass()).failure("Subscribe", new IllegalArgumentException("Not DiscordSRV: " + plugin));
        }
        
        if (isDiscordSrvHookEnabled())
        {
            throw debug(getClass()).failure("Subscribe", new IllegalStateException(
                "Already subscribed to DiscordSRV. Did the server reload? ... If so, don't do that!"
            ));
        }
        
        DiscordSRV.api.subscribe(discordSrvHook = new DiscordStaffChatListener(this));
        
        getLogger().info("Subscribed to DiscordSRV: messages will be sent to Discord");
    }
    
    @Override
    public @NullOr TextChannel getDiscordChannelOrNull()
    {
        return (isDiscordSrvHookEnabled())
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
            events().call(new PlayerStaffChatMessageEvent(author, message));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            debug(getClass()).log(ChatService.MINECRAFT, "Message", () -> "Cancelled or text is empty");
            return;
        }
        
        String text = event.getText();
        String format = messages().getOrDefault(MessagesConfig.IN_GAME_MESSAGE_FORMAT);
        
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
            events().call(new DiscordStaffChatMessageEvent(author, message, message.getContentStripped()));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            debug(getClass()).log(ChatService.DISCORD, "Message", () -> "Cancelled or text is empty");
            return;
        }
    
        // Emoji Unicode -> Alias (library included with DiscordSRV)
        String text = EmojiParser.parseToAliases(event.getText());
        String format = messages().getOrDefault(MessagesConfig.DISCORD_MESSAGE_FORMAT);
        
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
    
    private void upgradeLegacyConfig()
    {
        Path configPath = directory().resolve("config.yml");
        if (!Files.isRegularFile(configPath)) { return; }
        
        debug(getClass()).log("Upgrade Legacy Config", () ->
            "Found legacy config, upgrading it to new configs..."
        );
        
        config().migrateValues(StaffChatConfig.VALUES, getConfig());
        config().save();
        
        messages().migrateValues(MessagesConfig.VALUES, getConfig());
        messages().save();
        
        try
        {
            FileIO.backup(configPath, backups().resolve("config.legacy.yml"));
            Files.deleteIfExists(configPath);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            debug(getClass()).log("Upgrade Legacy Config", () ->
                "Failed to backup legacy config: " + e.getMessage()
            );
        }
    }
    
    private void command(String name, CommandExecutor executor)
    {
        @NullOr PluginCommand command = getCommand(name);
        
        if (command == null)
        {
            debug(getClass()).log("Command: Setup", () ->
                "Unable to register command /" + name + " because it is not defined in plugin.yml"
            );
            return;
        }
        
        command.setExecutor(executor);
        debug(getClass()).log("Command: Setup", () -> "Registered command executor for: /" + name);
        
        if (executor instanceof TabCompleter)
        {
            command.setTabCompleter((TabCompleter) executor);
            debug(getClass()).log("Command: Setup", () -> "Registered tab completer for: /" + name);
        }
    }
    
    private void startMetrics()
    {
        if (!config().getOrDefault(StaffChatConfig.METRICS_ENABLED))
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
}
