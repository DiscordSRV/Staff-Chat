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
package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.config.MessagesConfig;
import com.rezzedup.discordsrv.staffchat.events.ConsoleStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.DiscordStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.PlayerStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import community.leaf.configvalues.bukkit.DefaultYamlValue;
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

import java.util.Objects;
import java.util.function.Consumer;

public class MessageProcessor
{
    private final StaffChatPlugin plugin;
    
    MessageProcessor(StaffChatPlugin plugin)
    {
        this.plugin = plugin;
    }
    
    private void sendFormattedChatMessage(@NullOr Object author, DefaultYamlValue<String> format, MappedPlaceholder placeholders)
    {
        // If the value of %message% doesn't exist for some reason, don't announce.
        if (Strings.isEmptyOrNull(placeholders.get("message"))) { return; }
        
        String formatted = plugin.messages().getOrDefault(format);
        
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
            StaffChatProfile profile = plugin.data().getOrCreateProfile(player);
            
            // Author left the staff chat but is sending a message there...
            if (!profile.receivesStaffChatMessages())
            {
                String reminder = Strings.colorful(placeholders.update(
                    plugin.messages().getOrDefault(MessagesConfig.LEFT_CHAT_NOTIFICATION_REMINDER))
                );
                
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
            plugin.debug(getClass()).log(ChatService.MINECRAFT, event,() -> "Cancelled or text is empty");
            return;
        }
        
        MappedPlaceholder placeholders = plugin.messages().placeholders();
        placeholders.map("message", "content", "text").to(event::getText);
        
        sendFormattedChatMessage(null, MessagesConfig.IN_GAME_CONSOLE_FORMAT, placeholders);
        
        if (plugin.isDiscordSrvHookEnabled())
        {
            String discordMessage = placeholders.update(
                plugin.messages().getOrDefault(MessagesConfig.DISCORD_CONSOLE_FORMAT)
            );
            
            sendToDiscord(channel -> DiscordUtil.queueMessage(channel, discordMessage, true));
        }
        else
        {
            plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
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
            plugin.debug(getClass()).log(ChatService.MINECRAFT, event, () -> "Cancelled or text is empty");
            return;
        }
        
        MappedPlaceholder placeholders = plugin.messages().placeholders(author);
        placeholders.map("message", "content", "text").to(event::getText);
        
        sendFormattedChatMessage(author, MessagesConfig.IN_GAME_PLAYER_FORMAT, placeholders);
        
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
            plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
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
        
        MappedPlaceholder placeholders = plugin.messages().placeholders();
        
        placeholders.map("message", "content", "text").to(() -> text);
        placeholders.map("user", "name", "username", "sender").to(author::getName);
        placeholders.map("discriminator", "discrim").to(author::getDiscriminator);
        
        placeholders.map("nickname", "displayname").to(() -> {
            @NullOr Member member = message.getGuild().getMember(author);
            return (member == null ) ? "" : member.getEffectiveName();
        });
        
        sendFormattedChatMessage(author, MessagesConfig.IN_GAME_DISCORD_FORMAT, placeholders);
    }
}
