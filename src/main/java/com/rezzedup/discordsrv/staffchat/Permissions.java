package com.rezzedup.discordsrv.staffchat;

import org.bukkit.permissions.Permissible;

import java.util.Arrays;
import java.util.function.Predicate;

public enum Permissions
{
    ALL("*"),
    MANAGE("manage"),
    ACCESS("access");
    
    public static final String PREFIX = "staffchat";
    
    private final String permission;
    
    Permissions(String permission) { this.permission = PREFIX + "." + permission; }
    
    public String getPermissionNode() { return this.permission; }
    
    public boolean isAllowedBy(Permissible permissible) { return permissible.hasPermission(permission); }
}
