# Daniel Bot Setup

## Java
Daniel Bot needs at least Java 11 to run. This can be installed [here](https://jdk.java.net/), under "Reference
implementations".

## Environment Variables
Daniel Bot needs several environment variables in order to run. To apply these, make a file called `.env` in the project folder, and fill it out like
so:

```
DISCORD_TOKEN=put token here
GUILD_ONLY_COMMANDS=put server ID here
```

### Discord
Daniel Bot requires a Discord token to run. Here's how to get one:

1. Sign in to [https://discord.com/developers/](https://discord.com/developers/).
2. Start a new application.
3. Add a bot to your application.
4. Copy its token, and supply it as `DISCORD_TOKEN`.

### Guild Only Commands
Optionally, Daniel Bot can register its commands to a single guild. This is much faster to update than commands which register globally, but should
only be used in development. To enable this, provide the ID of a server as `GUILD_ONLY_COMMANDS`.
