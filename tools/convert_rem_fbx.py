#!/usr/bin/env python3
"""Convert Rem FBX + supplied PNGs into one self-contained offline GLB using Blender."""
import argparse, json, sys
from pathlib import Path
import bpy

argv = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
p = argparse.ArgumentParser()
p.add_argument('--input', required=True)
p.add_argument('--textures', required=True)
p.add_argument('--output', required=True)
a = p.parse_args(argv)
source, texture_dir, output = Path(a.input).resolve(), Path(a.textures).resolve(), Path(a.output).resolve()
if not source.is_file(): raise SystemExit(f'FBX not found: {source}')
if not texture_dir.is_dir(): raise SystemExit(f'Texture directory not found: {texture_dir}')

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.fbx(filepath=str(source), use_anim=True)
meshes = sum(o.type == 'MESH' for o in bpy.context.scene.objects)
armatures = sum(o.type == 'ARMATURE' for o in bpy.context.scene.objects)
actions = len(bpy.data.actions)
if meshes == 0: raise SystemExit('FBX import produced no mesh objects.')

# Relink FBX references such as images/hair2.png to the extracted image folder.
for image in bpy.data.images:
    name = Path(image.filepath).name or image.name
    candidate = texture_dir / name
    if candidate.is_file():
        image.filepath = str(candidate.resolve())
        try: image.reload()
        except Exception: pass

# Material-name fallback for any image nodes that the FBX importer did not connect.
fallback = {'backhair':'hair2.png', 'body':'cloth1.png', 'face':'face.png',
            'whiteeye':'eye.png', 'eye':'eye.png', 'hair':'hair1.png'}
for material in bpy.data.materials:
    material.use_nodes = True
    nodes = material.node_tree.nodes
    if any(n.type == 'TEX_IMAGE' and n.image for n in nodes): continue
    name = material.name.lower().replace('_0', '')
    filename = next((file for key, file in fallback.items() if key in name), None)
    if not filename: continue
    image_path = texture_dir / filename
    if not image_path.is_file(): continue
    image = bpy.data.images.load(str(image_path.resolve()), check_existing=True)
    tex = nodes.new('ShaderNodeTexImage'); tex.image = image
    shader = next((n for n in nodes if n.type == 'BSDF_PRINCIPLED'), None)
    if shader:
        material.node_tree.links.new(tex.outputs['Color'], shader.inputs['Base Color'])
        if 'Alpha' in shader.inputs and ('hair' in name or 'backhair' in name):
            material.node_tree.links.new(tex.outputs['Alpha'], shader.inputs['Alpha'])

# Pack all referenced images into the GLB; runtime does not need network or loose textures.
try: bpy.ops.file.pack_all()
except Exception as exc: print(f'Texture packing warning: {exc}')
output.parent.mkdir(parents=True, exist_ok=True)
bpy.ops.export_scene.gltf(filepath=str(output), export_format='GLB',
    export_animations=True, export_skins=True, export_morph=True,
    export_materials='EXPORT', export_image_format='AUTO', export_texcoords=True,
    export_normals=True, export_tangents=True, export_colors=True,
    export_cameras=False, export_lights=False)
if not output.is_file() or output.stat().st_size < 20: raise SystemExit('GLB export failed.')
report = {'source': source.name, 'glb_bytes': output.stat().st_size,
          'mesh_objects': meshes, 'armature_objects': armatures,
          'animation_actions': actions, 'skeletal_rig_detected': armatures > 0}
output.with_name('rem_model_report.json').write_text(json.dumps(report, indent=2))
print(json.dumps(report, indent=2))
if armatures == 0:
    print('WARNING: no armature detected. This model can render, but true skeletal animation needs a rigged source.')
