package com.rezzedup.discordsrv.staffchat.util.yaml;

import com.google.gson.reflect.TypeToken;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import org.bukkit.configuration.ConfigurationSection;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class YamlValue<T>
{
    public static final TypeToken<YamlValue<?>> TYPE = new TypeToken<>() {};
    
    public static Builder<String> ofString(String path)
    {
        return new Builder<>(path, ConfigurationSection::getString);
    }
    
    public static Builder<Boolean> ofBoolean(String path)
    {
        return new Builder<>(path, ConfigurationSection::getBoolean);
    }
    
    public static Builder<Integer> ofInteger(String path)
    {
        return new Builder<>(path, ConfigurationSection::getInt);
    }
    
    public static Builder<Long> ofLong(String path)
    {
        return new Builder<>(path, ConfigurationSection::getLong);
    }
    
    public static Builder<Double> ofDouble(String path)
    {
        return new Builder<>(path, ConfigurationSection::getDouble);
    }
    
    public static Builder<List<String>> ofStringList(String path)
    {
        return new Builder<>(path, ConfigurationSection::getStringList);
    }
    
    public static Builder<List<Map<?, ?>>> ofMapList(String path)
    {
        return new Builder<>(path, ConfigurationSection::getMapList);
    }
    
    //
    //
    //
    
    protected final String path;
    protected final BiFunction<ConfigurationSection, String, T> getter;
    protected final List<String> migrations;
    
    public YamlValue(String path, BiFunction<ConfigurationSection, String, T> getter, @NullOr List<String> migrations)
    {
        this.path = Strings.requireNonEmpty(path, "path");
        this.getter = Objects.requireNonNull(getter, "getter");
        
        if (migrations == null)
        {
            this.migrations = Collections.emptyList();
        }
        else
        {
            List<String> paths = new ArrayList<>(migrations);
            paths.add(path);
            this.migrations = List.copyOf(paths);
        }
    }
    
    public String path() { return path; }
    
    public boolean hasMigrations() { return !migrations.isEmpty(); }
    
    public List<String> migrations() { return migrations; }
    
    public boolean isSet(ConfigurationSection yaml)
    {
        return yaml.isSet(path);
    }
    
    public @NullOr T apply(ConfigurationSection yaml)
    {
        return getter.apply(yaml, path);
    }
    
    public Optional<T> get(ConfigurationSection yaml)
    {
        return isSet(yaml) ? Optional.ofNullable(apply(yaml)) : Optional.empty();
    }
    
    public void set(ConfigurationSection yaml, @NullOr T value)
    {
        yaml.set(path, value);
    }
    
    //
    //
    //
    
    public static class Maybe<T> extends YamlValue<T>
    {
        public Maybe(String path, BiFunction<ConfigurationSection, String, T> getter, @NullOr List<String> migrations)
        {
            super(path, getter, migrations);
        }
    }
    
    public static class Default<T> extends YamlValue<T>
    {
        protected final T defaultValue;
        
        public Default(String path, BiFunction<ConfigurationSection, String, T> getter, @NullOr List<String> migrations, T defaultValue)
        {
            super(path, getter, migrations);
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        }
        
        public T getDefaultValue() { return defaultValue; }
        
        public T getOrDefault(ConfigurationSection yaml)
        {
            return get(yaml).orElse(defaultValue);
        }
        
        public void setAsDefault(ConfigurationSection yaml)
        {
            set(yaml, defaultValue);
        }
        
        public boolean setAsDefaultIfUnset(ConfigurationSection yaml)
        {
            if (isSet(yaml)) { return false; }
            set(yaml, defaultValue);
            return true;
        }
    }
    
    public static class Builder<T>
    {
        private @NullOr List<String> migrations = null;
        
        private final String path;
        private final BiFunction<ConfigurationSection, String, T> getter;
        
        public Builder(String path, BiFunction<ConfigurationSection, String, T> getter)
        {
            this.path = Strings.requireNonEmpty(path, "path");
            this.getter = Objects.requireNonNull(getter, "getter");
        }
        
        public Builder<T> migrates(String ... paths)
        {
            this.migrations = List.of(paths);
            return this;
        }
        
        public Maybe<T> maybe() { return new Maybe<>(path, getter, migrations); }
        
        public Default<T> defaults(T value) { return new Default<>(path, getter, migrations, value); }
    }
}
