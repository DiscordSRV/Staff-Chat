package com.rezzedup.discordsrv.staffchat.config;

import com.rezzedup.util.valuables.Adapter;
import community.leaf.configvalues.bukkit.YamlAccessor;

import java.time.Instant;
import java.util.Optional;

public class Configs
{
    private Configs() { throw new UnsupportedOperationException(); }
    
    public static final String NO_VERSION = "0.0.0-new";
    
    public static YamlAccessor<Instant> INSTANT =
        YamlAccessor.of(Adapter.of(
            object -> {
                try { return Optional.of(Instant.parse(String.valueOf(object))); }
                catch (RuntimeException e) { return Optional.empty(); }
            },
            instant -> Optional.of(String.valueOf(instant))
        ));
}
