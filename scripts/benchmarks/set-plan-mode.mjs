/** Controlled prepared-plan experiment on the owned disposable database only. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import {readRun,guard,sql,docker} from './common.mjs';

const {directory,state}=readRun(process.argv[2]), mode=process.argv[3];
if(!['auto','force_custom_plan'].includes(mode)) throw new Error('Experiment mode must be auto or force_custom_plan.');
guard(state);
sql(state,'ALTER DATABASE irr_benchmark SET plan_cache_mode = '+mode);
docker(['restart',state.backend]);
guard(state);
fs.writeFileSync(path.join(directory,'run.json'),JSON.stringify(state,null,2));
const started=Date.now();
while(true) {
    try {
        const response=await fetch(state.api+'/actuator/health/readiness',{signal:AbortSignal.timeout(2000)});
        if(response.ok) break;
    } catch { /* Restart temporarily makes the isolated loopback endpoint unavailable. */ }
    if(Date.now()-started>180000) throw new Error('Isolated experimental backend did not become ready.');
    await new Promise(resolve=>setTimeout(resolve,1000));
}
fs.writeFileSync(path.join(directory,'experiment-settings.json'),JSON.stringify({mode,configuredAt:new Date().toISOString(),backendRestarted:true}));
console.log('Isolated prepared-plan mode: '+sql(state,'SHOW plan_cache_mode'));
