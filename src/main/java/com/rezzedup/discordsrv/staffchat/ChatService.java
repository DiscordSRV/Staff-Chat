package com.rezzedup.discordsrv.staffchat;

public enum ChatService
{
    MINECRAFT("In-Game"),
    DISCORD("Discord");
    
    private final String prefix;
    
    ChatService(String prefix) { this.prefix = prefix; }
    
    public String asPrefixInBrackets(String context)
    {
        return "[" + prefix + ": " + context + "]";
    }
}
