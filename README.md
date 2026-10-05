# GemPlugin (Elements)

Paper plugin: 9 elements, element nullifier, dragon egg bonuses, boss bar cooldowns.
Requires Paper (not Spigot/Bukkit). Built against Paper 1.21.4 API, api-version 1.21.

## Get the .jar (no programming needed)
1. Make a free GitHub account, create a new repository, and upload everything in this folder
   (drag and drop, including the hidden .github folder).
2. Open the repo's "Actions" tab, wait for "Build GemPlugin" to finish (about 1-2 minutes).
3. Click the finished run, download the "GemPlugin" artifact, unzip it -> GemPlugin.jar.
4. Put GemPlugin.jar in your server's plugins folder and restart.

Or build locally: install JDK 21 + Gradle, then run `gradle build` -> build/libs/GemPlugin.jar.

## Resource pack
Host GemPack.zip at a direct download link, then put the url (and sha1) in
plugins/GemPlugin/config.yml. Players will be prompted on join.

## Commands (ops)
/element <fire|wind|water|earth|lightning|ice|light|darkness|time|none> [player]
/nullifier [player]
Right-click the focus item to use your ability.
