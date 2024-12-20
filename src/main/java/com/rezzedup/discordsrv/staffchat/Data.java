/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
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
package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingStaffChatToggleEvent;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.configvalues.bukkit.util.Sections;
import community.leaf.tasks.TaskContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.tlinkowski.annotation.basic.NullOr;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Data extends YamlDataFile implements StaffChatData {
	private static final String PROFILES_PATH = "staff-chat.profiles";
	
	private final Map<UUID, Profile> profilesByUuid = new HashMap<>();
	
	private final StaffChatPlugin plugin;
	
	private @NullOr TaskContext<BukkitTask> task = null;
	
	Data(StaffChatPlugin plugin) {
		super(plugin.directory().resolve("data"), "staff-chat.data.yml");
		this.plugin = plugin;
		
		// Load persistent toggles.
		if (plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) {
			Sections.get(data(), PROFILES_PATH).ifPresent(section ->
			{
				for (String key : section.getKeys(false)) {
					try {
						getOrCreateProfile(UUID.fromString(key));
					} catch (IllegalArgumentException ignored) {
					}
				}
			});
		}
		
		// Start the save task.
		task = plugin.async().every(2).minutes().run(() -> {
			if (isUpdated()) {
				save();
			}
		});
		
		// Update profiles of all online players when reloaded.
		reloadsWith(() -> plugin.getServer().getOnlinePlayers().forEach(this::updateProfile));
	}
	
	protected void end() {
		if (task != null) {
			task.cancel();
		}
		if (isUpdated()) {
			save();
		}
	}
	
	@Override
	public StaffChatProfile getOrCreateProfile(UUID uuid) {
		return profilesByUuid.computeIfAbsent(uuid, k -> new Profile(plugin, this, k));
	}
	
	@Override
	public Optional<StaffChatProfile> getProfile(UUID uuid) {
		return Optional.ofNullable(profilesByUuid.get(uuid));
	}
	
	public void updateProfile(Player player) {
		@NullOr Profile profile = profilesByUuid.get(player.getUniqueId());
		
		if (Permissions.ACCESS.allows(player)) {
			// Ensure that this staff member has an active profile.
			if (profile == null) {
				profile = (Profile) getOrCreateProfile(player);
			}
			
			// If leaving the staff chat is disabled...
			if (!plugin.config().getOrDefault(StaffChatConfig.LEAVING_STAFFCHAT_ENABLED)) {
				// ... and this staff member previously left the staff chat ...
				if (profile.left != null) {
					// Bring them back.
					profile.receivesStaffChatMessages(true);
				}
			}
		} else {
			// Not a staff member but has a loaded profile...
			if (profile != null) {
				// Notify that they're no longer talking in staff chat.
				if (profile.automaticStaffChat()) {
					profile.automaticStaffChat(false);
				}
				
				// No longer staff, delete data.
				profile.clearStoredProfileData();
				
				// Remove from the map.
				profilesByUuid.remove(player.getUniqueId());
			}
		}
	}
	
	static class Profile implements StaffChatProfile {
		static final YamlValue<Instant> AUTO_TOGGLE_DATE = YamlValue.ofInstant("toggles.auto").maybe();
		
		static final YamlValue<Instant> LEFT_TOGGLE_DATE = YamlValue.ofInstant("toggles.left").maybe();
		
		static final YamlValue<Boolean> MUTED_SOUNDS_TOGGLE = YamlValue.ofBoolean("toggles.muted-sounds").maybe();
		
		private final StaffChatPlugin plugin;
		private final YamlDataFile yaml;
		private final UUID uuid;
		
		private @NullOr Instant auto;
		private @NullOr Instant left;
		private boolean mutedSounds = false;
		
		Profile(StaffChatPlugin plugin, YamlDataFile yaml, UUID uuid) {
			this.plugin = plugin;
			this.yaml = yaml;
			this.uuid = uuid;
			
			if (plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) {
				Sections.get(yaml.data(), path()).ifPresent(section ->
				{
					auto = AUTO_TOGGLE_DATE.get(section).orElse(null);
					left = LEFT_TOGGLE_DATE.get(section).orElse(null);
					mutedSounds = MUTED_SOUNDS_TOGGLE.get(section).orElse(false);
				});
			}
		}
		
		String path() {
			return PROFILES_PATH + "." + uuid;
		}
		
		@Override
		public UUID uuid() {
			return uuid;
		}
		
		@Override
		public Optional<Instant> sinceEnabledAutoChat() {
			return Optional.ofNullable(auto);
		}
		
		@Override
		public boolean automaticStaffChat() {
			return auto != null;
		}
		
		@Override
		public void automaticStaffChat(boolean enabled) {
			if (plugin.events().call(new AutoStaffChatToggleEvent(this, enabled)).isCancelled()) {
				return;
			}
			
			auto = (enabled) ? Instant.now() : null;
			updateStoredProfileData();
		}
		
		@Override
		public Optional<Instant> sinceLeftStaffChat() {
			return Optional.ofNullable(left);
		}
		
		@Override
		public boolean receivesStaffChatMessages() {
			// hasn't left the staff chat or leaving is disabled outright
			return left == null || !plugin.config().getOrDefault(StaffChatConfig.LEAVING_STAFFCHAT_ENABLED);
		}
		
		@Override
		public void receivesStaffChatMessages(boolean enabled) {
			if (plugin.events().call(new ReceivingStaffChatToggleEvent(this, enabled)).isCancelled()) {
				return;
			}
			
			left = (enabled) ? null : Instant.now();
			updateStoredProfileData();
		}
		
		@Override
		public boolean receivesStaffChatSounds() {
			return !mutedSounds;
		}
		
		@Override
		public void receivesStaffChatSounds(boolean enabled) {
			mutedSounds = !enabled;
		}
		
		boolean hasDefaultSettings() {
			return auto == null && left == null && !mutedSounds;
		}
		
		void clearStoredProfileData() {
			if (!plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) {
				return;
			}
			
			yaml.data().set(path(), null);
			yaml.updated(true);
		}
		
		void updateStoredProfileData() {
			if (!plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) {
				return;
			}
			
			if (hasDefaultSettings()) {
				clearStoredProfileData();
				return;
			}
			
			ConfigurationSection section = Sections.getOrCreate(yaml.data(), path());
			
			AUTO_TOGGLE_DATE.set(section, auto);
			LEFT_TOGGLE_DATE.set(section, left);
			MUTED_SOUNDS_TOGGLE.set(section, mutedSounds);
			
			yaml.updated(true);
		}
	}
}
