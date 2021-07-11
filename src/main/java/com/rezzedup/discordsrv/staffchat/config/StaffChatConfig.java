/*
 * The MIT License
 * Copyright © 2017-2021 RezzedUp and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.rezzedup.discordsrv.staffchat.config;

import com.github.zafarkhaja.semver.Version;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.util.constants.Aggregates;
import com.rezzedup.util.constants.annotations.AggregatedResult;
import community.leaf.configvalues.bukkit.DefaultYamlValue;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.configvalues.bukkit.migrations.Migration;

import java.util.List;

public class StaffChatConfig extends YamlDataFile
{
    public static final YamlValue<Version> VERSION =
        YamlValue.of("meta.config-version", Configs.VERSION).maybe();
    
    public static final DefaultYamlValue<Boolean> METRICS_ENABLED =
        YamlValue.ofBoolean("plugin.metrics")
            .migrates(Migration.move("metrics"))
            .defaults(true);
    
    public static final DefaultYamlValue<Boolean> UPDATE_CHECKER_ENABLED =
        YamlValue.ofBoolean("plugin.updates.check-for-updates").defaults(true);
    
    public static final DefaultYamlValue<Boolean> NOTIFY_IF_UPDATE_AVAILABLE =
        YamlValue.ofBoolean("plugin.updates.notify-operators").defaults(true);
    
    public static final DefaultYamlValue<Boolean> PERSIST_TOGGLES =
        YamlValue.ofBoolean("staffchat.toggles.chat-toggles-persist-after-restart").defaults(true);
    
    public static final DefaultYamlValue<Boolean> LEAVING_STAFFCHAT_ENABLED =
        YamlValue.ofBoolean("staffchat.toggles.let-staff-members-turn-off-staffchat").defaults(true);
    
    public static final DefaultYamlValue<Boolean> NOTIFY_IF_TOGGLE_ENABLED =
        YamlValue.ofBoolean("staffchat.toggles.notify-toggle-status-on-join")
            .migrates(Migration.move("notify-staff-chat-enabled-on-join"))
            .defaults(true);
    
    public static final DefaultYamlValue<Boolean> PREFIXED_CHAT_ENABLED =
        YamlValue.ofBoolean("staffchat.prefixed.enable-prefixed-chat-messages")
            .migrates(Migration.move("enable-prefixed-chat-messages"))
            .defaults(false);
    
    public static final DefaultYamlValue<String> PREFIXED_CHAT_IDENTIFIER =
        YamlValue.ofString("staffchat.prefixed.prefixed-chat-identifier")
            .migrates(Migration.move("prefixed-chat-identifier"))
            .defaults("@");
    
    @AggregatedResult
    public static final List<YamlValue<?>> VALUES = Aggregates.list(StaffChatConfig.class, YamlValue.type());
    
    public StaffChatConfig(StaffChatPlugin plugin)
    {
        super(plugin.directory(), "staff-chat.config.yml");
        plugin.debug(getClass()).logConfigValues(getFilePath(), VALUES);
        
        reloadsWith(() ->
        {
            if (isInvalid())
            {
                plugin.debug(getClass()).log("Reload", () -> "Couldn't load: " + getInvalidReason());
                return;
            }
            
            Version existing = get(VERSION).orElse(Configs.NO_VERSION);
            boolean isOutdated = existing.lessThan(plugin.version());
            
            if (isOutdated)
            {
                plugin.debug(getClass()).log("Reload", () -> "Updating outdated config: " + existing);
                set(VERSION, plugin.version());
            }
            
            headerFromResource("staffchat.config.header.txt");
            defaultValues(VALUES);
            
            if (isUpdated())
            {
                plugin.debug(getClass()).log("Reload", () -> "Saving updated config and backing up old config: v" + existing);
                backupThenSave(plugin.backups(), "v" + existing);
            }
        });
    }
}
