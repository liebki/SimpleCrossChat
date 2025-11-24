# SimpleCrossChatPlus Plugin

## Description

SimpleCrossChatPlus is an advanced Minecraft plugin for cross-server communication, entity transfer, item transfer, money transfer, and more. It integrates with MQTT brokers and Vault, supporting private messaging, server info, and player location features.

## Java Version

Requires **Java 21** for Minecraft 1.21 and above.

## Features

- **Global chat**: Real-time chat across servers (can be toggled or deactivated).

- **Cross-server private messaging**: Send private messages between servers.

- **Entity transfer**: Transfer animals, owned entities, or any entity type across servers.

- **Item transfer**: Send items between servers.

- **Money transfer**: Transfer money between servers (Vault integration required).

- **Player locate**: Locate players across servers.

- **Server info request**: Request information about other servers.

- **Server list request**: View a list of connected servers.

- **Toggle chat/notifications**: Enable or disable global chat and notifications per player.

- **Admin controls**: Disable/enable cross-server features for specific players.

- **Advanced permissions**: Granular control over all features.

- **Customizable communication security**: Host a private MQTT broker for secure communication.

- **Broadcast message formatting**: Customize message appearance.

- **Vault integration**: Soft-depend on Vault for economy and permissions support.

## Important Notes

### Global Message Handling

- **Every message is sent through the broker**: All global chat and cross-server messages are transmitted via the broker, encrypted but still accessible to parties connected to the broker.

- **Feature deactivation**: Global chat and other features can be deactivated in the configuration to reduce message transmission.

- **Third-party risks**: Using public brokers (e.g., test.mosquitto.org) means messages could theoretically be intercepted. Host your own broker for privacy.

- **Message scope**: Only global chat and cross-server messages are sent. Other plugin messages are not transmitted unless using the relevant commands.

## Setup

1. **Install an MQTT Broker** (default: test.mosquitto.org, but private broker recommended).
2. **Install the plugin**: Place the JAR in your server's `plugins` folder.
3. **Configure options** (optional): Update `options.yml` for broker address, server name, and message format.
4. **Install Vault** (optional): For money transfer and advanced permissions.
5. **Restart/Reload server** to apply changes.
6. **Verify communication** between servers.

## Commands

- `/scc` - Main command for plugin management and all features. Aliases: `/simplecrosschat`, `/crosschat`
  - `/scc com` - Request server list
  - `/scc disable <player>` - Admin disable/enable cross-server features for a player
  - `/scc etp <entity>` - Transfer entity
  - `/scc get <item/entity>` - Get item or entity
  - `/scc info <server>` - Request server info
  - `/scc itp <item>` - Transfer item
  - `/scc locate <player>` - Locate player across servers
  - `/scc toggle <disabled|notify>` - Toggle chat/notifications
  - `/scc transfer <player> <amount>` - Transfer money (Vault required)
- `/sccpm <player> <message>` - Send cross-server private message. Aliases: `/xpm`, `/crosspm`
- `/sccm <player> <message>` - Alternate command for cross-server private message

## Permissions

- `sccplus.admin.*` - All admin permissions (default: op)
  - Includes: `sccplus.admin.disable`, `sccplus.admin.com`, `sccplus.admin.info`, `sccplus.admin.bypass`
- `sccplus.entity.transfer` - Transfer entities cross-server (default: true)
- `sccplus.entity.tier.owned` - Transfer owned entities only (default: true)
- `sccplus.entity.tier.animals` - Transfer all animals (default: op)
- `sccplus.entity.tier.everything` - Transfer any entity type
- Additional permissions for item transfer, money transfer, player locate, etc.

## Security Recommendations

- **Host a private broker** for secure communication.
- **Encryption**: Messages are encrypted (`AES/CBC/PKCS5Padding`), but public brokers are not foolproof.
- **Inform players** about message transmission and privacy risks.

## Usage

1. Install the plugin in your server's `plugins` folder.
2. Optionally configure the `options.yml` file.
3. Install Vault for extended features (optional).
4. Restart or reload your server.
5. Use the commands to communicate and transfer data across servers.

## Disclaimer

This plugin is provided "as-is" without any warranty. The developer is not liable for privacy concerns, misuse, third-party interception, or legal implications. Server owners are responsible for secure and compliant usage.

---

#### Enable cross-server communication responsibly with **SimpleCrossChatPlus**!
