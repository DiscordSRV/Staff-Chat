package com.rezzedup.discordsrv.staffchat.config;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.util.Aggregates;
import com.rezzedup.discordsrv.staffchat.util.yaml.YamlDataFile;
import com.rezzedup.discordsrv.staffchat.util.yaml.YamlValue;

import java.util.List;

public class StaffChatConfig extends YamlDataFile
{
    private static final YamlValue.Maybe<String> VERSION =
        YamlValue.ofString("config-version").maybe();
    
    private static final YamlValue.Default<Boolean> ENABLE_METRICS =
        YamlValue.ofBoolean("metrics").defaults(true);
    
    @SuppressWarnings("unchecked")
    private static final List<YamlValue.Default<?>> VALUES =
        Aggregates.list(StaffChatConfig.class, YamlValue.Default.class);
    
    public StaffChatConfig(StaffChatPlugin plugin)
    {
        super(plugin.getPluginDirectoryPath(), "staffchat.config.yml");
        setupDefaults(VALUES);
    }
    
    public boolean isMetricsEnabled()
    {
        return ENABLE_METRICS.getOrDefault(data());
    }
}
