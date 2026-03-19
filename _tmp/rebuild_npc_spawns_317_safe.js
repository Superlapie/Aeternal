const fs = require('fs');
const path = require('path');

const ROOT = 'd:/CodingProjects/OSRSRSPS';
const npcInfoPath = path.join(ROOT, 'NPCINFO.json');
const osrsDirPath = path.join(ROOT, 'osrs_npcs.json');
const npcDefsPath = path.join(ROOT, 'server/data/definitions/npc_defs.json');
const mapIndexPath = path.join(ROOT, 'server/data/clipping/map_index');
const outPath = path.join(ROOT, '_tmp/npc_spawns.fixed.json');

const normalize = (s) => String(s || '').toLowerCase().replace(/[^a-z0-9 ]+/g, ' ').replace(/\s+/g, ' ').trim();
const toFacing = (direction) => ({0:'SOUTH',1:'SOUTH_WEST',2:'WEST',3:'NORTH_WEST',4:'NORTH',5:'NORTH_EAST',6:'EAST',7:'SOUTH_EAST'})[Math.floor((((Number(direction)||0)+128)%2048)/256)] || 'SOUTH';
const regionIdForTile = (x, y) => ((x >> 6) << 8) | (y >> 6);

const defs = JSON.parse(fs.readFileSync(npcDefsPath, 'utf8'));
const idToDef = new Map();
const nameToIds = new Map();
for (const d of defs) {
  if (!d || !Number.isInteger(d.id)) continue;
  idToDef.set(d.id, d);
  const k = normalize(d.name);
  if (!nameToIds.has(k)) nameToIds.set(k, []);
  nameToIds.get(k).push(d.id);
}

const mapIndex = fs.readFileSync(mapIndexPath);
const regionCount = mapIndex.readUInt16BE(0);
const validRegions = new Set();
for (let i=0,off=2; i<regionCount && off+5<mapIndex.length; i++,off+=6) validRegions.add(mapIndex.readUInt16BE(off));

const dirEntries = JSON.parse(fs.readFileSync(osrsDirPath, 'utf8'));
const dirByFull = new Map();
const dirByTile = new Map();
for (const e of dirEntries) {
  const id=Number(e?.id),x=Number(e?.x),y=Number(e?.y),z=Number(e?.z??e?.p??0),direction=Number(e?.direction??0),radius=Number(e?.radius??0);
  if (![id,x,y,z,direction,radius].every(Number.isFinite)) continue;
  const full=`${Math.trunc(id)}:${Math.trunc(x)}:${Math.trunc(y)}:${Math.trunc(z)}`;
  dirByFull.set(full,{direction:Math.trunc(direction),radius:Math.trunc(radius)});
  const tile=`${Math.trunc(x)}:${Math.trunc(y)}:${Math.trunc(z)}`;
  if(!dirByTile.has(tile)) dirByTile.set(tile,[]);
  dirByTile.get(tile).push({direction:Math.trunc(direction),radius:Math.trunc(radius)});
}

const infoEntries = JSON.parse(fs.readFileSync(npcInfoPath, 'utf8'));
const out=[]; const seen=new Set();
const stats={total:0,kept:0,skip_bad:0,skip_region:0,skip_coords:0,skip_plane:0,skip_name_unmapped:0,id_exact_name_match:0,id_from_name:0,dir_full:0,dir_tile:0,dir_default:0,dupes:0};

for (const e of infoEntries) {
  stats.total++;
  const srcId=Number(e?.id),x=Number(e?.x),y=Number(e?.y),z=Number(e?.z??e?.p??0),srcName=String(e?.name||'').trim();
  if(![srcId,x,y,z].every(Number.isFinite)||!srcName){stats.skip_bad++;continue;}
  const xi=Math.trunc(x),yi=Math.trunc(y),zi=Math.trunc(z),idi=Math.trunc(srcId);
  if(zi<0||zi>3){stats.skip_plane++;continue;}
  if(xi<0||yi<0||xi>=16384||yi>=16384){stats.skip_coords++;continue;}
  if(!validRegions.has(regionIdForTile(xi,yi))){stats.skip_region++;continue;}

  const nameNorm=normalize(srcName);
  let localId=null;
  const sameIdDef=idToDef.get(idi);
  if(sameIdDef && normalize(sameIdDef.name)===nameNorm){localId=idi;stats.id_exact_name_match++;}
  else {
    const candidates=(nameToIds.get(nameNorm)||[]).slice();
    if(candidates.length){candidates.sort((a,b)=>Math.abs(a-idi)-Math.abs(b-idi));localId=candidates[0];stats.id_from_name++;}
  }
  if(localId==null){stats.skip_name_unmapped++;continue;}

  let direction=0,radius=0;
  const fullKey=`${idi}:${xi}:${yi}:${zi}`;
  const tileKey=`${xi}:${yi}:${zi}`;
  if(dirByFull.has(fullKey)){const d=dirByFull.get(fullKey);direction=d.direction;radius=d.radius;stats.dir_full++;}
  else if(dirByTile.has(tileKey)){
    const mode=new Map(); for(const d of dirByTile.get(tileKey)){const k=`${d.direction}:${d.radius}`;mode.set(k,(mode.get(k)||0)+1);} 
    const best=[...mode.entries()].sort((a,b)=>b[1]-a[1])[0][0].split(':').map(Number);
    direction=best[0]; radius=best[1]; stats.dir_tile++;
  } else stats.dir_default++;

  const rec={facing:toFacing(direction),radius:Math.max(0,Math.min(Math.trunc(radius),5)),id:localId,position:{x:xi,y:yi,z:zi},description:srcName};
  const key=`${rec.id}:${xi}:${yi}:${zi}:${rec.facing}:${rec.radius}`;
  if(seen.has(key)){stats.dupes++;continue;}
  seen.add(key); out.push(rec); stats.kept++;
}

fs.writeFileSync(outPath, JSON.stringify(out,null,4), 'utf8');
console.log(JSON.stringify(stats,null,2));

const nm=new Map(defs.map(d=>[d.id,d.name||'']));
const box=(arr,a,b,c,d)=>arr.filter(n=>n.position.x>=a&&n.position.x<=b&&n.position.y>=c&&n.position.y<=d&&n.position.z===0);
const lum=box(out,3190,3265,3190,3265);
const ard=box(out,2550,2695,3260,3345);
function countBy(list){const m={}; for(const n of list){const k=nm.get(n.id)||('id'+n.id); m[k]=(m[k]||0)+1;} return Object.entries(m).sort((x,y)=>y[1]-x[1]).slice(0,20);}
console.log('LUM total',lum.length); for(const [k,v] of countBy(lum)) console.log(v+' '+k);
console.log('ARD total',ard.length); for(const [k,v] of countBy(ard)) console.log(v+' '+k);
