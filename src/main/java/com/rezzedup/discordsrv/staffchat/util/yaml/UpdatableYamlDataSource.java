package com.rezzedup.discordsrv.staffchat.util.yaml;

import pl.tlinkowski.annotation.basic.NullOr;

import java.util.Optional;

public interface UpdatableYamlDataSource extends YamlDataSource
{
    boolean isUpdated();
    
    void updated(boolean state);
    
    default <T> Optional<T> get(YamlValue<T> value)
    {
        return value.get(data());
    }
    
    @SuppressWarnings("NullableProblems") // what even
    default <T> T getOrDefault(YamlValue.Default<T> value)
    {
        return value.getOrDefault(data());
    }
    
    default void set(String path, @NullOr Object value)
    {
        updated(true);
        data().set(path, value);
    }
}
