package com.rezzedup.discordsrv.staffchat.util.yaml;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.tlinkowski.annotation.basic.NullOr;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class YamlDataFile
{
    private final Path filePath;
    private final YamlConfiguration data;
    
    private boolean isUpdated = false;
    
    public YamlDataFile(Path directoryPath, String name)
    {
        this.filePath = directoryPath.resolve(name);
        this.data = new YamlConfiguration();
        
        
    }
    
    public Path getFilePath() { return filePath; }
    
    public YamlConfiguration data() { return data; }
    
    public boolean isUpdated() { return isUpdated; }
    
    public void updated(boolean state) { this.isUpdated = state; }
    
    public <T> Optional<T> get(YamlValue<T> value)
    {
        return value.get(data);
    }
    
    public <T> T getOrDefault(YamlValue.Default<T> value)
    {
        return value.getOrDefault(data);
    }
    
    public void set(String path, @NullOr Object value)
    {
        updated(true);
        data.set(path, value);
    }
    
    public <T> void set(YamlValue<T> path, @NullOr T value)
    {
        updated(true);
        path.set(data, value);
    }
    
    public void setupDefaults(List<YamlValue.Default<?>> defaults)
    {
        long valuesUpdated = defaults.stream().filter(value -> value.setAsDefaultIfUnset(data)).count();
        updated(valuesUpdated > 0);
    }
}
