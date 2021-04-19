package com.rezzedup.discordsrv.staffchat.util.yaml;

import com.rezzedup.discordsrv.staffchat.util.ExceptionHandler;
import com.rezzedup.discordsrv.staffchat.util.FileIO;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tlinkowski.annotation.basic.NullOr;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class YamlDataFile implements UpdatableYamlDataSource
{
    private final Path filePath;
    private final YamlConfiguration data;
    private final ExceptionHandler<Exception> exceptions;
    
    private boolean isLoaded = false;
    private boolean isUpdated = false;
    private @NullOr Exception invalid = null;
    
    public YamlDataFile(Path directoryPath, String name)
    {
        this(directoryPath, name, ExceptionHandler::print);
    }
    
    public YamlDataFile(Path directoryPath, String name, ExceptionHandler<Exception> exceptions)
    {
        this.filePath = directoryPath.resolve(name);
        this.data = new YamlConfiguration();
        this.exceptions = exceptions;
        
        reload();
    }
    
    public Path getFilePath() { return filePath; }
    
    @Override
    public YamlConfiguration data() { return data; }
    
    public boolean isLoaded() { return isLoaded; }
    
    @Override
    public boolean isUpdated() { return isUpdated; }
    
    @Override
    public void updated(boolean state) { this.isUpdated = state; }
    
    public boolean isInvalid() { return invalid != null; }
    
    public @NullOr Exception getInvalidReason() { return invalid; }
    
    public final void reload()
    {
        invalid = null;
        boolean isAlreadyLoaded = isLoaded;
        
        if (Files.isRegularFile(filePath))
        {
            try
            {
                data.loadFromString(Files.readString(filePath));
                isLoaded = true;
            }
            catch (InvalidConfigurationException | IOException | RuntimeException e)
            {
                invalid = e;
                exceptions.handle(e);
            }
        }
        
        if (isAlreadyLoaded) { handleReload(); }
    }
    
    // Override me.
    protected void handleReload() {}
    
    public String toYamlString() { return data.saveToString(); }
    
    public void save()
    {
        FileIO.write(getFilePath(), toYamlString(), exceptions);
    }
    
    public void backupThenSave(Path backupsDirectoryPath, String additionalNameInfo)
    {
        String fileName = filePath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        
        String name = (lastDot > 0) ? fileName.substring(0, lastDot) : fileName;
        String extension = (lastDot > 0) ? fileName.substring(lastDot) : "";
        
        if (!Strings.isEmptyOrNull(additionalNameInfo))
        {
            name += "." + additionalNameInfo;
        }
        
        FileIO.backup(getFilePath(), backupsDirectoryPath.resolve(name + extension), exceptions);
        save();
    }
    
    protected void setupHeader(String resource)
    {
        try
        {
            @NullOr URL resourceUrl = getClass().getClassLoader().getResource(resource);
            if (resourceUrl == null) { throw new IllegalStateException("No such resource: " + resource); }
            
            // Solution to FileSystemNotFoundException -> https://stackoverflow.com/a/22605905
            URI resourceUri = resourceUrl.toURI();
            String[] parts = resourceUri.toString().split("!");
            
            try (FileSystem fs = FileSystems.newFileSystem(URI.create(parts[0]), Map.of()))
            {
                String header = Files.readString(fs.getPath(parts[1]));
                if (Strings.isEmptyOrNull(header)) { return; }
                
                data.options().header(header);
                // Though this may technically "update" the config, if only the header
                // changes, that should not constitute rewriting the entire file.
            }
        }
        catch (IOException | URISyntaxException | RuntimeException e)
        {
            exceptions.handle(e);
        }
    }
    
    public void migrateValues(List<YamlValue<?>> values, ConfigurationSection existing)
    {
        for (YamlValue<?> value : values)
        {
            if (!value.hasMigrations()) { continue; }
            
            for (String path : value.migrations())
            {
                @NullOr Object existingValue = existing.get(path);
                if (existingValue == null) { continue; }
    
                Optional<?> newValue = get(value);
                if (newValue.isEmpty() || !existingValue.equals(newValue.get()))
                {
                    set(value.path(), existingValue);
                }
            }
        }
    }
    
    protected void setupDefaults(List<YamlValue<?>> defaults)
    {
        for (YamlValue<?> value : defaults)
        {
            if (!(value instanceof YamlValue.Default<?>)) { continue; }
            if (((YamlValue.Default<?>) value).setAsDefaultIfUnset(data)) { updated(true); }
        }
        
        migrateValues(defaults, data);
    }
}
