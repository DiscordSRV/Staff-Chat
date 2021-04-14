package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.util.CheckedConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

public class Debugger
{
    private static String now() { return OffsetDateTime.now().toString(); }
    
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
            debug("===== Enabled Debugging. =====");
            if (!isToggleFilePresent()) { updateToggleFile(Files::createFile); }
        }
        else
        {
            debug("===== Disabled Debugging. =====");
            updateToggleFile(Files::deleteIfExists);
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
            Files.write(debugLogFile, ("[" + now() + "]" + content + "\n").getBytes(), StandardOpenOption.APPEND);
        }
        catch (IOException io) { io.printStackTrace(); }
    }
}
