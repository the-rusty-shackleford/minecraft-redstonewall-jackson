# Redstonewall Jackson

Redstone dust, repeaters and comparators on walls and on the undersides of blocks, behaving
exactly as they do on a floor. For Minecraft 1.21.1 on NeoForge.

## What it does

**Same items.** Hold redstone dust, a repeater or a comparator and click a wall face: it goes
on the wall. Click the underside of a block: it hangs from the block. Click a floor and you
get vanilla's floor form as always. The rule is the torch's: the clicked face decides, and
where it cannot (a corner), the direction you look. Nothing new appears in a creative tab or a
recipe.

**Same look.** Wall and ceiling dust use vanilla's dust textures and colours and glow with
their power; wall and ceiling repeaters and comparators are vanilla's models stood up or
turned over. Break one and it drops the vanilla item; pick-block gives the vanilla item.

**Same rules.** A wall is the floor stood up: up the wall is north on the map, down is south,
and left and right are what you see facing the wall. A ceiling is the floor turned over: lying
on your back with your head to the north, north is beyond your head and east on your left.
Everything vanilla's dust does, wall and ceiling dust do in that frame: a lone piece is a
cross, a click makes it a dot, it draws lines and corners toward what it touches, it climbs
onto a block that stands out of the wall and steps down past a gap, it loses one point of
power per block, it powers the block behind it strongly and what it points at weakly, and it
never powers the space in front of it. A wall repeater's input is behind it along the wall and
its output ahead; click it to cycle the delay; a diode of either kind pointing into its side
locks it. A wall comparator reads a container behind it along the wall (or a container two
blocks behind through a solid block, or an item frame on that block), compares or subtracts
its side inputs, and clicks between modes.

**Corners.** A run turns any corner between planes and draws it. Round an edge (over the top
of a wall onto the floor above, or from a ceiling down the side of the block it hangs from),
the two lines meet at the edge. Into a corner (a floor run up a wall, a wall run onto the next
wall, a wall run onto the ceiling, and each the other way), the piece in the corner draws
vanilla's climbing band up the face of the block beside it to the other run, exactly as floor
dust climbs a block with dust on top. Power crosses every corner one point down, as between any
two pieces of dust. Two pieces facing each other across a gap, on facing walls or on a floor
and a ceiling, do not connect, as two floor runs with air between do not.

The one thing a wall diode cannot do is lock a vanilla floor repeater beside it; vanilla
decides that with a check the mod cannot widen safely. A wall repeater locks any wall
repeater, and a floor repeater locks a wall one.

## Placing near the floor

Clicking a wall face just above a floor gives the wall form, as it does for a torch. For the
floor form, click the floor. Dust placed on a face that is not sturdy (glass, a fence) is
refused, as dust on such a floor is.

## Building and testing

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --offline --no-watch-fs build
```

Three tiers:

- `./gradlew test`: plain JUnit against the `domain` source set, compiled against nothing
  but the JDK. The frame (the floor, each wall and the ceiling, planar directions and their
  inverse), the wire rules (joints including the inside corner, shapes from scratch, one side
  changing, the click, power, signals in every frame) and the diode's sides and placement
  facing.
- `./gradlew runGameTestServer`: gametests on a real headless server (`src/gametest`, a mod of
  its own). Walls: a wall run against the floor run it is a turned copy of, power and joints
  block by block and both lighting a lamp; power falling by one a step up a wall; the seam up
  from a floor run, down onto a floor run, and onto a run along the wall's top; a wall
  repeater's delay at one and four ticks; a wall repeater locked by a sideways wall repeater; a
  wall comparator reading a chest behind it, comparing and subtracting a side comparator; the
  wall block going takes the dust and drops the item; a click on a wall face places the wall
  forms and spends the item, a click on a floor places vanilla's; the wall forms pick as the
  vanilla items. Corners: an L on one wall placed in both orders; a floor run up a wall from
  the dust at its foot and the wall run down onto it; a run turning the inside corner onto the
  wall it meets with the corner cell on either wall; a run round a pillar. Ceilings: a ceiling
  run against its floor twin; a wall run up onto the ceiling and a ceiling run down onto the
  wall, with the ceiling run in front of the top wall dust and directly above the wall run; a
  ceiling run round the edge of the block it hangs from; a ceiling repeater's delay; a click on
  the underside placing the ceiling forms. The server's exit code is not the assertion; the
  task reads the framework's "All N required tests passed" line.
- `./gradlew runPhotoBooth`: a client that builds the scenes and photographs them into
  `run/booth/screenshots`, then quits: the wall with a dust run climbing it from a floor run, a
  repeater, two comparators and a lamp (front, angle, close, seam, climb); a pillar with an L
  on each face (one photo per face); a stair from a floor run up a wall, along a roof's
  underside through a ceiling repeater and round the roof's edge to a lamp (front, from under
  the edge, close); and two walls meeting with a run turning the corner. What to look for:
  dust lines that run up the wall and along it with the right half toward the centre; the climb
  onto the block standing out of the wall; the repeater's torches on the wall with its arrow
  along the run; the comparators' torch triangle upright for one facing along the wall and one
  facing down; the colours darkening along the run as on a floor; every corner a continuous
  line, the inside corners with the band up the face of the block beside; the ceiling run
  hanging under the roof with the repeater's torches pointing down; each lamp lit.

Models and blockstates are generated: `uv run --no-project python devtools/models/generate.py
<minecraft-resources.jar>` reads vanilla's blockstates and models, turns them for each wall and
the ceiling, and writes `src/main/resources/assets/redstonewalljackson/`. Run it again after
changing it and commit the output.

## How it is built

The rules are written once against a *frame*, a plane as an orientation of the floor: up,
four planar directions and a support. The floor frame reproduces vanilla; a wall or ceiling
frame is the same rule turned. Vanilla's wire logic lives in the pure `domain.Wire` over a
`View` of the neighbourhood already turned into the frame, so "the same as the floor" is a
tested fact. The blocks are new (vanilla's have no face in their state), placed by the vanilla
items through an interaction event. The mod's touches on vanilla are one mixin class: injects
letting floor dust read wall dust's power where it steps onto a wall's top edge and where a
wall run rises directly above it, an inject drawing that floor dust's joint toward the wall as
its climb, and an accessor onto the flag vanilla silences its wires with while any wire
computes, shared so no wire of either kind reads itself back through a powered block. Why each
of these: `knowledge/decisions/D-0001.md` and `D-0002.md`.

## License

AGPL-3.0-or-later. The textures and models are vanilla's, referenced and turned, none copied.
