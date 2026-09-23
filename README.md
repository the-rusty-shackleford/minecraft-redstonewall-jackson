# Redstonewall Jackson

Redstone dust, repeaters and comparators on walls, behaving exactly as they do on a floor.
For Minecraft 1.21.1 on NeoForge.

## What it does

**Same items.** Hold redstone dust, a repeater or a comparator and click a wall face: it goes
on the wall. Click a floor and you get vanilla's floor form as always. The rule is the
torch's: the clicked face decides, and where it cannot (a corner), the direction you look.
Nothing new appears in a creative tab or a recipe.

**Same look.** Wall dust uses vanilla's dust textures and colours and glows with its power;
wall repeaters and comparators are vanilla's models stood up. Break one and it drops the
vanilla item; pick-block gives the vanilla item.

**Same rules.** A wall is the floor stood up. Up the wall is north on the map, down is south,
and left and right are what you see facing the wall. Everything vanilla's dust does, wall dust
does in that frame: a lone piece is a cross, a click makes it a dot, it draws lines and
corners toward what it touches, it climbs onto a block that stands out of the wall and steps
down past a gap, it loses one point of power per block, it powers the wall block behind it
strongly and what it points at weakly, and it never powers the space in front of it. A wall
repeater's input is behind it along the wall and its output ahead; click it to cycle the
delay; a diode of either kind pointing into its side locks it. A wall comparator reads a
container behind it along the wall (or a container two blocks behind through a solid block, or
an item frame on that block), compares or subtracts its side inputs, and clicks between modes.

**Seams.** A floor run meets a wall run wherever they touch, losing one point of power
across the joint as between any two pieces of dust: a floor run ending in front of the
lowest wall dust, a wall run rising from a piece of floor dust below it, and a floor run on
the wall's top edge above the highest wall dust. The one thing a wall diode cannot do is
lock a vanilla floor repeater beside it; vanilla decides that with a check the mod cannot
widen safely. A wall repeater locks any wall repeater, and a floor repeater locks a wall one.

**Not yet.** Ceilings. The design admits them in one line when asked.

## Placing on a wall near the floor

Clicking a wall face just above a floor gives the wall form, as it does for a torch. For the
floor form, click the floor. Dust placed on a wall face where the wall block is not sturdy
(glass, a fence) is refused, as dust on such a floor is.

## Building and testing

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew --offline --no-watch-fs build
```

Three tiers:

- `./gradlew test`: plain JUnit against the `domain` source set, compiled against nothing
  but the JDK. The frame (the floor and each wall, planar directions and their inverse), the
  wire rules (joints, shapes from scratch, one side changing, the click, power, signals in
  every frame) and the diode's sides and placement facing.
- `./gradlew runGameTestServer`: gametests on a real headless server (`src/gametest`, a mod of
  its own). A wall run against the floor run it is a turned copy of, power and joints block by
  block and both lighting a lamp; power falling by one a step up a wall; the seam up from a
  floor run, down onto a floor run, and onto a run along the wall's top; a wall repeater's delay
  at one and four ticks; a wall repeater locked by a sideways wall repeater; a wall comparator
  reading a chest behind it, comparing and subtracting a side comparator; the wall block going
  takes the dust and drops the item; a click on a wall face places the wall forms and spends the
  item, a click on a floor places vanilla's; the wall forms pick as the vanilla items. The
  server's exit code is not the assertion; the task reads the framework's "All N required tests
  passed" line.
- `./gradlew runPhotoBooth`: a client that builds a wall with a dust run climbing it from a
  floor run, a repeater, two comparators and a lamp, powers it, photographs it from the front,
  at an angle, close up and from the seam end into `run/booth/screenshots`, and quits. What to
  look for: dust lines that run up the wall and along it with the right half toward the centre,
  the climb onto the block standing out of the wall, the repeater's torches on the wall with its
  arrow along the run, the comparators' torch triangle upright for one facing along the wall and
  one facing down, and the colours darkening along the run as on a floor.

Models and blockstates are generated: `uv run --no-project python devtools/models/generate.py
<minecraft-resources.jar>` reads vanilla's blockstates and models, turns them for each wall, and
writes `src/main/resources/assets/redstonewalljackson/`. Run it again after changing it and commit
the output.

## How it is built

The rules are written once against a *frame*, a plane as an orientation of the floor: up,
four planar directions and a support. The floor frame reproduces vanilla; a wall frame is the
same rule turned. Vanilla's wire logic lives in the pure `domain.Wire` over a `View` of the
neighbourhood already turned into the frame, so "the same as the floor" is a tested fact.
The blocks are new (vanilla's have no face in their state), placed by the vanilla items through
an interaction event, and the mod's two touches on vanilla are an inject letting floor dust read
wall dust's power where a floor run steps up onto a wall's top edge, and an accessor onto the flag
vanilla silences its wires with while any wire computes, shared so no wire of either kind reads
itself back through a powered block. Why each of these: `knowledge/decisions/D-0001.md`.

## License

AGPL-3.0-or-later. The textures and models are vanilla's, referenced and turned, none copied.
