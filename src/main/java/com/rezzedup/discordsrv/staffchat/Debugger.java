package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.util.CheckedConsumer;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import pl.tlinkowski.annotation.basic.NullOr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

public class Debugger
{
    private static String now() { return OffsetDateTime.now().toString(); }
    
    public static final DebugLogger EMPTY = message -> {};
    
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
        catch (IOException io) { io.printStackTrace(); }
    }
    
    public boolean isEnabled() { return isEnabled; }
    
    public void setEnabled(boolean enabled)
    {
        if (this.isEnabled == enabled) { return; }
        
        this.isEnabled = enabled;
        
        if (enabled)
        {
            record("===== Enabled Debugging. =====");
            if (!isToggleFilePresent()) { updateToggleFile(Files::createFile); }
        }
        else
        {
            record("===== Disabled Debugging. =====");
            updateToggleFile(Files::deleteIfExists);
        }
    }
    
    public DebugLogger debug(Class<?> clazz)
    {
        return (isEnabled) ? message -> record("[" + clazz.getSimpleName() + "] " + message.get()) : EMPTY;
    }
    
    private void record(String message)
    {
        if (!isEnabled) { return; }
        
        plugin.getLogger().info("[Debug] " + message);
        
        try
        {
            if (!Files.isRegularFile(debugLogFile)) { Files.createFile(debugLogFile); }
            Files.write(debugLogFile, ("[" + now() + "] " + message + "\n").getBytes(), StandardOpenOption.APPEND);
        }
        catch (IOException io) { io.printStackTrace(); }
    }
    
    private static String handleContext(@NullOr Object context)
    {
        if (context instanceof Class<?>) { return ((Class<?>) context).getSimpleName(); }
        if (context instanceof Event) { return ((Event) context).getEventName(); }
        return String.valueOf(context);
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
    }
}
