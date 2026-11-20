Minions Remastered - Minecraft 1.21.1 / NeoForge

Requires Java 21 and NeoForge 21.1.266 or newer in the 21.1 series.

Build
  ./gradlew assemble
  The mod JAR is in build/libs; use the one without the -sources suffix.
  Use gradlew.bat on Windows. Development runs: runClient and runServer.

Getting the staff
  Press M and choose Commit to Evil. Each deed costs 2 experience levels;
  four deeds earn a Master's Staff. There is no crafting recipe.
  Deed progress survives reconnects and death.

Controls
  Hold right-click, hover an action, then release to select it.
  Release over empty space to cancel. Arrows and toggles keep the menu open.
  Right-click air for the self-target menu, including carrying the master.
  Press M > Worker orders for strip mining, custom excavation, and other
  group orders. Hold the staff when giving orders.

  The left action on a block is Move here; hold Shift for Follow me.
  Valuable blocks offer vein mining, or strip mining with Shift.
  Select storage to stop work and deliver cargo; Shift keeps the job running.
  Hit your minion to release its passenger or boost work speed for 30 seconds.
  Hold attack with the staff to cast lightning. Commands and lightning cost
  hunger in survival.

  /minions lists the commands. For example, /minions dig 9 5 excavates a
  9-by-9 area, five blocks high, starting at the block you are looking at.
  Custom excavation supports odd widths 3..71 and heights 3..25. Strip mines
  are 80 blocks long, with one worker per gallery; repeat to assign more.

Workers
  Minions have 27 cargo slots and return to storage when full, then resume
  work. Without assigned storage they deliver to you. Full storage makes
  them wait. Cargo and jobs survive saves; recall also preserves cargo.
  Workers follow veins, fell trees, repair tunnel floors, and place torches.
  Work pauses while you are offline or in another dimension.

Configuration
  Settings are in minions_remastered-server.toml, including worker count,
  hunger costs, chunk loading, and mining limits. The defaults allow four
  minions. Additional tree/worthless block IDs and custom deeds are supported.
  Custom deed format: id|Display name|namespace:sound_event|seconds
  Custom audio needs a matching resource pack.
  Set legacyXpEasterEgg=false to disable the original iluvevil XP cheat.

This is a beta. Gameplay and compatibility still need in-game verification.

Original mod: https://github.com/AtomicStryker/atomicstrykers-minecraft-mods/tree/1.12.2/Minions
Original audio attribution: META-INF/ORIGINAL_MINIONS_ASSETS.txt in the JAR.
