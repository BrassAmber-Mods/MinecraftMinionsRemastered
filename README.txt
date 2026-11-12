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

Worker inventories and hauling
  Each minion has 27 saved cargo slots. Harvested drops enter cargo;
  overflow remains in the world for the owner. Workers also collect
  nearby unclaimed drops after their normal pickup delay.
  Select a storage block or friendly inventory-bearing entity with the
  existing staff action. Workers walk there to unload when full, then
  resume their job. Without assigned storage they deliver to the owner.
  Finishing or pausing an order also sends existing cargo to storage.
  Full or inaccessible storage makes workers wait with their cargo.
  A destroyed/missing destination falls back to the owner while retaining
  the assignment. Storage in another dimension waits for reassignment.
  Recall/resummon preserves cargo, name, color, and sitting state by slot.

Excavation and scheduling
  Shafts and tunnels follow exposed, connected blocks in the c:ores tag.
  Ore searches default to 256 extra targets per layer/segment and a
  Manhattan radius of 16 from each vein's first ore. Searches are bounded
  and saved, including branches deferred while the target queue is full.
  Tunnels repair replaceable floor gaps with blackstone after excavation
  and add floor torches every seven segments when the area is dark.
  Staircase shaft geometry continues to use blackstone and stairs.
  These construction materials are supplied by the minions.
  Excavation seals adjacent fluids and clears reachable falling material
  before removing its support; unsafe blocks are skipped.

  One player order owns a shared queue with exclusive block assignments.
  Workers retry unreachable targets three times, then report a skipped
  count when finished. Jobs skip block entities, unbreakable blocks,
  incompatible tools, changed blocks, and denied protection hooks.
  Harvesting uses vanilla player/tool loot rules and NeoForge events
  under the owner's identity. Actual protection-mod support depends on
  those mods' handling of fake players and needs gameplay verification.
  Carry orders assign one worker and expire after 30 seconds if unsuccessful.

Persistence and server configuration
  Cargo, work cursors, queued targets, storage locations, and job dimensions
  survive saves/reconnects. Work pauses while the owner is offline, dead,
  or in another dimension, and resumes when the owner returns.
  Recall/resummon can move workers between dimensions; it does not move
  the order or its assigned storage. Sitting workers do not work or haul.
  Staff requests validate ownership, reach, bounds, and allowed selectors
  on the server, with a per-player request limit.
  The workers section of minions_remastered-server.toml controls scanBudget
  (128 source positions/player/tick), maxTunnelLength (1024), maxVeinBlocks
  (256), veinRadius (16), unreachableTicks (200), chunkLoading (true),
  maxChunksPerPlayer (12 work/storage chunk tickets), followOreVeins,
  placeTorches, and repairTunnels (all true).
  Chunk tickets are bounded per owner, expire after five seconds without
  renewal, and are released at logout. They operate only while the owner
  is alive and in that dimension. Disabling chunk loading makes unloaded
  work wait until those chunks load normally.

This branch ports the 1.19.2 remake to NeoForge 1.21.1 and restores the
original evil-deed progression for obtaining the Master's Staff.
It is still a beta and does not include every feature of the original
AtomicStryker Minions mod. Gameplay, visual, multiplayer, persistence,
and compatibility verification are deferred to the maintainer.
No automated tests are included in this port.
