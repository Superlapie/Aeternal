const fs = require('fs');
const path = require('path');

const ROOT = 'd:/CodingProjects/OSRSRSPS';
const osrsNpcsPath = path.join(ROOT, 'osrs_npcs.json');
const npcDefsPath = path.join(ROOT, 'server/data/definitions/npc_defs.json');
const mapIndexPath = path.join(ROOT, 'server/data/clipping/map_index');
const outPath = path.join(ROOT, 'server/data/definitions/npc_spawns.json');

const toFacing = (direction) => {
  const sector = Math.floor((((Number(direction) || 0) + 128) % 2048) / 256);
  return {
    0: 'SOUTH', 1: 'SOUTH_WEST', 2: 'WEST', 3: 'NORTH_WEST',
    4: 'NORTH', 5: 'NORTH_EAST', 6: 'EAST', 7: 'SOUTH_EAST',
  }[sector] || 'SOUTH';
};

const regionIdForTile = (x, y) => ((x >> 6) << 8) | (y >> 6);

const defs = JSON.parse(fs.readFileSync(npcDefsPath, 'utf8'));
const validIds = new Set(defs.filter(d => d && Number.isInteger(d.id)).map(d => d.id));

const mapIndex = fs.readFileSync(mapIndexPath);
const count = mapIndex.readUInt16BE(0);
const validRegions = new Set();
for (let i = 0, off = 2; i < count && off + 5 < mapIndex.length; i++, off += 6) {
  const region = mapIndex.readUInt16BE(off);
  const landscape = mapIndex.readUInt16BE(off + 2);
  const objects = mapIndex.readUInt16BE(off + 4);
  if (landscape > 0 || objects > 0) validRegions.add(region);
}

const entries = JSON.parse(fs.readFileSync(osrsNpcsPath, 'utf8'));
const stats = { total: 0, kept: 0, skip_bad: 0, skip_id: 0, skip_region: 0, skip_coords: 0, skip_plane: 0, dupes: 0 };
const seen = new Set();
const out = [];

for (const e of entries) {
  stats.total++;
  const npcId = Number(e?.id), x = Number(e?.x), y = Number(e?.y), z = Number(e?.z ?? e?.p ?? 0);
  const radiusRaw = Number(e?.radius ?? 0), direction = Number(e?.direction ?? 0);
  if (![npcId, x, y, z, radiusRaw, direction].every(Number.isFinite)) { stats.skip_bad++; continue; }
  if (npcId < 0 || npcId > 8195 || !validIds.has(Math.trunc(npcId))) { stats.skip_id++; continue; }
  if (z < 0 || z > 3) { stats.skip_plane++; continue; }
  if (x < 0 || y < 0 || x >= 16384 || y >= 16384) { stats.skip_coords++; continue; }
  if (!validRegions.has(regionIdForTile(Math.trunc(x), Math.trunc(y)))) { stats.skip_region++; continue; }

  const rec = {
    facing: toFacing(direction),
    radius: Math.max(0, Math.min(Math.trunc(radiusRaw), 5)),
    id: Math.trunc(npcId),
    position: { x: Math.trunc(x), y: Math.trunc(y), z: Math.trunc(z) },
    description: typeof e?.name === 'string' && e.name.length ? e.name : 'OSRS import',
  };

  const key = `${rec.id}:${rec.position.x}:${rec.position.y}:${rec.position.z}:${rec.facing}:${rec.radius}`;
  if (seen.has(key)) { stats.dupes++; continue; }
  seen.add(key);
  out.push(rec);
  stats.kept++;
}

fs.writeFileSync(outPath, JSON.stringify(out, null, 4), 'utf8');
console.log(`map_index bytes: ${mapIndex.length}`);
console.log(`map regions in index: ${count}`);
console.log(`valid regions: ${validRegions.size}`);
console.log('Rebuilt npc_spawns.json');
for (const [k,v] of Object.entries(stats)) console.log(`${k}: ${v}`);
