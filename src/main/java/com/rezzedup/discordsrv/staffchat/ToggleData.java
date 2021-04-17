package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.util.Strings;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ToggleData
{
    private final Set<UUID> autoChatToggles = new HashSet<>();
    
    private final StaffChatPlugin plugin;
    
    public ToggleData(StaffChatPlugin plugin) { this.plugin = plugin; }
    
    public boolean isChatAutomatic(Player player){ return autoChatToggles.contains(player.getUniqueId()); }
    
    public void toggleAutoChat(Player player) { setAutoChatToggle(player, !isChatAutomatic(player)); }
    
    public void setAutoChatToggle(Player player, boolean state)
    {
        UUID uuid = player.getUniqueId();
        if (state == autoChatToggles.contains(uuid)) { return; }
        
        if (state)
        {
            autoChatToggles.add(uuid);
            player.sendMessage(Strings.colorful(plugin.getConfig().getString("enable-staff-chat")));
            
            plugin.debug(getClass()).log("Toggle", () ->
                "Enabled automatic staff-chat for player: " + player.getName()
            );
        }
        else
        {
            autoChatToggles.remove(uuid);
            player.sendMessage(Strings.colorful(plugin.getConfig().getString("disable-staff-chat")));
            
            plugin.debug(getClass()).log("Toggle", () ->
                "Disabled automatic staff-chat for player: " + player.getName()
            );
        }
    }
}
