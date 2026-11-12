Minions Remastered - Minecraft 1.21.1 / NeoForge

Requirements
  Java 21 JDK
  Minecraft 1.21.1
  NeoForge 21.1.266 or newer in the 21.1 series

Build
  ./gradlew assemble

The mod JAR and sources JAR are written to build/libs.
Use the mod JAR, without the -sources suffix, in your mods directory.

Development
  ./gradlew runClient
  ./gradlew runServer

On Windows, use gradlew.bat instead of ./gradlew.

Staff controls
  The staff menu uses larger buttons and previews and follows Minecraft's
  GUI Scale setting. Tooltips use the normal Minecraft text size.
  Hold right-click on a block or entity to open the staff menu.
  Hover an action and release right-click to apply it.
  Release over empty space to close without selecting an action.
  Release over a small adjustment arrow or toggle to change its setting;
  the menu stays open. Right-click and release again to adjust or apply.

This branch ports the existing 1.19.2 feature set to NeoForge 1.21.1.
It is still a beta and does not include every feature of the original
AtomicStryker Minions mod. Gameplay, visual, multiplayer, persistence,
and compatibility verification are deferred to the maintainer.
No automated tests are included in this port.
