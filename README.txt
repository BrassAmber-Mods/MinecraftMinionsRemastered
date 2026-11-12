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

Evil deeds and the Master's Staff
  Press M (rebindable under Controls > Minions Remastered) and choose
  Commit to Evil. Pick one of three randomly offered original deeds.
  Each deed costs 2 experience levels, applies temporary blindness, and
  plays its original audio. After four paid offerings, the gods award a
  Master's Staff. Further sets of four deeds earn additional staffs.
  A full inventory causes the reward to drop at your feet.
  The temporary staff crafting recipe has been removed. Creative access
  to the staff is still available.

  Paid progress and a deed in progress survive reconnects and player
  death. The deed timer pauses while offline and resumes on return.
  The server config minions_remastered-server.toml has evilDeedXPCost:
  2 by default, 0 for free deeds, or -1 to disable deeds.
  Original audio attribution and license are in
  META-INF/ORIGINAL_MINIONS_ASSETS.txt inside the mod JAR.

Staff controls
  The staff menu uses larger buttons and previews and follows Minecraft's
  GUI Scale setting. Tooltips use the normal Minecraft text size.
  Hold right-click on a block or entity to open the staff menu.
  Hover an action and release right-click to apply it.
  Release over empty space to close without selecting an action.
  Release over a small adjustment arrow or toggle to change its setting;
  the menu stays open. Right-click and release again to adjust or apply.

This branch ports the 1.19.2 remake to NeoForge 1.21.1 and restores the
original evil-deed progression for obtaining the Master's Staff.
It is still a beta and does not include every feature of the original
AtomicStryker Minions mod. Gameplay, visual, multiplayer, persistence,
and compatibility verification are deferred to the maintainer.
No automated tests are included in this port.
