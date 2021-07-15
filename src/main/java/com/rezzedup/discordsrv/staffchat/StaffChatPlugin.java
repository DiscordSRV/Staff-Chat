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
import com.rezzedup.discordsrv.staffchat.listeners.DiscordSrvLoadedLaterListener;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordStaffChatListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerPrefixedMessageListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerStaffChatToggleListener;
import com.rezzedup.discordsrv.staffchat.util.FileIO;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.eventful.bukkit.EventSource;
import community.leaf.tasks.bukkit.BukkitTaskSource;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StaffChatPlugin extends JavaPlugin implements BukkitTaskSource, EventSource, StaffChatAPI
{
    // https://bstats.org/plugin/bukkit/DiscordSRV-Staff-Chat/11056
    public static final int BSTATS = 11056;
    
    public static final String CHANNEL = "staff-chat";
    
    public static final String DISCORDSRV = "DiscordSRV";
    
    private final Set<String> existingConfigs = new HashSet<>();
    
    private @NullOr Version version;
    private @NullOr Path pluginDirectoryPath;
    private @NullOr Path backupsDirectoryPath;
    private @NullOr Debugger debugger;
    private @NullOr StaffChatConfig config;
    private @NullOr MessagesConfig messages;
    private @NullOr Data data;
    private @NullOr MessageProcessor processor;
    private @NullOr DiscordStaffChatListener discordSrvHook;
    
    @Override
    public void onEnable()
    {
        this.version = Version.valueOf(getDescription().getVersion());
        
        this.pluginDirectoryPath = getDataFolder().toPath();
        this.backupsDirectoryPath = pluginDirectoryPath.resolve("backups");
        
        checkExistingConfigs();
        
        this.debugger = new Debugger(this);
        
        debug(getClass()).header(() -> "Starting Plugin: " + this);
        debugger().schedulePluginStatus(getClass(), "Enable");
        
        this.config = new StaffChatConfig(this);
        this.messages = new MessagesConfig(this);
    
        upgradeLegacyConfig();
        
        this.data = new Data(this);
        this.processor = new MessageProcessor(this);
        
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
            
            // Subscribe to DiscordSRV later because it somehow hasn't enabled yet.
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
    
    private <T> T initialized(@NullOr T thing)
    {
        if (thing != null) { return thing; }
        throw new IllegalStateException("Not initialized yet");
    }
    
    @Override
    public Plugin plugin() { return this; }
    
    public Version version() { return initialized(version); }
    
    public Path directory() { return initialized(pluginDirectoryPath); }
    
    public Path backups() { return initialized(backupsDirectoryPath); }
    
    public Debugger debugger() { return initialized(debugger); }
    
    public Debugger.DebugLogger debug(Class<?> clazz) { return debugger().debug(clazz); }
    
    public StaffChatConfig config() { return initialized(config); }
    
    public MessagesConfig messages() { return initialized(messages); }
    
    @Override
    public Data data() { return initialized(data); }
    
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
    
    private MessageProcessor processor() { return initialized(processor); }
    
    @Override
    public void submitMessageFromConsole(String message)
    {
        processor().processConsoleChat(message);
    }
    
    @Override
    public void submitMessageFromInGame(Player author, String message)
    {
        processor().processPlayerChat(author, message);
    }
    
    @Override
    public void submitMessageFromDiscord(User author, Message message)
    {
        processor().processDiscordChat(author, message);
    }
    
    //
    //
    //
    
    private void checkExistingConfig(String fileName)
    {
        if (Files.isRegularFile(directory().resolve(fileName))) { existingConfigs.add(fileName); }
    }
    
    private void checkExistingConfigs()
    {
        existingConfigs.clear();
        checkExistingConfig(StaffChatConfig.FILE_NAME);
        checkExistingConfig(MessagesConfig.FILE_NAME);
    }
    
    private void upgradeLegacyConfig(YamlDataFile file, String fileName, List<YamlValue<?>> values)
    {
        file.migrateValues(values, getConfig());
        
        if (existingConfigs.contains(fileName))
        {
            file.backupThenSave(backups(), "migrated");
        }
        else
        {
            file.save();
        }
    }
    
    private void upgradeLegacyConfig()
    {
        Path legacyConfigPath = directory().resolve("config.yml");
        if (!Files.isRegularFile(legacyConfigPath)) { return; }
        
        debug(getClass()).log("Upgrade Legacy Config", () ->
            "Found legacy config, upgrading it to new configs..."
        );
        
        upgradeLegacyConfig(config(), StaffChatConfig.FILE_NAME, StaffChatConfig.VALUES);
        upgradeLegacyConfig(messages(), MessagesConfig.FILE_NAME, MessagesConfig.VALUES);
        
        try
        {
            FileIO.backup(legacyConfigPath, backups().resolve("config.legacy.yml"));
            Files.deleteIfExists(legacyConfigPath);
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
