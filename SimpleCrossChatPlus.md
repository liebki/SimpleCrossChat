# SimpleCrossChatPlus: Premium Cross-Network Communications Suite

## Overview
SimpleCrossChatPlus is the premium successor to SimpleCrossChat, layering asset logistics, moderated routing, and monetizable utilities on top of the original MQTT chat backbone from ciranux/liebki (Ciran). It keeps the “zero-proxy-scripting” promise while enabling networks to teleport entities, items, currency, and private conversations between Paper or Spigot servers through the same secure broker channel.

## Market Positioning
- **Target**: Multi-server survival/adventure networks that need cross-server continuity for pets, inventories, and economies without investing in proxy patches.
- **Competitive Edge**: Unified MQTT transport for chat, assets, and currency with Vault-aware pricing policies and granular permission gating.
- **Monetization**: Price at €7.99 as a premium upgrade or bundle with a cross-network utilities pack (Discord relay, analytics) at €14.99.

## Core Pillars & Feature Sets
### 1. **Conversation Control & Presence**
- Global chat inherits the SimpleCrossChat reliability with player-level toggles: `/scc disabled` silences outbound relays, while receive notifications can be toggled via `/scc notify off` (configurable aliases).
- Ad-hoc group channels using `/chat <group> <msg>` create ephemeral MQTT topics so teams can hold temporary meetings without Discord.
- Cross-server private messages via `/pc <player> <msg>` route through the broker, respecting ignore lists and locale formatting.

### 2. **Cross-Server Asset Teleportation**
- **Entity Transfer**: `/scc etp <receiverServer>` serializes the looked-at tamed entity, encrypts its NBT, and transmits it with a generated UID. Players redeem on the destination with `/scc get <UID>`; admins bypass restrictions, players need the `sccplus.entity.transfer` permission.
- **Safety Nets**: If spawning fails because of version drift, the plugin catches the error and hands the player a spawn egg matching the entity type. Configurable tiers (`transport-owned-only`, `transport-all-animals`, `transport-everything`) govern eligibility and Vault cost (defaults: 50/100/500).
- **Item Courier**: `/scc itp <server>` bundles the held item stack (metadata + enchants) and sends it cross-network. If the receiver’s version rejects the payload, `/scc get <UID>` returns a vanilla base item as a fallback, with audit logging for staff.

### 3. **Economy & Event Sync**
- Vault-backed money transfer: `/scc transfer <amount> <player>` debits the sender instantly and credits the remote player after broker confirmation.
- Achievement broadcasts leverage advancement hooks to publish server-agnostic achievement toasts globally.
- Player kill / entity kill broadcasts create opt-in bragging rights for PvP hubs and mob grinders; thresholds and formatting are configurable per server.

### 4. **Operations & Intelligence**
- `/scc com` lists every active server registered with the broker, their friendly names, UUIDs, and latency, so staff can target the correct endpoint for teleports.
- `/scc info <server>` requests live player counts and TPS for a specific node; the broker fans out the request and only the named server replies with encrypted payload scoped to the requesting player.
- Built-in rate limiting, permission nodes, and configurable Vault deductions ensure asset moves stay profitable and safe.
- Audit trails for every entity/item/money transfer with reason codes (success, version-downgrade, fallback-egg) stored locally or in JDBC for compliance.

### 5. **Broker Message Workflow**
- Every advanced feature (teleports, info lookups, currency transfers) follows a consistent handshake: client command → broker broadcast → targeted server processing → broker response → requester delivery.
- Common middleware handles encryption/decryption, permission checks, and timeout handling before responses are surfaced to players.

## Technical Specifications
- **Platforms**: Paper, Spigot 1.20.6–1.21+ (Folia-safe internals for async serialization).
- **Language/Runtime**: Java 21 plugin leveraging the SimpleCrossChat MQTT codec.
- **Dependencies**: MQTT broker (Mosquitto, HiveMQ, EMQX), Vault (economy connector), LuckPerms or equivalent for permissions. Optional: PlaceholderAPI for message formatting, bStats for metrics.
- **Configuration**: `plugins/SimpleCrossChatplus/config.yml` handles broker credentials, Vault pricing tiers, broadcast formats, toggle defaults, retry windows, and fallback behaviour.
- **Security Guardrails**: AES/CBC encrypted payloads with per-node keys, entity whitelist/blacklist, configurable max payload size, and signature verification to prevent spoofed UID claims.

## Implementation Outline
1. **Provision Broker & Economy** – Ensure MQTT broker credentials and a Vault-backed economy provider are available to all servers.
2. **Deploy Plugin** – Install the SimpleCrossChatPlus JAR on every participating Paper/Spigot node; delete legacy SimpleCrossChat once migration tests pass.
3. **Configure Keys & Pricing** – Align broker channel IDs/keys, set Vault deduction per tier, and choose default toggle states for notifications and outbound chat.
4. **Assign Permissions** – Grant granular nodes (`sccplus.entity.transfer`, `sccplus.entity.tier.all`, `sccplus.item.transfer`, `sccplus.money.transfer`, etc.) to ranks via LuckPerms.
5. **Validate Flows** – Test `/scc etp` with owned pets, `/scc itp` with enchanted gear, `/scc transfer` for currency, and `/pc` for private messaging on staging servers.
6. **Monitor & Iterate** – Review audit logs, adjust price multipliers, and tune broadcast formatting to match brand voice.

## Command Reference
| Command | Role | Description |
| --- | --- | --- |
| `/scc etp <server>` | Player/Admin | Queue looked-at entity for transfer; charges Vault based on configured tier. |
| `/scc itp <server>` | Player/Admin | Sends held item stack to another server; generates redeemable UID. |
| `/scc get <UID>` | Player/Admin | Redeems pending entity/item payloads into the world or inventory. |
| `/scc transfer <amount> <player>` | Player/Admin | Cross-server Vault money transfer with confirmation feedback. |
| `/scc com` | Admin | Lists broker-connected servers with IDs, version tags, and status. |
| `/scc info <server>` | Admin | Queries live player count and TPS; only the named server returns encrypted stats to the requester. |
| `/scc disabled` | Player | Toggles sending own global chat to other servers; companion `/scc notify <on|off>`. |
| `/pc <player> <msg>` | Player | Sends private cross-server message with delivery receipts. |
| `/chat <group> <msg>` | Player | Creates/joins a temporary broker channel for party chat; expires after inactivity window. |
| `/scc disable <playername>` | Admin | Disables all cross-server functionality for the specified player. The restriction is persisted in a YAML file. |

## Configurable Safeguards & Pricing
- `transport-mode`: `OWNED_ONLY` (default), `ALL_ANIMALS`, `EVERYTHING`; each maps to a Vault cost multiplier (50/100/500 by default).
- `version-fallback`: `spawn-egg` (entities) and `base-item` (items) toggles ensure players never lose assets silently.
- `message-broadcasts`: enable/disable achievement and kill feeds per server, with MiniMessage templates.
- `notification-defaults`: choose whether new players auto-subscribe to global messages or require opt-in.
- `audit-storage`: YAML, SQLite, or MySQL target for logging transfers and fallback events.

