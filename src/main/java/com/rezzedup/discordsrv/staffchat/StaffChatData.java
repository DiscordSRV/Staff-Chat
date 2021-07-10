package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.config.Configs;
import com.rezzedup.discordsrv.staffchat.config.MessagesConfig;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.configvalues.bukkit.util.Sections;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class StaffChatData
{
    private final Set<UUID> autoChatToggles = new HashSet<>();
    private final Set<UUID> leftChatToggles = new HashSet<>();
    
    private final StaffChatPlugin plugin;
    private final YamlDataFile yaml;
    
    public StaffChatData(StaffChatPlugin plugin)
    {
        this.plugin = plugin;
        this.yaml = new YamlDataFile(plugin.directory().resolve("data"), "staff-chat.data.yml");
    }
    
    public boolean isChatAutomatic(Player player){ return autoChatToggles.contains(player.getUniqueId()); }
    
    public void toggleAutoChat(Player player) { setAutoChatToggle(player, !isChatAutomatic(player)); }
    
    public void setAutoChatToggle(Player player, boolean state)
    {
        // TODO: convert to profiles
    }
    
    static class Profile implements StaffChatProfile
    {
        static final YamlValue<Instant> AUTO_TOGGLE_DATE = YamlValue.of("toggles.auto", Configs.INSTANT).maybe();
        
        static final YamlValue<Instant> LEFT_TOGGLE_DATE = YamlValue.of("toggles.left", Configs.INSTANT).maybe();
        
        private final StaffChatPlugin plugin;
        private final YamlDataFile yaml;
        private final UUID uuid;
        
        private @NullOr Instant auto;
        private @NullOr Instant left;
        
        Profile(StaffChatPlugin plugin, YamlDataFile yaml, UUID uuid)
        {
            this.plugin = plugin;
            this.yaml = yaml;
            this.uuid = uuid;
            
            if (plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES))
            {
                Sections.get(yaml.data(), path()).ifPresent(section -> {
                    auto = AUTO_TOGGLE_DATE.get(section).orElse(null);
                    left = LEFT_TOGGLE_DATE.get(section).orElse(null);
                });
            }
        }
        
        String path()
        {
            return "staff-chat.profiles." + uuid;
        }
        
        @Override
        public UUID uuid() { return uuid; }
    
        @Override
        public Optional<Instant> sinceEnabledAutoChat()
        {
            return Optional.ofNullable(auto);
        }
    
        @Override
        public void enableAutoChat(boolean enabled)
        {
            // Avoid redundantly setting: already enabled if auto is not null
            if (enabled == (auto != null)) { return; }
            
            if (plugin.events().call(new AutoStaffChatToggleEvent(this, enabled)).isCancelled()) { return; }
            
            auto = (enabled) ? Instant.now() : null;
            updateProfileData();
        }
        
        @Override
        public Optional<Instant> sinceLeftStaffChat()
        {
            return Optional.ofNullable(left);
        }
    
        @Override
        public void receiveStaffChat(boolean enabled)
        {
            // Avoid redundantly setting: already enabled if `left` is null
            // (the staff member is receiving messages because they have not left the chat)
            if (enabled == (left == null)) { return; }
            
            if (plugin.events().call(new ReceivingStaffChatToggleEvent(this, enabled)).isCancelled()) { return; }
            
            left = (enabled) ? null : Instant.now();
            updateProfileData();
        }
        
        void updateProfileData()
        {
            if (!plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) { return; }
            
            if (auto == null && left == null)
            {
                yaml.data().set(path(), null);
            }
            else
            {
                ConfigurationSection section = Sections.getOrCreate(yaml.data(), path());
            
                AUTO_TOGGLE_DATE.set(section, auto);
                LEFT_TOGGLE_DATE.set(section, left);
            }
            
            yaml.updated(true);
        }
    }
}
