package com.rezzedup.discordsrv.staffchat.util.yaml;

import org.bukkit.configuration.ConfigurationSection;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.Optional;

public interface YamlDataSource
{
    ConfigurationSection data();
    
    <T> Optional<T> get(YamlValue<T> value);
    
    <T> T getOrDefault(YamlValue.Default<T> value);
    
    void set(String path, @NullOr Object value);
    
    default boolean has(YamlValue<?> value)
    {
        return value.isSet(data());
    }
    
    default <T> void set(YamlValue<T> path, @NullOr T value)
    {
        // call this interface's own set() so that implementers can see the value
        set(path.path(), value);
    }
}
