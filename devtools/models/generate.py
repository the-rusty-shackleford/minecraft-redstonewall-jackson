# Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later
"""Generate the wall blocks' blockstates and models from vanilla's own.

Why generated: a wall plane is vanilla's floor plane turned, but a blockstate's x-then-y
rotation reaches only sixteen of the twenty-four orientations of a block, so every diode that
faces along a wall (rather than up or down it) needs a model file whose elements are already
turned about the vertical axis. Dust is simpler: its parts are flat quads, drawn here directly
for a north-facing wall and turned to the other walls with a y rotation.

Conventions verified against vanilla's own blockstates (ladder: y=90 turns north to east;
lever: x=90 turns up to north), as rotations of a point about the block's centre:
  y=90: (x, y, z) -> (-z, y, x)       x=90: (x, y, z) -> (x, z, -y)
and a model rotates by x first, then y.

Usage: uv run --no-project python devtools/models/generate.py <minecraft-resources.jar>
Writes into src/main/resources/assets/redstonewalljackson/.
"""
import json
import sys
import zipfile
from pathlib import Path

MOD = 'redstonewalljackson'
ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets' / MOD
CUTOUT = 'minecraft:cutout'

# --- directions and rotations -------------------------------------------------

DIRS = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}
NAME = {v: k for k, v in DIRS.items()}
HORIZONTAL = ['north', 'east', 'south', 'west']


def rot_y(v, times):
    x, y, z = v
    for _ in range(times % 4):
        x, y, z = -z, y, x
    return (x, y, z)


def rot_x(v, times):
    x, y, z = v
    for _ in range(times % 4):
        x, y, z = x, z, -y
    return (x, y, z)


def apply(v, pre_yaw, x, y):
    """The blockstate's rotation of a pre-yawed model: pre-yaw about y, then x, then y."""
    return rot_y(rot_x(rot_y(v, pre_yaw // 90), x // 90), y // 90)


def solve(model_up, model_front, target_up, target_front):
    """Returns (pre_yaw, x, y), preferring no pre-yaw, that takes the model's up and front to the targets."""
    for pre in (0, 90, 180, 270):
        for x in (0, 90, 180, 270):
            for y in (0, 90, 180, 270):
                if apply(model_up, pre, x, y) == target_up and apply(model_front, pre, x, y) == target_front:
                    return pre, x, y
    raise AssertionError(f'no rotation takes {model_front} onto {target_front} with up {target_up}')


# --- the wall's planar directions, as the domain's Frame defines them -------------

def planar(normal):
    """TOP, BOTTOM, LEFT, RIGHT as world direction names for the wall whose face points normal."""
    support = NAME[tuple(-c for c in DIRS[normal])]
    order = ['north', 'east', 'south', 'west']
    cw = order[(order.index(support) + 1) % 4]
    ccw = order[(order.index(support) - 1) % 4]
    return {'top': 'up', 'bottom': 'down', 'left': ccw, 'right': cw}


# --- dust ------------------------------------------------------------------------

def dust_models():
    """Flat quads for a north-facing wall: the plane at z=15.75, the dust facing north."""
    textures = lambda line: {'particle': 'minecraft:block/redstone_dust_dot', 'line': f'minecraft:block/{line}', 'overlay': 'minecraft:block/redstone_dust_overlay'}

    def plane(name, line, x1, y1, x2, y2, north_uv, north_rot=0):
        south_uv = [north_uv[2], north_uv[1], north_uv[0], north_uv[3]]
        faces = lambda tex, tint: {
            'north': {'uv': north_uv, 'texture': tex, **({'rotation': north_rot} if north_rot else {}), **({'tintindex': 0} if tint else {})},
            'south': {'uv': south_uv, 'texture': tex, **({'rotation': north_rot} if north_rot else {}), **({'tintindex': 0} if tint else {})},
        }
        return name, {
            'ambientocclusion': False, 'render_type': CUTOUT, 'textures': textures(line),
            'elements': [
                {'from': [x1, y1, 15.75], 'to': [x2, y2, 15.75], 'shade': False, 'faces': faces('#line', True)},
                {'from': [x1, y1, 15.75], 'to': [x2, y2, 15.75], 'shade': False, 'faces': faces('#overlay', False)},
            ],
        }

    models = dict([
        plane('wall_dust_dot', 'redstone_dust_dot', 0, 0, 16, 16, [0, 0, 16, 16]),
        plane('wall_dust_top', 'redstone_dust_line0', 0, 8, 16, 16, [0, 0, 16, 8]),
        plane('wall_dust_bottom', 'redstone_dust_line0', 0, 0, 16, 8, [0, 8, 16, 16]),
        # The horizontal legs: the half line turned to run along the wall, its outer end at the edge.
        plane('wall_dust_left', 'redstone_dust_line1', 8, 0, 16, 16, [0, 0, 16, 8], 270),
        plane('wall_dust_right', 'redstone_dust_line1', 0, 0, 8, 16, [0, 0, 16, 8], 90),
    ])

    def climb(name, frm, to, visible, hidden, uv, rot=0):
        faces = lambda tex, tint: {
            visible: {'uv': uv, 'texture': tex, **({'rotation': rot} if rot else {}), **({'tintindex': 0} if tint else {})},
            hidden: {'uv': uv, 'texture': tex, **({'rotation': rot} if rot else {}), **({'tintindex': 0} if tint else {})},
        }
        return name, {
            'ambientocclusion': False, 'render_type': CUTOUT, 'textures': textures('redstone_dust_line0'),
            'elements': [
                {'from': frm, 'to': to, 'shade': False, 'faces': faces('#line', True)},
                {'from': frm, 'to': to, 'shade': False, 'faces': faces('#overlay', False)},
            ],
        }

    # The climbs run up the face of the neighbouring block, drawn just inside this block at its edge,
    # the line's start at the wall (z=16) and its end at the front (z=0).
    models.update(dict([
        climb('wall_dust_up_top', [0, 15.75, 0], [16, 15.75, 16], 'down', 'up', [0, 16, 16, 0]),
        climb('wall_dust_up_bottom', [0, 0.25, 0], [16, 0.25, 16], 'up', 'down', [0, 0, 16, 16]),
        climb('wall_dust_up_left', [15.75, 0, 0], [15.75, 16, 16], 'west', 'east', [0, 0, 16, 16], 270),
        climb('wall_dust_up_right', [0.25, 0, 0], [0.25, 16, 16], 'east', 'west', [0, 0, 16, 16], 90),
    ]))
    return models


def dust_blockstate():
    y_of = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    parts = []
    for facing, y in y_of.items():
        model = lambda part: {'model': f'{MOD}:block/{part}', **({'y': y} if y else {})}
        joined = 'side|up'
        parts.append({'when': {'OR': [
            {'facing': facing, 'top': 'none', 'bottom': 'none', 'left': 'none', 'right': 'none'},
            {'facing': facing, 'top': joined, 'left': joined},
            {'facing': facing, 'top': joined, 'right': joined},
            {'facing': facing, 'bottom': joined, 'left': joined},
            {'facing': facing, 'bottom': joined, 'right': joined},
        ]}, 'apply': model('wall_dust_dot')})
        for side in ('top', 'bottom', 'left', 'right'):
            parts.append({'when': {'facing': facing, side: joined}, 'apply': model(f'wall_dust_{side}')})
            parts.append({'when': {'facing': facing, side: 'up'}, 'apply': model(f'wall_dust_up_{side}')})
    return {'multipart': parts}


# --- diodes ------------------------------------------------------------------------

def yawed(model, times):
    """The model turned about the vertical axis by times quarter turns, Minecraft's y sense."""
    out = json.loads(json.dumps(model))
    for element in out['elements']:
        corners = [tuple(c - 8 for c in element['from']), tuple(c - 8 for c in element['to'])]
        turned = [rot_y(c, times) for c in corners]
        element['from'] = [min(a, b) + 8 for a, b in zip(*turned)]
        element['to'] = [max(a, b) + 8 for a, b in zip(*turned)]
        faces = {}
        for name, face in element['faces'].items():
            new = NAME[rot_y(DIRS[name], times)]
            face = dict(face)
            if 'cullface' in face:
                face['cullface'] = NAME[rot_y(DIRS[face['cullface']], times)]
            if name in ('up', 'down'):
                # The texture turns with the block: clockwise seen from above, so the other way seen from below.
                turn = (times * 90) if name == 'up' else (-times * 90)
                face['rotation'] = (face.get('rotation', 0) + turn) % 360
                if face['rotation'] == 0:
                    face.pop('rotation', None)
            faces[new] = face
        element['faces'] = faces
    return out


def diode_assets(jar):
    """Blockstates and models for the wall repeater and comparator, from vanilla's."""
    vanilla_states = {name: json.loads(jar.read(f'assets/minecraft/blockstates/{name}.json')) for name in ('repeater', 'comparator')}
    # The direction vanilla's unrotated model faces (its input side), read off the blockstate.
    native = {}
    for name, state in vanilla_states.items():
        for key, variant in state['variants'].items():
            props = dict(kv.split('=') for kv in key.split(','))
            if variant.get('y', 0) == 0 and variant.get('x', 0) == 0:
                native[name] = props['facing']
                break
    models = {}
    blockstates = {}
    for name in ('repeater', 'comparator'):
        model_front = DIRS[native[name]]
        variants = {}
        for wall in HORIZONTAL:
            plane_dirs = planar(wall)
            for facing in ['up', 'down', 'north', 'south', 'west', 'east']:
                # A facing along the wall's normal is not a state the block ever takes; it shows the up model.
                target = facing if facing in plane_dirs.values() else 'up'
                pre, x, y = solve((0, 1, 0), model_front, DIRS[wall], DIRS[target])
                for key, variant in vanilla_states[name]['variants'].items():
                    props = dict(kv.split('=') for kv in key.split(','))
                    if props['facing'] != native[name]:
                        continue
                    base = variant['model'].split('/')[-1]
                    wall_model = f'wall_{base}' + (f'_yaw{pre}' if pre else '')
                    if wall_model not in models:
                        vanilla = json.loads(jar.read(f'assets/minecraft/models/block/{base}.json'))
                        turned = yawed(vanilla, pre // 90) if pre else json.loads(json.dumps(vanilla))
                        turned['render_type'] = CUTOUT
                        turned['textures'] = {k: (v if ':' in v else f'minecraft:{v}') for k, v in turned['textures'].items()}
                        models[wall_model] = turned
                    props = {k: v for k, v in props.items() if k != 'facing'}
                    variant_key = ','.join([f'facing={facing}'] + [f'{k}={v}' for k, v in sorted(props.items())] + [f'wall={wall}'])
                    entry = {'model': f'{MOD}:block/{wall_model}'}
                    if x:
                        entry['x'] = x
                    if y:
                        entry['y'] = y
                    variants[variant_key] = entry
        blockstates[f'wall_{name}'] = {'variants': variants}
    return blockstates, models


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def main():
    jar = zipfile.ZipFile(sys.argv[1])
    for name, model in dust_models().items():
        write(ASSETS / 'models/block' / f'{name}.json', model)
    write(ASSETS / 'blockstates/wall_redstone_wire.json', dust_blockstate())
    blockstates, models = diode_assets(jar)
    for name, state in blockstates.items():
        write(ASSETS / 'blockstates' / f'{name}.json', state)
    for name, model in models.items():
        write(ASSETS / 'models/block' / f'{name}.json', model)
    print(f'wrote {len(blockstates) + 1} blockstates and {len(models) + 9} models')


if __name__ == '__main__':
    main()
