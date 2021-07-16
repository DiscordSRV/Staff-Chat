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
package com.rezzedup.discordsrv.staffchat.config;

import com.github.zafarkhaja.semver.Version;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import com.rezzedup.util.constants.Aggregates;
import com.rezzedup.util.constants.annotations.AggregatedResult;
import community.leaf.configvalues.bukkit.DefaultYamlValue;
import community.leaf.configvalues.bukkit.ExampleYamlValue;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.configvalues.bukkit.migrations.Migration;
import community.leaf.configvalues.bukkit.util.Sections;
import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.List;
import java.util.function.Predicate;

public class MessagesConfig extends YamlDataFile
{
    public static final YamlValue<Version> VERSION =
        YamlValue.of("meta.config-version", Configs.VERSION).maybe();
    
    public static final DefaultYamlValue<String> PREFIX =
        YamlValue.ofString("placeholders.prefix")
            .defaults("&d(&5&l&oStaff&d)");
    
    public static final ExampleYamlValue<String> EXAMPLE_PLACEHOLDER =
        YamlValue.ofString("placeholders.example")
            .example("Define your own placeholders here!");
    
    public static final DefaultYamlValue<String> IN_GAME_PLAYER_FORMAT =
        YamlValue.ofString("messages.in-game-formats.player")
            .migrates(Migration.move("in-game-message-format"))
            .defaults("%prefix% %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> IN_GAME_DISCORD_FORMAT =
        YamlValue.ofString("messages.in-game-formats.discord")
            .migrates(Migration.move("discord-message-format"))
            .defaults("&9&ldiscord &f→ %prefix% %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> IN_GAME_CONSOLE_FORMAT =
        YamlValue.ofString("messages.in-game-formats.console")
            .defaults("%prefix% [CONSOLE]&7:&f %message%");
    
    public static final DefaultYamlValue<String> DISCORD_CONSOLE_FORMAT =
        YamlValue.ofString("messages.discord-formats.console")
            .defaults("**`CONSOLE:`** %message%");
    
    public static final DefaultYamlValue<String> AUTO_ENABLED_NOTIFICATION =
        YamlValue.ofString("notifications.automatic-staff-chat.enabled")
            .migrates(Migration.move("enable-staff-chat"))
            .defaults("%prefix% &2→&a &nEnabled&a automatic staff chat");
    
    public static final DefaultYamlValue<String> AUTO_DISABLED_NOTIFICATION =
        YamlValue.ofString("notifications.automatic-staff-chat.disabled")
            .migrates(Migration.move("disable-staff-chat"))
            .defaults("%prefix% &4→&c &nDisabled&c automatic staff chat");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("notifications.leave.self")
            .defaults(
                "%prefix% &4→&c You &nleft&c the staff chat&r\n" +
                "&8&oYou won't receive any staff chat messages"
            );
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("notifications.leave.others")
            .defaults("%prefix% &4→&c %player% &nleft&c the staff chat");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_REMINDER =
        YamlValue.ofString("notifications.leave.reminder")
            .defaults("&8&o(Reminder: you left the staff chat)");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_DISABLED_ERROR =
        YamlValue.ofString("notifications.leave.disabled")
            .defaults(
                "%prefix% &6→&e You cannot leave the staff chat\n" +
                "&8&oLeaving the staff chat is currently disabled"
            );
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("notifications.join.self")
            .defaults(
                "%prefix% &2→&a You &njoined&a the staff chat&r\n" +
                "&8&oYou will now receive staff chat messages again"
            );
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("notifications.join.others")
            .defaults("%prefix% &2→&a %player% &njoined&a the staff chat");
    
    @AggregatedResult
    public static final List<YamlValue<?>> VALUES = Aggregates.list(MessagesConfig.class, YamlValue.type());
    
    private final StaffChatPlugin plugin;
    
    private @NullOr MappedPlaceholder definitions = null;
    
    public MessagesConfig(StaffChatPlugin plugin)
    {
        super(plugin.directory(), "messages.config.yml");
        this.plugin = plugin;
        
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
            
            // Remove old placeholder definitions
            definitions = null;
            
            // Load defined placeholders
            Sections.get(data(), "placeholders").ifPresent(section ->
            {
                definitions = new MappedPlaceholder();
                
                for (String key : section.getKeys(false))
                {
                    @NullOr String value = section.getString(key);
                    if (Strings.isEmptyOrNull(value)) { continue; }
                    definitions.map(key).to(() -> value);
                }
            });
        });
    }
    
    public MappedPlaceholder placeholders()
    {
        MappedPlaceholder placeholders = new MappedPlaceholder();
        if (definitions != null) { placeholders.inherit(definitions); }
        return placeholders;
    }
    
    public MappedPlaceholder placeholders(Player player)
    {
        MappedPlaceholder placeholders = placeholders();
        
        placeholders.map("user", "name", "username", "player", "sender").to(player::getName);
        placeholders.map("nickname", "displayname").to(player::getDisplayName);
        
        return placeholders;
    }
    
    private void sendNotification(Player player, DefaultYamlValue<String> self, @NullOr DefaultYamlValue<String> others)
    {
        MappedPlaceholder placeholders = placeholders(player);
        
        player.sendMessage(Strings.colorful(placeholders.update(getOrDefault(self))));
        plugin.config().playNotificationSound(player);
        
        if (others == null) { return; }
        
        String notification = Strings.colorful(placeholders.update(getOrDefault(others)));
        plugin.getServer().getConsoleSender().sendMessage(notification);
        
        plugin.onlineStaffChatParticipants()
            .filter(Predicate.not(player::equals))
            .forEach(staff -> {
                staff.sendMessage(notification);
                plugin.config().playNotificationSound(staff);
            });
    }
    
    public void notifyAutoChatEnabled(Player enabler)
    {
        sendNotification(enabler, AUTO_ENABLED_NOTIFICATION, null);
    }
    
    public void notifyAutoChatDisabled(Player disabler)
    {
        sendNotification(disabler, AUTO_DISABLED_NOTIFICATION, null);
    }
    
    public void notifyLeaveChat(Player leaver, boolean notifyOthers)
    {
        @NullOr DefaultYamlValue<String> others = (notifyOthers) ? LEFT_CHAT_NOTIFICATION_OTHERS : null;
        sendNotification(leaver, LEFT_CHAT_NOTIFICATION_SELF, others);
    }
    
    public void notifyLeavingChatIsDisabled(Player leaver)
    {
        sendNotification(leaver, LEFT_CHAT_DISABLED_ERROR, null);
    }
    
    public void notifyJoinChat(Player joiner, boolean notifyOthers)
    {
        @NullOr DefaultYamlValue<String> others = (notifyOthers) ? JOIN_CHAT_NOTIFICATION_OTHERS : null;
        sendNotification(joiner, JOIN_CHAT_NOTIFICATION_SELF, others);
    }
}
