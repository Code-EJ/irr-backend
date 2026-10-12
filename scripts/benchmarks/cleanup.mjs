/** Removes only the named disposable benchmark resources. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import { readRun,cleanup } from './common.mjs';
const {directory,state} = readRun(process.argv[2]);
cleanup(state);
fs.writeFileSync(path.join(directory,'cleanup.json'),JSON.stringify({cleanedAt:new Date().toISOString(),project:state.project,evidenceRetained:true},null,2));
console.log('Removed labeled benchmark resources; retained local evidence.');
