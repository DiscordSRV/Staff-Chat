package com.rezzedup.discordsrv.staffchat.api;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import com.vdurmont.emoji.EmojiParser;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.core.entities.Channel;
import github.scarsz.discordsrv.dependencies.jda.core.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.core.entities.User;
import org.bukkit.entity.Player;

import static com.rezzedup.discordsrv.staffchat.util.Strings.colorful;

public class StaffChatApi {
	private final StaffChatPlugin plugin;
	
	public StaffChatApi(StaffChatPlugin plugin) { this.plugin = plugin; }
	
	public void inGameAnnounce(String message)
	{
		String content = colorful(message);
		
		plugin.getServer().getOnlinePlayers().stream()
				.filter(Permissions.ACCESS::isAllowedBy)
				.forEach(staff -> staff.sendMessage(content));
		
		plugin.getServer().getConsoleSender().sendMessage(content);
	}
	
	public void updatePlaceholdersThenAnnounceInGame(String format, MappedPlaceholder placeholders)
	{
		// If the value of %message% doesn't exist for some reason, don't announce.
		if (Strings.isEmptyOrNull(placeholders.get("message"))) { return; }
		inGameAnnounce(placeholders.update(format));
	}
	
	public void submitFromInGame(Player player, String message)
	{
		plugin.getDebugger().debug("[In-Game-Message] From:\"%s\" Message:\"%s\"", player.getName(), message);
		
		MappedPlaceholder placholders = new MappedPlaceholder();
		
		placholders.map("message", "content", "text").to(() -> message);
		placholders.map("user", "name", "username", "player", "sender").to(player::getName);
		placholders.map("nickname", "displayname").to(player::getDisplayName);
		
		updatePlaceholdersThenAnnounceInGame(plugin.getConfig().getString("in-game-message-format"), placholders);
		
		if (plugin.getDiscordChannel() != null)
		{
			plugin.getDebugger().debug("Sending message to discord channel: %s => %s", StaffChatPlugin.CHANNEL, plugin.getDiscordChannel());
			
			// Send to discord off the main thread (just like DiscordSRV does)
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
					DiscordSRV.getPlugin().processChatMessage(player, message, StaffChatPlugin.CHANNEL, false)
			);
		}
		else { plugin.getDebugger().debug("Unable to send message to discord: %s => null", StaffChatPlugin.CHANNEL); }
	}
	
	public void submitFromDiscord(User user, Guild guild, Channel channel, String message)
	{
		plugin.getDebugger().debug(
				"[Discord-Message] From:\"%s#%s\" Channel:\"%s\" Message:\"%s\"",
				user.getName(), user.getDiscriminator(), channel, message
		);
		
		// Emoji Unicode -> Alias (library included with DiscordSRV)
		String text = EmojiParser.parseToAliases(message);
		
		MappedPlaceholder placholders = new MappedPlaceholder();
		
		placholders.map("message", "content", "text").to(() -> text);
		placholders.map("user", "name", "username", "sender").to(user::getName);
		placholders.map("nickname", "displayname").to(guild.getMember(user)::getNickname);
		placholders.map("discriminator", "discrim").to(user::getDiscriminator);
		
		updatePlaceholdersThenAnnounceInGame(plugin.getConfig().getString("discord-message-format"), placholders);
	}
}
