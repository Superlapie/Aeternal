const fs=require('fs');
const sp=JSON.parse(fs.readFileSync('server/data/definitions/npc_spawns.json','utf8'));
const src=JSON.parse(fs.readFileSync('osrs_npcs.json','utf8'));
const defs=JSON.parse(fs.readFileSync('server/data/definitions/npc_defs.json','utf8'));
const nm=new Map(defs.map(d=>[d.id,d.name||'']));
function top(list, label){
 const c={}; for(const n of list){const k=n.name||n._name||('id'+n.id); c[k]=(c[k]||0)+1;}
 const out=Object.entries(c).sort((a,b)=>b[1]-a[1]).slice(0,40);
 console.log(label,'total',list.length); for(const [k,v] of out) console.log(v+' '+k);
}
const lumSp=sp.filter(n=>n.position.x>=3190&&n.position.x<=3265&&n.position.y>=3190&&n.position.y<=3265&&n.position.z===0).map(n=>({name:nm.get(n.id)||('id'+n.id),id:n.id}));
const lumSrc=src.filter(n=>n.x>=3190&&n.x<=3265&&n.y>=3190&&n.y<=3265&&((n.z??n.p??0)===0)).map(n=>({name:n.name||('id'+n.id),id:n.id}));
const ardSp=sp.filter(n=>n.position.x>=2550&&n.position.x<=2695&&n.position.y>=3260&&n.position.y<=3345&&n.position.z===0).map(n=>({name:nm.get(n.id)||('id'+n.id),id:n.id}));
const ardSrc=src.filter(n=>n.x>=2550&&n.x<=2695&&n.y>=3260&&n.y<=3345&&((n.z??n.p??0)===0)).map(n=>({name:n.name||('id'+n.id),id:n.id}));
top(lumSrc,'src lum');
top(lumSp,'spawn lum');
top(ardSrc,'src ardougne');
top(ardSp,'spawn ardougne');
