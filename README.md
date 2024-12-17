# SimpleCrossChat Plugin

## Description

SimpleCrossChat is a lightweight Minecraft plugin designed for cross-server communication. Built for Minecraft version
1.21, it provides easy integration with MQTT brokers, allowing players to share messages globally across servers. While
powerful, it requires careful setup to ensure secure and proper usage.

## Java Version

Starting with Minecraft 1.21, Spigot and related platforms require **Java 21**.

## Features

- **Plug-and-play functionality**: Works immediately after adding to your plugins folder.
- **Global messaging support**: Players can chat across servers in real-time.
- **Customizable communication security**: Harden your setup by hosting a private MQTT broker.
- **MQTT-based messaging**: Lightweight and efficient.
- **Broadcast message formatting**: Customize the appearance of messages.
- **Future extensibility**: Planned support for message filtering through external plugins.

## Important Notes

### Global Message Handling

- **No Channels**: All global chat messages are sent to the broker, which then shares them with all communication
  partners. This means **every global message** is accessible to parties connected to the broker.
- **Third-party Risks**: When using public brokers, such as the default `test.mosquitto.org`, global messages could
  theoretically be intercepted by third parties. Hosting your own broker is highly recommended for privacy.
- **Message Scope**: Only **global chat messages** are broadcasted. Private messages, broadcasts, or messages from other
  plugins are not shared via the broker. Inform players about this behavior to ensure transparency.

## Setup

1. **Install an MQTT Broker**:
    - By default, the plugin uses `test.mosquitto.org` (a public broker).
    - For better security, set up and configure your private broker.

2. **Plugin Installation**:
    - Place the plugin JAR file in your server's `plugins` folder.

3. **Configuration** (Optional but Recommended):
    - Update the `options.yml` file in `plugins/simplecrosschat` to customize settings like the broker address.

4. **Restart/Reload Server**:
    - Apply changes by restarting or reloading your server.

5. **Verify Communication**:
    - Ensure the plugin connects to the broker and messages are transmitted.

## Plugin Configuration

Modify the `options.yml` file as needed. Here's an example:

```yaml
donottouch:
  configexists: true
debug:
  showmessages: false
general:
  info: '- INFO: id and key have to match on other servers, to enable a global chat,
    one value different and it wont work'
  servername: YourServerName
  broadcastmessageformat: '&a%PLAYER% &0| &f%MESSAGE%'
  enabled: true
  privacyinfo: '- INFO: Even tho the messages are not readable for outstanders, I
    would host my own broker for a safe communication!'
technical:
  broker:
    address: test.mosquitto.org
    protocol: tcp
    port: 1883
communication:
  channel:
    id: simplecrosschatwelcome
    key: z87d3z8hde3z8

```

## Security Recommendations

- **Host a Private Broker**: Using public brokers exposes communication to potential interception. Set up a private MQTT
  broker for secure operation.
- **Encryption**: The plugin uses `AES/CBC/PKCS5Padding` for encryption, but this is not foolproof against third-party
  access on public brokers.
- **Inform Players**: Let players know that global messages are shared via the broker and could theoretically be
  intercepted if a public broker is used.

## Usage

1. Install the plugin in your server's `plugins` folder.
2. Optionally configure the `options.yml` file with a custom broker address and settings.
3. Restart or reload your server to activate the plugin.
4. Players can now communicate globally across servers using the global chat.

## Disclaimer

This plugin is provided "as-is" without any warranty. The developer is not liable for:

- Privacy concerns or misuse of the plugin.
- Third-party interception or sniffing of messages.
- Legal implications of using this plugin in different regions (e.g., EU laws regarding user data).
  It is the sole responsibility of the server owner(s) or technical advisor(s) to ensure correct, secure, and legally
  compliant usage of this plugin.

---

#### Enable cross-server communication responsibly with **SimpleCrossChat**!