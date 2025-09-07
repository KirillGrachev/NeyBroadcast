Tired of boring, monotonous alerts? Want every event to make players excited? Then **NeyBroadcast** is exactly what you need. It's not just a message plugin - it's a full-fledged alert/purchase/advertising system designed to turn your server into a lively, breathing community where every player feels a part of something bigger.

**NeyBroadcast** combines three key features that used to require three different plugins:
**✅ Alerts** - important news, events, and announcements.
**✅ Ads** - promotion of teams, events, store.
**✅ Purchases** - purchase of in-game items.

**All this** - with support for **HEX** colors, integration with Vault and flexible configuration via config.yml. You yourself decide how messages will look: text, color, sound, maximum length - everything is under your control.

**📄 Configuration (config.yml)**
```yaml
# _   _            ____                      _               _
#| \ | | ___ _   _| __ ) _ __ ___   __ _  __| | ___ __ _ ___| |_
#|  \| |/ _ \ | | |  _ \| '__/ _ \ / _` |/ _` |/ __/ _` / __| __|
#| |\  |  __/ |_| | |_) | | | (_) | (_| | (_| | (_| (_| \__ \ |_
#|_| \_|\___|\__, |____/|_|  \___/ \__,_|\__,_|\___\__,_|___/\__|
#            |___/

# Developer - https://t.me/NeyOff
# Permissions: ney_broadcast.ad, ney_broadcast.buy, ney_broadcast.bc

# Common messages (global for all commands)
messages:
  no_console: "&cThis command can only be used by a player!"
  no_permission: "&cYou don't have permission to use this command!"
  contains_color_code: "&cYou cannot use & or § symbols in the message!"
  too_large: "&cMessage must not exceed {length} characters"

# Settings for each command
commands:

  bc:
    # Permission node
    permission: "ney_broadcast.bc"
    # Maximum message length
    max_length: 90
    # Cost to use (in coins)
    cost: 0

    # Command messages
    messages:
      usage: "&eUsage: /bc [text]"
      insufficient_funds: "&fYou don't have enough coins on your balance!"
      payment_success: "&f{count} coins have been deducted from your balance!"

    # Broadcast message template
    broadcast_template:
      - "&c"
      - "&c &7[#ff4242ANNOUNCEMENT&7] &fAuthor: &b%player%"
      - "&c &fText &7» &c%message%"
      - "&c"

    # Sound settings
    sound:
      enabled: true
      type: "BLOCK_NOTE_BLOCK_HAT"
      volume: 0.7
      pitch: 1.0

  ad:
    # Permission node
    permission: "ney_broadcast.ad"
    # Maximum message length
    max_length: 90
    # Cost to use (in coins)
    cost: 1500

    # Command messages
    messages:
      usage: "&eUsage: /ad [text]"
      insufficient_funds: "&fYou don't have enough coins on your balance!"
      payment_success: "&f{count} coins have been deducted from your balance!"

    # Broadcast message template
    broadcast_template:
      - "&c"
      - "&c &7[#63ffefADVERTISEMENT&7] &fAuthor: &b%player%"
      - "&c &fText &7» &c%message%"
      - "&c"

    # Sound settings
    sound:
      enabled: true
      type: "BLOCK_NOTE_BLOCK_HAT"
      volume: 0.7
      pitch: 1.0

  buy:
    # Permission node
    permission: "ney_broadcast.buy"
    # Maximum message length
    max_length: 90
    # Cost to use (in coins)
    cost: 2000

    # Command messages
    messages:
      usage: "&eUsage: /buy [text]"
      insufficient_funds: "&fYou don't have enough coins on your balance!"
      payment_success: "&f{count} coins have been deducted from your balance!"

    # Broadcast message template
    broadcast_template:
      - "&c"
      - "&c &7[#8642fcPURCHASE&7] &fAuthor: &b%player%"
      - "&c &fText &7» &c%message%"
      - "&c"

    # Sound settings
    sound:
      enabled: true
      type: "BLOCK_NOTE_BLOCK_HAT"
      volume: 0.7
      pitch: 1.0
```

PS: If you like the plugin, give it a star on GitHub (Clickable):

Photos:
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/723ba389-44ac-466f-9b5c-5812d2076cc7" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/a1fab741-248c-4061-8438-4adc721c7477" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/cf1c8bd0-8101-4ffb-b193-5dee4825902b" />
