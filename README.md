# DiscordSRV Staff Chat

DiscordSRV-Staff-Chat is a staff chat plugin that connects to a Discord channel (via [DiscordSRV](https://github.com/Scarsz/DiscordSRV)), allowing in-game staff to communicate with staff on Discord.

![](http://i.imgur.com/363hVvE.gif)

## Installation

* Add **DiscordSRV** and **DiscordSRV-Staff-Chat** to your `plugins/` directory.
* Restart the server.
* Add a `staff-chat` channel to **DiscordSRV**'s config.
* Execute: `/discord reload` *or* restart the server again.

### Adding a staff-chat channel

Add a **"staff-chat"** entry to DiscordSRV's config. Note: it's very important that this entry is called "staff-chat".

```yaml
# Channel links from game to Discord
# syntax is Channels: {"in-game channel name": "numerical channel ID from Discord", "another in-game channel name": "another numerical channel ID from Discord"}
# The first channel pair specified in this config option will be the "main" channel, used for sending player joins/quits/deaths/achievements/etc
#
Channels: {"global": "000000000000000000", "staff-chat": "000000000000000000"}
```
Replace all those zeros with your staff chat's channel ID.

![](http://i.imgur.com/Y8ncgsh.gif)

```yaml
Channels: {"global": "000000000000000000", "staff-chat": "337769984539361281"}
```

## Commands & Permissions

## /staffchat

**Permission:** `staffchat.access`

**Aliases:** `/adminchat`, `/schat`, `/achat`, `/sc`, `/ac`, and `/a`

**Usage:**
* `/staffchat` - Toggle automatic staff chat.
* `/staffchat <message>` - Send a message to the staff chat.

## /managestaffchat

**Permission:** `staffchat.manage`

**Aliases:** `/discordstaffchat`, `/discordadminchat`, `/manageadminchat`, `/managesc`, `/manageac`, `/dsc`, `/msc`, `/dac`, and `/mac`

**Usage:**
* `/managestaffchat` - Display plugin information and command usage.
* `/managestaffchat reload` - Reload the config.
