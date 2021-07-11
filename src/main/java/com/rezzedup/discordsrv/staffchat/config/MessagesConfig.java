package com.rezzedup.discordsrv.staffchat.config;

import com.github.zafarkhaja.semver.Version;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.util.constants.Aggregates;
import com.rezzedup.util.constants.annotations.AggregatedResult;
import community.leaf.configvalues.bukkit.DefaultYamlValue;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.configvalues.bukkit.migrations.Migration;

import java.util.List;

public class MessagesConfig extends YamlDataFile
{
    public static final YamlValue<Version> VERSION =
        YamlValue.of("meta.config-version", Configs.VERSION).maybe();
    
    public static final DefaultYamlValue<String> IN_GAME_MESSAGE_FORMAT =
        YamlValue.ofString("messages.formats.in-game")
            .migrates(Migration.move("in-game-message-format"))
            .defaults("&d(&5&l&oStaff&d) %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> DISCORD_MESSAGE_FORMAT =
        YamlValue.ofString("messages.formats.discord")
            .migrates(Migration.move("discord-message-format"))
            .defaults("&9&ldiscord &f-> &d(&5&l&oStaff&d) %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> AUTO_ENABLED_NOTIFICATION =
        YamlValue.ofString("messages.notifications.automatic-staff-chat.enabled")
            .migrates(Migration.move("enable-staff-chat"))
            .defaults("&d(&5&l&oStaff&d) &2->&a &nEnabled&a automatic staff chat");
    
    public static final DefaultYamlValue<String> AUTO_DISABLED_NOTIFICATION =
        YamlValue.ofString("messages.notifications.automatic-staff-chat.disabled")
            .migrates(Migration.move("disable-staff-chat"))
            .defaults("&d(&5&l&oStaff&d) &4->&c &nDisabled&c automatic staff chat");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("messages.notifications.leave.self")
            .defaults("&d(&5&l&oStaff&d) &4->&c You &nleft&c the staff chat (you won't receive any messages)");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("messages.notifications.leave.others")
            .defaults("&d(&5&l&oStaff&d) &4->&c %player% &nleft&c the staff chat");
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("messages.notifications.join.self")
            .defaults("&d(&5&l&oStaff&d) &4->&a You &njoined&a the staff chat (you will receive messages again)");
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("messages.notifications.join.others")
            .defaults("&d(&5&l&oStaff&d) &4->&a %player% &njoined&a the staff chat");
    
    @AggregatedResult
    public static final List<YamlValue<?>> VALUES = Aggregates.list(MessagesConfig.class, YamlValue.type());
    
    public MessagesConfig(StaffChatPlugin plugin)
    {
        super(plugin.directory(), "messages.config.yml");
        plugin.debug(getClass()).logConfigValues(getFilePath(), VALUES);
        
        reloadsWith(() ->
        {
            if (isInvalid())
            {
                plugin.debug(getClass()).log("Reload", () -> "Couldn't load: " + getInvalidReason());
                return;
            }
            
            Version existing = get(VERSION).orElse(Configs.NO_VERSION);
            boolean isOutdated = existing.lessThan(plugin.version());
            
            if (isOutdated)
            {
                plugin.debug(getClass()).log("Reload", () -> "Updating outdated config: " + existing);
                set(VERSION, plugin.version());
            }
            
            defaultValues(VALUES);
            
            if (isUpdated())
            {
                plugin.debug(getClass()).log("Reload", () -> "Saving updated config and backing up old config: v" + existing);
                backupThenSave(plugin.backups(), "v" + existing);
            }
        });
    }
}
