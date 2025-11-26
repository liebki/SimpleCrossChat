[CENTER]SimpleCrossChatPlus - Technical Documentation[/CENTER]

Configuration Overview
All settings are managed in the [I]options.yml[/I] file in your plugin folder. This file controls every aspect of the plugin’s behavior, from broker connection to chat formatting and feature toggles.

Example options.yml
[CODE]
donottouch:
  configexists: true
debug:
  showmessages: false
general:
  info: '- INFO: id and key have to match on other servers, to enable a global chat, one value different and it wont work'
  servername: YourServerName
  broadcastmessageformat: '&a%PLAYER% &0| &f%MESSAGE%'
  enabled: true
  privacyinfo: '- INFO: Even tho the messages are not readable for outstanders, I would host my own broker for a safe communication!'
technical:
  broker:
    address: broker.emqx.io
    protocol: tcp
    port: 1883
communication:
  channel:
    id: simplecrosschatwelcome
    key: z87d3z8hde3z8
[/CODE]

Config Value Explanations
- broker.address: The MQTT broker address. Use a public broker for testing, but a private broker for production.
- broker.protocol: Choose tcp for standard or ssl for encrypted connections.
- broker.port: The port your broker listens on (default: 1883 for tcp).
- general.servername: Unique name for your server, shown in cross-server messages.
- broadcastmessageformat: Customize how global chat messages appear.
- debug.showmessages: Enable to see debug output in your console/logs.
- communication.channel.id/key: Must match across servers to enable communication. Change for privacy.
- enabled: Toggle global chat and other features on/off.

How it works
All global and cross-server messages are sent through the broker, encrypted for privacy. You can disable global chat or other features to limit message transmission. Vault integration enables money transfer and advanced permissions. Only global and cross-server messages are sent—other plugin messages remain local.

Permissions
Use permissions to control who can transfer entities, items, or money. Example:
- sccplus.admin.*
- sccplus.entity.transfer
- sccplus.entity.tier.owned
- sccplus.entity.tier.animals
- sccplus.entity.tier.everything

MQTT Broker Setup
- Public Broker: Quick setup, but less privacy. Use for testing.
- Private Broker: Recommended for production. Host your own for full control and security. Supports SSL for encrypted connections.
- Old to New: Transfers from older Minecraft versions to newer ones work most often without problems. Newer versions can read and upgrade legacy data formats for entities and items.
- New to Old: Transfers from newer to older versions are unreliable. Items usually fail due to NBT/data differences, while some entities may work if their type and properties exist in both versions.
- Same Version: Transfers between servers running the same Minecraft version work as expected.
- Toggle features per server or per player.

Troubleshooting
- If messages aren’t sent, check broker address and port.
- Ensure channel id/key match across all servers.
- Verify Vault is installed for money transfer.
- Use debug.showmessages for more info.

Step-by-Step Guides
1. Place the plugin JAR in your plugins folder.
2. Edit [I]options.yml[/I] to match your broker and server settings.
3. (Optional) Install Vault for economy features.
4. Restart your server.
5. Test global chat and cross-server features.
6. Adjust permissions for staff and players.

Admin Tips
- Host your own broker for maximum privacy and control.
- All configuration is non-technical and can be managed by server admins—no coding required.
- Use permissions to restrict sensitive features.
- Regularly update your plugin for new features and security.

FAQ
- Can I use this with any server type? Yes, supports all major server types.
- Is Vault required? Only for money transfer and if you want people to pay for transfers.
- How do I secure my communication? Use a private broker.
- Can I disable features? Yes, toggle features in options.yml.

Version Compatibility
- Old to New: Transfers from older Minecraft versions to newer ones work most often without problems. Newer versions can read and upgrade legacy data formats for entities and items.
- New to Old: Transfers from newer to older versions are unreliable. Items usually fail due to NBT/data differences, while some entities may work if their type and properties exist in both versions.
- Same Version: Transfers between servers running the same Minecraft version work as expected.

[CENTER]Simple, secure, and powerful cross-server communication for every Minecraft admin.[/CENTER]
