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
import com.rezzedup.discordsrv.staffchat.ChatService;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.events.ConsoleStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.DiscordStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.PlayerStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import com.rezzedup.util.constants.Aggregates;
import com.rezzedup.util.constants.annotations.AggregatedResult;
import community.leaf.configvalues.bukkit.DefaultYamlValue;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.configvalues.bukkit.migrations.Migration;
import community.leaf.configvalues.bukkit.util.Sections;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.emoji.EmojiParser;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MessagesConfig extends YamlDataFile
{
    public static final YamlValue<Version> VERSION =
        YamlValue.of("meta.config-version", Configs.VERSION).maybe();
    
    public static final DefaultYamlValue<String> PREFIX =
        YamlValue.ofString("placeholders.prefix")
            .defaults("&d(&5&l&oStaff&d)");
    
    public static final DefaultYamlValue<String> IN_GAME_MESSAGE_FORMAT =
        YamlValue.ofString("messages.formats.in-game")
            .migrates(Migration.move("in-game-message-format"))
            .defaults("%prefix% %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> DISCORD_MESSAGE_FORMAT =
        YamlValue.ofString("messages.formats.discord")
            .migrates(Migration.move("discord-message-format"))
            .defaults("&9&ldiscord &f-> %prefix% %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> CONSOLE_MESSAGE_FORMAT =
        YamlValue.ofString("messages.formats.console")
            .defaults("%prefix% [CONSOLE]&7:&f %message%");
    
    public static final DefaultYamlValue<String> AUTO_ENABLED_NOTIFICATION =
        YamlValue.ofString("messages.notifications.automatic-staff-chat.enabled")
            .migrates(Migration.move("enable-staff-chat"))
            .defaults("%prefix% &2->&a &nEnabled&a automatic staff chat");
    
    public static final DefaultYamlValue<String> AUTO_DISABLED_NOTIFICATION =
        YamlValue.ofString("messages.notifications.automatic-staff-chat.disabled")
            .migrates(Migration.move("disable-staff-chat"))
            .defaults("%prefix% &4->&c &nDisabled&c automatic staff chat");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("messages.notifications.leave.self")
            .defaults(
                "%prefix% &4->&c You &nleft&c the staff chat&r\n" +
                "&8&oYou won't receive any staff chat messages"
            );
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_REMINDER =
        YamlValue.ofString("messages.notifications.leave.reminder")
            .defaults("&8&o(Reminder: you left the staff chat)");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("messages.notifications.leave.others")
            .defaults("%prefix% &4->&c %player% &nleft&c the staff chat");
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("messages.notifications.join.self")
            .defaults(
                "%prefix% &2->&a You &njoined&a the staff chat&r\n" +
                "&8&oYou will now receive staff chat messages again"
            );
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("messages.notifications.join.others")
            .defaults("%prefix% &2->&a %player% &njoined&a the staff chat");
    
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
    
    private void sendFormattedChatMessage(@NullOr Object author, DefaultYamlValue<String> format, MappedPlaceholder placeholders)
    {
        // If the value of %message% doesn't exist for some reason, don't announce.
        if (Strings.isEmptyOrNull(placeholders.get("message"))) { return; }
        
        String formatted = getOrDefault(format);
        
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
        {
            // Update format's PAPI placeholders before inserting the message
            // (which *could* contain arbitrary placeholders itself, ah placeholder injection).
            @NullOr Player player = (author instanceof Player) ? (Player) author : null;
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }
        
        String content = Strings.colorful(placeholders.update(formatted));
        
        if (author instanceof Player)
        {
            Player player = (Player) author;
            
            // Author left the staff chat but is sending a message there...
            if (!plugin.data().isReceivingStaffChatMessages(player))
            {
                String reminder = Strings.colorful(placeholders.update(getOrDefault(LEFT_CHAT_NOTIFICATION_REMINDER)));
                
                player.sendMessage(content);
                player.sendMessage(reminder);
                plugin.config().playNotificationSound(player);
            }
        }
        
        plugin.onlineStaffChatParticipants().forEach(staff -> {
            staff.sendMessage(content);
            plugin.config().playMessageSound(staff);
        });
        
        plugin.getServer().getConsoleSender().sendMessage(content);
    }
    
    private void sendToDiscord(Consumer<TextChannel> sender)
    {
        @NullOr TextChannel channel = plugin.getDiscordChannelOrNull();
        
        if (channel == null)
        {
            plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
                "Unable to send message to discord: " + StaffChatPlugin.CHANNEL + " => null"
            );
            return;
        }
        
        plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
            "Sending message to discord channel: " + StaffChatPlugin.CHANNEL + " => " + channel
        );
        
        sender.accept(channel);
    }
    
    public void processConsoleChat(String message)
    {
        Objects.requireNonNull(message, "message");
        
        plugin.debug(getClass()).logConsoleChatMessage(message);
        
        ConsoleStaffChatMessageEvent event =
            plugin.events().call(new ConsoleStaffChatMessageEvent(message));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message: Console",() ->
                "Cancelled or text is empty"
            );
            return;
        }
        
        MappedPlaceholder placeholders = placeholders();
        placeholders.map("message", "content", "text").to(event::getText);
        
        sendFormattedChatMessage(null, CONSOLE_MESSAGE_FORMAT, placeholders);
        
        if (plugin.isDiscordSrvHookEnabled())
        {
            sendToDiscord(channel -> DiscordUtil.queueMessage(channel, message, true));
        }
        else
        {
            plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message: Console", () ->
                "DiscordSRV hook is not enabled, cannot send to discord"
            );
        }
    }
    
    public void processPlayerChat(Player author, String message)
    {
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(message, "message");
        
        plugin.debug(getClass()).logPlayerChatMessage(author, message);
        
        PlayerStaffChatMessageEvent event =
            plugin.events().call(new PlayerStaffChatMessageEvent(author, message));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message: Player", () ->
                "Cancelled or text is empty"
            );
            return;
        }
        
        MappedPlaceholder placeholders = placeholders(author);
        placeholders.map("message", "content", "text").to(event::getText);
        
        sendFormattedChatMessage(author, IN_GAME_MESSAGE_FORMAT, placeholders);
        
        if (plugin.isDiscordSrvHookEnabled())
        {
            sendToDiscord(channel -> {
                // Send to discord off the main thread (just like DiscordSRV does)
                plugin.async().run(() ->
                    DiscordSRV.getPlugin().processChatMessage(author, message, StaffChatPlugin.CHANNEL, false)
                );
            });
        }
        else
        {
            plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message: Player", () ->
                "DiscordSRV hook is not enabled, cannot send to discord"
            );
        }
    }
    
    public void processDiscordChat(User author, Message message)
    {
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(message, "message");
        
        plugin.debug(getClass()).logDiscordChatMessage(author, message);
        
        DiscordStaffChatMessageEvent event =
            plugin.events().call(new DiscordStaffChatMessageEvent(author, message, message.getContentStripped()));
        
        if (event.isCancelled() || event.getText().isEmpty())
        {
            plugin.debug(getClass()).log(ChatService.DISCORD, "Message", () -> "Cancelled or text is empty");
            return;
        }
    
        // Emoji Unicode -> Alias (library included with DiscordSRV)
        String text = EmojiParser.parseToAliases(event.getText());
        
        MappedPlaceholder placeholders = placeholders();
        
        placeholders.map("message", "content", "text").to(() -> text);
        placeholders.map("user", "name", "username", "sender").to(author::getName);
        placeholders.map("discriminator", "discrim").to(author::getDiscriminator);
        
        placeholders.map("nickname", "displayname").to(() -> {
            @NullOr Member member = message.getGuild().getMember(author);
            return (member == null ) ? "" : member.getEffectiveName();
        });
        
        sendFormattedChatMessage(author, DISCORD_MESSAGE_FORMAT, placeholders);
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
    
    public void notifyLeaveChat(Player leaver)
    {
        sendNotification(leaver, LEFT_CHAT_NOTIFICATION_SELF, LEFT_CHAT_NOTIFICATION_OTHERS);
    }
    
    public void notifyJoinChat(Player joiner)
    {
        sendNotification(joiner, JOIN_CHAT_NOTIFICATION_SELF, JOIN_CHAT_NOTIFICATION_OTHERS);
    }
}
