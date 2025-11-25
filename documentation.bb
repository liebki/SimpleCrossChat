[CENTER][B]SimpleCrossChatPlus - Technical Documentation[/B][/CENTER]

[B]Configuration Overview[/B]
All settings are managed in the [I]options.yml[/I] file in your plugin folder. This file controls every aspect of the plugin’s behavior, from broker connection to chat formatting and feature toggles.

[B]Example options.yml[/B]
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

[B]Config Value Explanations[/B]
- [B]broker.address[/B]: The MQTT broker address. Use a public broker for testing, but a private broker for production.
- [B]broker.protocol[/B]: Choose tcp for standard or ssl for encrypted connections.
- [B]broker.port[/B]: The port your broker listens on (default: 1883 for tcp).
- [B]general.servername[/B]: Unique name for your server, shown in cross-server messages.
- [B]broadcastmessageformat[/B]: Customize how global chat messages appear.
- [B]debug.showmessages[/B]: Enable to see debug output in your console/logs.
- [B]communication.channel.id/key[/B]: Must match across servers to enable communication. Change for privacy.
- [B]enabled[/B]: Toggle global chat and other features on/off.

[B]How it works[/B]
All global and cross-server messages are sent through the broker, encrypted for privacy. You can disable global chat or other features to limit message transmission. Vault integration enables money transfer and advanced permissions. Only global and cross-server messages are sent—other plugin messages remain local.

[B]Permissions[/B]
Use permissions to control who can transfer entities, items, or money. Example:
- sccplus.admin.*
- sccplus.entity.transfer
- sccplus.entity.tier.owned
- sccplus.entity.tier.animals
- sccplus.entity.tier.everything

[B]MQTT Broker Setup[/B]
- [B]Public Broker[/B]: Quick setup, but less privacy. Use for testing.
- [B]Private Broker[/B]: Recommended for production. Host your own for full control and security. Supports SSL for encrypted connections.
- [B]SSL Setup[/B]: Configure your broker and plugin for SSL to encrypt all traffic.

[B]Advanced Configuration[/B]
- Customize message formats for branding.
- Enable debug mode for troubleshooting.
- Use channel keys for private communication.
- Toggle features per server or per player.

[B]Troubleshooting[/B]
- If messages aren’t sent, check broker address and port.
- Ensure channel id/key match across all servers.
- Verify Vault is installed for money transfer.
- Use debug.showmessages for more info.

[B]Step-by-Step Guides[/B]
1. Place the plugin JAR in your plugins folder.
2. Edit [I]options.yml[/I] to match your broker and server settings.
3. (Optional) Install Vault for economy features.
4. Restart your server.
5. Test global chat and cross-server features.
6. Adjust permissions for staff and players.

[B]Admin Tips[/B]
- Host your own broker for maximum privacy and control.
- All configuration is non-technical and can be managed by server admins—no coding required.
- Use permissions to restrict sensitive features.
- Regularly update your plugin for new features and security.

[B]FAQ[/B]
- [B]Can I use this with any server type?[/B] Yes, supports all major server types.
- [B]Is Vault required?[/B] Only for money transfer and if you want people to pay for transfers.
- [B]How do I secure my communication?[/B] Use a private broker.
- [B]Can I disable features?[/B] Yes, toggle features in options.yml.

[CENTER][B]Simple, secure, and powerful cross-server communication for every Minecraft admin.[/B][/CENTER]
