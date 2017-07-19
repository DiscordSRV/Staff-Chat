package com.rezzedup.discordsrv.staffchat;

import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.function.Predicate;

public enum Permissions
{
    ALL("*"),
    MANAGE("manage"),
    ACCESS("access");
    
    public static final String PREFIX = "staffchat";
    
    public static Predicate<CommandSender> any(Permissions ... of)
    {
        return (sender) -> Arrays.stream(of).map(Permissions::getPermissionNode).anyMatch(sender::hasPermission);
    }
    
    private final String permission;
    
    Permissions(String permission)
    {
        this.permission = PREFIX + "." + permission;
    }
    
    public String getPermissionNode()
    {
        return this.permission;
    }
}
