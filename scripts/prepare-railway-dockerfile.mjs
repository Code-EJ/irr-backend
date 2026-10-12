/** Binds Railway's literal cache namespace without replacing the owner's Dockerfile. @author Enzo Ribas (https://github.com/oEnzoRibas). */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const [serviceId,stagedDockerfile]=process.argv.slice(2);
if(!/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(serviceId??''))throw new Error('Supply the exact target Railway service UUID.');
const root=fileURLToPath(new URL('../',import.meta.url));
const allowed=path.resolve(root,'.local');
const destination=path.resolve(stagedDockerfile??'');
const relative=path.relative(allowed,destination);
if(!relative||relative.startsWith('..')||path.isAbsolute(relative)||path.basename(destination)!=='Dockerfile')throw new Error('Only an ignored .local staged Dockerfile may be prepared.');
if(fs.lstatSync(destination).isSymbolicLink())throw new Error('Refuse a symbolic-link Dockerfile.');
const source=fs.readFileSync(destination,'utf8');
const pattern=/id=s\/[a-f0-9-]{36}-\/root\/\.m2,target=\/root\/\.m2/g;
if([...source.matchAll(pattern)].length!==1)throw new Error('Expected exactly one reviewed Maven cache mount.');
fs.writeFileSync(destination,source.replace(pattern,`id=s/${serviceId}-/root/.m2,target=/root/.m2`));
console.log('Prepared the target service cache namespace; Maven goals, memory, stages and runtime unchanged.');
