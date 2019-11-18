package com.rezzedup.discordsrv.staffchat;

import org.bukkit.permissions.Permissible;

public enum Permissions
{
    MANAGE,
    ACCESS;
    
    public static final String PREFIX = "staffchat";
    
    private final String permission;
    
    Permissions() { this.permission = PREFIX + "." + name().toLowerCase(); }
    
    public String getPermissionNode() { return this.permission; }
    
    public boolean isAllowedBy(Permissible permissible) { return permissible.hasPermission(permission); }
}
