package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.util.CheckedConsumer;
import com.rezzedup.discordsrv.staffchat.util.yaml.YamlValue;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import pl.tlinkowski.annotation.basic.NullOr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

public class Debugger
{
    private static String now() { return OffsetDateTime.now().toString(); }
    
    private static final DebugLogger DISABLED = message -> {};
    
    private final StaffChatPlugin plugin;
    private final Path debugToggleFile;
    private final Path debugLogFile;
    
    private boolean isEnabled;
    
    public Debugger(StaffChatPlugin plugin)
    {
        this.plugin = plugin;
        Path root = plugin.getDataFolder().toPath();
        this.debugToggleFile = root.resolve("debugging-is-enabled");
        this.debugLogFile = root.resolve("debug.log");
        this.isEnabled = isToggleFilePresent();
    }
    
    private boolean isToggleFilePresent() { return Files.isRegularFile(debugToggleFile); }
    
    private void updateToggleFile(CheckedConsumer<Path, IOException> update)
    {
        try { update.accept(debugToggleFile); }
        catch (IOException e) { e.printStackTrace(); }
    }
    
    public boolean isEnabled() { return isEnabled; }
    
    public void setEnabled(boolean enabled)
    {
        if (this.isEnabled == enabled) { return; }
        
        this.isEnabled = enabled;
        
        if (enabled)
        {
            printThenWriteToLogFile("========== Starting Debugger ==========");
            if (!isToggleFilePresent()) { updateToggleFile(Files::createFile); }
        }
        else
        {
            printThenWriteToLogFile("========== Disabled Debugger ==========");
            updateToggleFile(Files::deleteIfExists);
        }
    }
    
    public DebugLogger debug(Class<?> clazz)
    {
        return (isEnabled) ? message -> record("[" + clazz.getSimpleName() + "] " + message.get()) : DISABLED;
    }
    
    private void record(String message)
    {
        if (isEnabled) { printThenWriteToLogFile(message); }
    }
    
    private void printThenWriteToLogFile(String message)
    {
        plugin.getLogger().info("[Debug] " + message);
        
        try
        {
            if (!Files.isRegularFile(debugLogFile)) { Files.createFile(debugLogFile); }
            Files.write(debugLogFile, ("[" + now() + "] " + message + "\n").getBytes(), StandardOpenOption.APPEND);
        }
        catch (IOException e) { e.printStackTrace(); }
    }
    
    public void schedulePluginStatus(Class<?> clazz, String context)
    {
        if (!isEnabled) { return; }
        
        // Log status directly on the next tick.
        plugin.sync().run(() -> logPluginStatus(clazz, context + " (Initial)"));
    
        // Log status 30 seconds after so that DiscordSRV has a chance to connect.
        plugin.sync().delay(30).seconds().run(() -> logPluginStatus(clazz, context + " (30 Seconds)"));
    }
    
    private void logPluginStatus(Class<?> clazz, String context)
    {
        debug(clazz).recordDebugLogEntry(() ->
        {
            @NullOr Plugin discordSrv = plugin.getServer().getPluginManager().getPlugin(Constants.DISCORDSRV);
            @NullOr Object channel = plugin.getDiscordChannelOrNull();
            
            boolean isDiscordSrvEnabled = discordSrv != null && discordSrv.isEnabled();
            boolean isDiscordSrvHooked = plugin.isDiscordSrvHookEnabled();
            boolean isChannelReady = channel != null;
            
            return "[Status: " + context + "] " +
                   "Is DiscordSRV installed and enabled? " + isDiscordSrvEnabled + " :: " +
                   "Is DiscordSRV hooked? " + isDiscordSrvHooked + " :: " +
                   "Is " + Constants.CHANNEL + " channel ready? " + isChannelReady + " (" + channel + ")";
        });
    }
    
    private static String handleContext(@NullOr Object context)
    {
        if (context instanceof Class<?>) { return ((Class<?>) context).getSimpleName(); }
        if (context instanceof Event) { return ((Event) context).getEventName(); }
        return String.valueOf(context);
    }
    
    private static String handleException(Throwable exception)
    {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
    
    @FunctionalInterface
    public interface DebugLogger
    {
        void recordDebugLogEntry(Supplier<String> message);
        
        default void log(Supplier<String> message)
        {
            recordDebugLogEntry(message);
        }
        
        default void log(@NullOr Object context, Supplier<String> message)
        {
            recordDebugLogEntry(() -> "[" + handleContext(context) + "] " + message.get());
        }
        
        default void log(ChatService source, @NullOr Object context, Supplier<String> message)
        {
            recordDebugLogEntry(() -> source.asPrefixInBrackets(handleContext(context)) + " " + message.get());
        }
        
        default void header(Supplier<String> message)
        {
            recordDebugLogEntry(() -> "---------- " + message.get() + " ----------");
        }
        
        default <T extends Throwable> T failure(T exception) throws T
        {
            log(() -> handleException(exception));
            throw exception;
        }
        
        default <T extends Throwable> T failure(@NullOr Object context, T exception) throws T
        {
            log(context, () -> handleException(exception));
            throw exception;
        }
        
        default void logMessageSubmissionFromInGame(Player author, String message)
        {
            log(ChatService.MINECRAFT, "Message", () ->
                "from(" + author.getName() + ") message(\"" + message + "\")"
            );
        }
        
        default void logMessageSubmissionFromDiscord(User author, Message message)
        {
            log(ChatService.DISCORD, "Message", () ->
                "from(" + author.getName() + "#" + author.getDiscriminator() + ") message(\"" + message.getContentStripped() + "\")"
            );
        }
        
        default void logConfigValues(Path filePath, List<YamlValue<?>> values)
        {
            recordDebugLogEntry(() ->
            {
                String name = filePath.getFileName().toString();
                StringBuilder defaults = new StringBuilder();
                
                defaults.append("Total: ").append(values.size()).append(" => ");
                
                boolean isAppended = false;
                
                for (YamlValue<?> value : values)
                {
                    if (isAppended) { defaults.append(" :: "); }
                    else { isAppended = true; }
                    
                    defaults.append("path(").append(value.path()).append(")");
                    
                    if (value instanceof YamlValue.Default<?>)
                    {
                        defaults.append(" default(").append(((YamlValue.Default<?>) value).getDefaultValue()).append(")");
                    }
                    else
                    {
                        defaults.append(" maybe()");
                    }
                }
                
                return "[" + name + "] " + defaults;
            });
        }
    }
}
