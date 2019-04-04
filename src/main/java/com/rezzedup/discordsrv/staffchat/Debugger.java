package com.rezzedup.discordsrv.staffchat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class Debugger
{
    public static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("YYYY-MM-dd => HH:mm:ss");
    
    public static String now() { return OffsetDateTime.now().format(TIMESTAMP); }
    
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
        this.isEnabled = Files.isRegularFile(debugToggleFile);
    }
    
    public boolean isEnabled() { return isEnabled; }
    
    public void toggle()
    {
        if (!isEnabled)
        {
            isEnabled = true;
            debug("===== Enabled Debugging. =====");
            
            try { Files.createFile(debugToggleFile); }
            catch (IOException io) { io.printStackTrace(); }
        }
        else 
        {
            isEnabled = false;
            debug("===== Disabled debugging. =====");
            
            try { Files.deleteIfExists(debugToggleFile); }
            catch (IOException io) { io.printStackTrace(); }
        }
    }
    
    public void debug(String message, Object ... placeholders)
    {
        if (!isEnabled) { return; }
        
        String content = String.format(message, placeholders);
        plugin.getLogger().info("[Debug] " + content);
        
        try
        {
            if (!Files.isRegularFile(debugLogFile)) { Files.createFile(debugLogFile); }
            
            Files.write(
                debugLogFile, String.format("[%s]: %s\n", now(), content).getBytes(), StandardOpenOption.APPEND
            );
        }
        catch (IOException io) { io.printStackTrace(); }
    }
    
    void logDiscordHookState()
    {
        debug(" - isDiscordSrvHookEnabled: %s", plugin.isDiscordSrvHookEnabled());
        debug(" - Discord channel: %s => %s", StaffChatPlugin.CHANNEL, plugin.getDiscordChannel());
    }
}
