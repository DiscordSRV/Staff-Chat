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

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import pl.tlinkowski.annotation.basic.NullOr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

public class Debugger {
	private static String now() {
		return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
	}
	
	private static final DebugLogger DISABLED = message -> {
	};
	
	private final StaffChatPlugin plugin;
	private final Path debugToggleFile;
	private final Path debugLogFile;
	
	private boolean isEnabled;
	
	public Debugger(StaffChatPlugin plugin) {
		this.plugin = plugin;
		this.debugToggleFile = plugin.directory().resolve("debugging-is-enabled");
		this.debugLogFile = plugin.directory().resolve("debug.log");
		this.isEnabled = isToggleFilePresent();
	}
	
	private boolean isToggleFilePresent() {
		return Files.isRegularFile(debugToggleFile);
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public void setEnabled(boolean enabled) {
		if (this.isEnabled == enabled) {
			return;
		}
		
		this.isEnabled = enabled;
		
		try {
			if (enabled) {
				printThenWriteToLogFile("========== Starting Debugger ==========");
				if (!isToggleFilePresent()) {
					Files.createFile(debugToggleFile);
				}
			} else {
				printThenWriteToLogFile("========== Disabled Debugger ==========");
				Files.deleteIfExists(debugToggleFile);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public DebugLogger debug(Class<?> clazz) {
		return (isEnabled) ? message -> record("[" + clazz.getSimpleName() + "] " + message.get()) : DISABLED;
	}
	
	private void record(String message) {
		if (isEnabled) {
			printThenWriteToLogFile(message);
		}
	}
	
	private void printThenWriteToLogFile(String message) {
		plugin.getLogger().info("[Debug] " + message);
		
		try {
			if (!Files.isRegularFile(debugLogFile)) {
				Files.createFile(debugLogFile);
			}
			Files.write(debugLogFile, ("[" + now() + "] " + message + "\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void schedulePluginStatus(Class<?> clazz, String context) {
		if (!isEnabled) {
			return;
		}
		
		// Log status directly on the next tick.
		plugin.sync().run(() -> logPluginStatus(clazz, context + " (Initial)"));
		
		// Log status 30 seconds after so that DiscordSRV has a chance to connect.
		plugin.sync().delay(30).seconds().run(() -> logPluginStatus(clazz, context + " (30 Seconds)"));
	}
	
	private void logPluginStatus(Class<?> clazz, String context) {
		debug(clazz).recordDebugLogEntry(() ->
		{
			@NullOr Plugin discordSrv = plugin.getServer().getPluginManager().getPlugin(StaffChatPlugin.DISCORDSRV);
			@NullOr Object channel = plugin.getDiscordChannelOrNull();
			
			boolean isDiscordSrvEnabled = discordSrv != null && discordSrv.isEnabled();
			boolean isDiscordSrvHooked = plugin.isDiscordSrvHookEnabled();
			boolean isChannelReady = channel != null;
			
			return "[Status: " + context + "] " +
				"Is DiscordSRV installed and enabled? " + isDiscordSrvEnabled + " :: " +
				"Is DiscordSRV hooked? " + isDiscordSrvHooked + " :: " +
				"Is " + StaffChatPlugin.CHANNEL + " channel ready? " + isChannelReady + " (" + channel + ")";
		});
	}
	
	private static String handleContext(@NullOr Object context) {
		if (context instanceof Class<?>) {
			return ((Class<?>) context).getSimpleName();
		}
		if (context instanceof Event) {
			return ((Event) context).getEventName();
		}
		return String.valueOf(context);
	}
	
	private static String handleException(Throwable exception) {
		return exception.getClass().getSimpleName() + ": " + exception.getMessage();
	}
	
	@FunctionalInterface
	public interface DebugLogger {
		void recordDebugLogEntry(Supplier<String> message);
		
		default void log(Supplier<String> message) {
			recordDebugLogEntry(message);
		}
		
		default void log(@NullOr Object context, Supplier<String> message) {
			recordDebugLogEntry(() -> "[" + handleContext(context) + "] " + message.get());
		}
		
		default void log(ChatService source, @NullOr Object context, Supplier<String> message) {
			recordDebugLogEntry(() -> source.asPrefixInBrackets(handleContext(context)) + " " + message.get());
		}
		
		default void header(Supplier<String> message) {
			recordDebugLogEntry(() -> "---------- " + message.get() + " ----------");
		}
		
		default void logException(Object context, Throwable exception) {
			log(context, () -> {
				exception.printStackTrace();
				return handleException(exception);
			});
		}
		
		default <T extends Throwable> T failure(T exception) throws T {
			log(() -> handleException(exception));
			throw exception;
		}
		
		default <T extends Throwable> T failure(@NullOr Object context, T exception) throws T {
			log(context, () -> handleException(exception));
			throw exception;
		}
		
		default void logConsoleChatMessage(String message) {
			log(ChatService.MINECRAFT, "Message", () ->
				"from(<CONSOLE>) message(\"" + message + "\")"
			);
		}
		
		default void logPlayerChatMessage(Player author, String message) {
			log(ChatService.MINECRAFT, "Message", () ->
				"from(" + author.getName() + ") message(\"" + message + "\")"
			);
		}
		
		default void logDiscordChatMessage(User author, Message message) {
			log(ChatService.DISCORD, "Message", () ->
				"from(" + author.getName() + "#" + author.getDiscriminator() + ") message(\"" + message.getContentStripped() + "\")"
			);
		}
	}
}
