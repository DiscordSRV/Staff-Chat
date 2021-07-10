package com.rezzedup.discordsrv.staffchat;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

public interface StaffChatAPI
{
    boolean isDiscordSrvHookEnabled();
    
    @NullOr TextChannel getDiscordChannelOrNull();
    
    void submitMessageFromInGame(Player author, String message);
    
    void submitMessageFromDiscord(User author, Message message);
    
    
}
