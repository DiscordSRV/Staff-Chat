package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.api.StaffChatApi;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordStaffChatListener;
import com.rezzedup.discordsrv.staffchat.listeners.MessageFromDiscordListener;
import com.rezzedup.discordsrv.staffchat.listeners.MessageFromGameListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerStaffChatToggleListener;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.core.entities.TextChannel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import static com.rezzedup.discordsrv.staffchat.util.Strings.colorful;

public class StaffChatPlugin extends JavaPlugin
{
    public static final String CHANNEL = "staff-chat";
         
    private boolean isDiscordSrvHookEnabled = false;
    private Debugger debugger;
    private DiscordStaffChatListener discordChatListener;
    private PlayerStaffChatToggleListener inGameToggles;
    private StaffChatApi api;
    
    @Override
    public void onEnable()
    {
        this.debugger = new Debugger(this);
        this.discordChatListener = new DiscordStaffChatListener(this);
        this.inGameToggles = new PlayerStaffChatToggleListener(this);
        this.api = new StaffChatApi(this);
        
        debugger.debug("----- Starting Plugin: v%s -----", getDescription().getVersion());
        
        getServer().getPluginManager().registerEvents(inGameToggles, this);
        getServer().getPluginManager().registerEvents(new MessageFromDiscordListener(this), this);
        getServer().getPluginManager().registerEvents(new MessageFromGameListener(this), this);
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
    
    public boolean isDiscordSrvHookEnabled() { return isDiscordSrvHookEnabled; }
    
    public TextChannel getDiscordChannel()
    {
        return (isDiscordSrvHookEnabled) 
            ? DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(CHANNEL) : null;
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
            else { api.submitFromInGame(player, String.join(" ", args)); }
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
    
    public StaffChatApi getApi() { return this.api; }
}
