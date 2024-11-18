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

import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import community.leaf.tasks.TaskContext;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.tlinkowski.annotation.basic.NullOr;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class Updater {
	public static final String RESOURCE_PAGE = "https://www.spigotmc.org/resources/44245/";
	
	public static final URI LATEST_UPDATE_API_ENDPOINT = URI.create("https://api.spiget.org/v2/resources/44245/versions/latest");
	
	private final StaffChatPlugin plugin;
	private final HttpClient client;
	private final String userAgent;
	
	private @NullOr TaskContext<BukkitTask> task;
	private @NullOr Version latestAvailableVersion;
	
	Updater(StaffChatPlugin plugin) {
		this.plugin = plugin;
		this.client = HttpClient.newHttpClient();
		
		this.userAgent =
			plugin.getName() + "/" + plugin.version() + " (Minecraft) " +
				plugin.getServer().getName() + "/" + plugin.getServer().getVersion();
		
		plugin.debug(getClass()).log("Init", () -> "User-Agent: " + userAgent);
		
		reload();
	}
	
	public boolean isRunningUpdateCheckTask() {
		return task != null;
	}
	
	public Optional<Version> latestAvailableVersion() {
		return Optional.ofNullable(latestAvailableVersion);
	}
	
	public Optional<Version> latestUpdateVersion() {
		return latestAvailableVersion().filter(plugin.version()::lessThan);
	}
	
	public boolean isOutdated() {
		return latestUpdateVersion().isPresent();
	}
	
	public void end() {
		if (task != null) {
			task.cancel();
		}
	}
	
	public void reload() {
		if (plugin.config().getOrDefault(StaffChatConfig.UPDATE_CHECKER_ENABLED)) {
			if (task == null || task.isCancelled()) {
				plugin.debug(getClass()).log("Reload", () -> "Update checker enabled: starting task");
				this.task = plugin.async().delay(10).ticks().every(7).hours().run(this::checkForUpdates);
			} else {
				plugin.debug(getClass()).log("Reload", () -> "Update checker enabled: task already running");
			}
		} else {
			latestAvailableVersion = null;
			
			if (task != null) {
				plugin.debug(getClass()).log("Reload", () -> "Update checker disabled: ending previously-enabled task");
				task.cancel();
			} else {
				plugin.debug(getClass()).log("Reload", () -> "Update checker disabled: will not start task");
			}
		}
	}
	
	private void checkForUpdates() {
		HttpRequest request =
			HttpRequest.newBuilder(LATEST_UPDATE_API_ENDPOINT)
				.setHeader("User-Agent", userAgent)
				.build();
		
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() != 200) {
				plugin.debug(getClass()).log("Update Check: Failure", () ->
					"Cannot check latest version, response status code: " + response.statusCode()
				);
				return;
			}
			
			JsonObject json = new JsonParser().parse(response.body()).getAsJsonObject();
			this.latestAvailableVersion = Version.valueOf(json.get("name").getAsString());
			
			plugin.debug(getClass()).log("Update Check: Success", () ->
				"Found latest available version: " + latestAvailableVersion + " (current: " + plugin.version() + ")"
			);
			
			plugin.sync().run(() -> notifyIfUpdateAvailable(plugin.getServer().getConsoleSender()));
		} catch (Exception e) {
			plugin.debug(getClass()).logException("Update Check: Failure", e);
		}
	}
	
	private void print(String text) {
		plugin.getLogger().info(ChatColor.BLUE + text);
	}
	
	public void notifyIfUpdateAvailable(CommandSender sender) {
		latestUpdateVersion().ifPresent(version ->
		{
			if (sender instanceof Player) {
				plugin.messages().notifyUpdateAvailable((Player) sender, version);
			} else if (sender instanceof ConsoleCommandSender) {
				print("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
				print("An update is available: " + version + " (current: " + plugin.version() + ")");
				print("Get the update @ " + RESOURCE_PAGE);
				print("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
			}
		});
	}
}
