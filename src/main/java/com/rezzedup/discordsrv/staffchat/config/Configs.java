package com.rezzedup.discordsrv.staffchat.config;

import com.github.zafarkhaja.semver.Version;
import com.rezzedup.util.valuables.Adapter;
import community.leaf.configvalues.bukkit.YamlAccessor;
import org.bukkit.Sound;

import java.time.Instant;
import java.util.Optional;

public class Configs
{
    private Configs() { throw new UnsupportedOperationException(); }
    
    public static final Version NO_VERSION = Version.forIntegers(0,0,0);
    
    public static YamlAccessor<Version> VERSION =
        YamlAccessor.of(Adapter.of(
            object -> {
                try { return Optional.of(Version.valueOf(String.valueOf(object))); }
                catch (RuntimeException e) { return Optional.empty(); }
            },
            version -> Optional.of(String.valueOf(version))
        ));
    
    public static YamlAccessor<Instant> INSTANT =
        YamlAccessor.of(Adapter.of(
            object -> {
                try { return Optional.of(Instant.parse(String.valueOf(object))); }
                catch (RuntimeException e) { return Optional.empty(); }
            },
            instant -> Optional.of(String.valueOf(instant))
        ));
    
    public static YamlAccessor<Sound> SOUND =
        YamlAccessor.of(Adapter.of(
            object -> {
                try { return Optional.of(Sound.valueOf(String.valueOf(object))); }
                catch (RuntimeException e) { return Optional.empty(); }
            },
            sound -> Optional.of(sound.name())
        ));
}
