package com.rezzedup.discordsrv.staffchat.config;

import com.github.zafarkhaja.semver.Version;
import com.rezzedup.discordsrv.staffchat.Constants;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.util.Aggregates;
import com.rezzedup.discordsrv.staffchat.util.yaml.YamlDataFile;
import com.rezzedup.discordsrv.staffchat.util.yaml.YamlValue;

import java.util.List;

public class StaffChatConfig extends YamlDataFile
{
    public static final YamlValue.Maybe<String> VERSION =
        YamlValue.ofString("meta.config-version").maybe();
    
    public static final YamlValue.Default<Boolean> ENABLE_METRICS =
        YamlValue.ofBoolean("plugin.metrics").migrates("metrics").defaults(true);
    
    public static final YamlValue.Default<Boolean> ENABLE_UPDATE_CHECKER =
        YamlValue.ofBoolean("plugin.updates.check-for-updates").defaults(true);
    
    public static final YamlValue.Default<Boolean> UPDATE_CHECKER_NOTIFICATIONS =
        YamlValue.ofBoolean("plugin.updates.notify-operators").defaults(true);
    
    public static final YamlValue.Default<Boolean> PERSIST_TOGGLES =
        YamlValue.ofBoolean("staffchat.toggles.chat-toggles-persist-after-restart").defaults(true);
    
    public static final YamlValue.Default<Boolean> ENABLE_LEAVING_STAFFCHAT =
        YamlValue.ofBoolean("staffchat.toggles.let-staff-members-turn-off-staffchat").defaults(true);
    
    public static final YamlValue.Default<Boolean> TOGGLE_NOTIFICATIONS =
        YamlValue.ofBoolean("staffchat.toggles.notify-toggle-status-on-join").defaults(true);
    
    public static final YamlValue.Default<Boolean> ENABLE_PREFIXED_CHAT =
        YamlValue.ofBoolean("staffchat.prefixed.enable-prefixed-chat-messages")
            .migrates("enable-prefixed-chat-messages")
            .defaults(false);
    
    public static final YamlValue.Default<String> PREFIXED_CHAT_IDENTIFIER =
        YamlValue.ofString("staffchat.prefixed.prefixed-chat-identifier")
            .migrates("prefixed-chat-identifier")
            .defaults("@");
    
    @Aggregates.Result
    public static final List<YamlValue<?>> VALUES = Aggregates.list(StaffChatConfig.class, YamlValue.TYPE);
    
    private final StaffChatPlugin plugin;
    
    public StaffChatConfig(StaffChatPlugin plugin)
    {
        super(plugin.directory(), "staffchat.config.yml");
        this.plugin = plugin;
        plugin.debug(getClass()).logConfigValues(getFilePath(), VALUES);
        handleReload();
    }
    
    @Override
    protected void handleReload()
    {
        if (isInvalid())
        {
            plugin.debug(getClass()).log("Reload", () -> "Couldn't load: " + getInvalidReason());
            return;
        }
        
        Version existing = Version.valueOf(get(VERSION).orElse(Constants.NO_VERSION));
        boolean isOutdated = existing.lessThan(plugin.version());
        
        if (isOutdated)
        {
            plugin.debug(getClass()).log("Reload", () -> "Updating outdated config: " + existing);
            set(VERSION, plugin.version().toString());
        }
        
        setupHeader("staffchat.config.header.txt");
        setupDefaults(VALUES);
        
        if (isUpdated())
        {
            plugin.debug(getClass()).log("Reload", () -> "Saving updated config and backing up old config: v" + existing);
            backupThenSave(plugin.backups(), "v" + existing);
        }
    }
}
