# ![](https://i.imgur.com/DZwTm1u.png)

**Spigot:** https://www.spigotmc.org/resources/discordsrv-staff-chat.44245/

DiscordSRV-Staff-Chat is a staff chat plugin that connects to a Discord channel (via [DiscordSRV](https://github.com/Scarsz/DiscordSRV)), allowing in-game staff to communicate with staff on Discord.

![](https://i.imgur.com/ssKGDTJ.gif) 

## Installation

* Add **DiscordSRV** and **DiscordSRV-Staff-Chat** to your `plugins/` directory.
* Restart the server.
* Add a `staff-chat` channel to **DiscordSRV**'s config.
* Execute: `/discord reload` *or* restart the server again.

### Adding a staff-chat channel

Add a **"staff-chat"** entry to DiscordSRV's config. Note: it's very important that this entry is called `"staff-chat"`.

```yaml
# Channel links from game to Discord
# syntax is Channels: {"in-game channel name from Minecraft": "numerical channel ID from Discord", "another in-game channel name from Minecraft": "another numerical channel ID from Discord"}
#
Channels: {"global": "000000000000000000", "staff-chat": "000000000000000000"}
```

Replace all those zeros with your staff chat's channel ID.

![](https://i.imgur.com/tXNU6Ei.gif)

```yaml
Channels: {"global": "000000000000000000", "staff-chat": "337769984539361281"}
```


## Commands & Permissions

### /staffchat

![](https://i.imgur.com/ILwkaqa.gif)

**Permission:** `staffchat.access`

**Aliases:** `/adminchat`, `/schat`, `/achat`, `/sc`, `/ac`, and `/a`

**Usage:**
- `/staffchat` - Toggle automatic staff chat.
- `/staffchat <message>` - Send a message to the staff chat.

### /leavestaffchat

![](https://i.imgur.com/BO3fgmC.png)

**Permission:** `staffchat.access`

**Aliases:** `/leaveadminchat`

**Usage:**
- `/leavestaffchat` - Stop receiving staff chat messages.
    - This can be disabled in the config if you don't want staff members to turn off their staff chat.

### /joinstaffchat

![](https://i.imgur.com/7EriNrS.png)

**Permission:** `staffchat.access`

**Aliases:** `/joinadminchat`

**Usage:**
- `/joinstaffchat` - Start receiving staff chat messages again if you previously left.

### /managestaffchat

**Permission:** `staffchat.manage`

**Aliases:** `/discordsrv-staff-chat`, `/discordsrvstaffchat`, `/discordstaffchat`, `/discordadminchat`, `/manageadminchat`

**Usage:**
- `/managestaffchat` - Display plugin information and command usage.
- `/managestaffchat reload` - Reload configs.
- `/managestaffchat debug` - Enable or disable debugging.

[![](https://bstats.org/signatures/bukkit/DiscordSRV-Staff-Chat.svg)](https://bstats.org/plugin/bukkit/DiscordSRV-Staff-Chat/11056)
