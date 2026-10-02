/** Maven dependency advisory audit. Maintainer: Enzo Ribas (https://github.com/oEnzoRibas). */
import fs from 'node:fs';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../', import.meta.url));
const tree=JSON.parse(fs.readFileSync(root+'.local/audit/dependency-tree.json','utf8'));
const all=new Map();
function walk(n){if(n.groupId&&n.artifactId&&n.version)all.set(n.groupId+':'+n.artifactId+':'+n.version,{name:n.groupId+':'+n.artifactId,version:n.version,scope:n.scope??'compile'});for(const c of n.children??[])walk(c);}
for(const node of tree.children??[])walk(node);
const dependencies=[...all.values()].sort((a,b)=>a.name.localeCompare(b.name));
const results=[];
for(let start=0;start<dependencies.length;start+=100){
 const batch=dependencies.slice(start,start+100);
 const response=await fetch('https://api.osv.dev/v1/querybatch',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({queries:batch.map(d=>({package:{ecosystem:'Maven',name:d.name},version:d.version}))}),signal:AbortSignal.timeout(60000)});
 if(!response.ok)throw new Error('OSV query failed: '+response.status);
 const data=await response.json();data.results.forEach((r,i)=>{for(const vuln of r.vulns??[])results.push({...batch[i],id:vuln.id});});
}
const details=[];
for(const id of [...new Set(results.map(r=>r.id))]){const response=await fetch('https://api.osv.dev/v1/vulns/'+encodeURIComponent(id),{signal:AbortSignal.timeout(30000)});if(!response.ok)throw new Error('OSV detail failed: '+response.status);details.push(await response.json());}
fs.writeFileSync(root+'.local/audit/osv-audit.json',JSON.stringify({date:new Date().toISOString(),dependencies,results,details},null,2));
console.log(JSON.stringify({dependencies:dependencies.length,findings:results.map(r=>({package:r.name,version:r.version,scope:r.scope,id:r.id})),advisories:details.map(d=>({id:d.id,summary:d.summary,withdrawn:d.withdrawn,affected:d.affected?.map(a=>({package:a.package?.name,ranges:a.ranges}))}))},null,2));

if(results.length) process.exitCode=1;
