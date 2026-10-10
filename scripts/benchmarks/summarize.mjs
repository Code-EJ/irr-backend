/** Exports only public measurement metadata, never runtime credentials or database dumps. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import {readRun} from './common.mjs';

const {directory}=readRun(process.argv[2]);
const files=process.argv.slice(3);
if(!files.length||files.some(f=>!/^[-a-z0-9]+\.json$/.test(f))) throw new Error('Supply result filenames inside the run directory.');
const summaries=files.map(filename=>{
    const r=JSON.parse(fs.readFileSync(path.join(directory,filename),'utf8'));
    if(r.scenarioVersion!=='synthetic-reports-1'||!Array.isArray(r.runs)) throw new Error('Not a benchmark result.');
    return {filename,status:r.status,error:r.error,startedAt:r.startedAt,completedAt:r.completedAt,sourceCommit:r.sourceCommit,
        backendImageId:r.backendImageId,databaseImageId:r.databaseImageId,redisImageId:r.redisImageId,openapiSha256:r.openapiSha256,
        environment:r.environment,configuration:r.configuration,migrationHistory:r.migrationHistory,
        runs:r.runs.map(run=>({executions:run.executions,sqlSha256:run.sqlSha256,counts:run.counts,seedMs:run.seedMs,measurements:run.measurements,
            sql:run.plans.map(p=>({scenario:p.scenario,endpoint:p.endpoint,observations:p.observations.map(o=>({executionMs:o['Execution Time'],planningMs:o['Planning Time'],
                sharedHits:o.Plan['Shared Hit Blocks'],sharedReads:o.Plan['Shared Read Blocks'],tempReads:o.Plan['Temp Read Blocks'],tempWrites:o.Plan['Temp Written Blocks']}))}))}))};
});
const preparedPlans=[];
for(const filename of fs.readdirSync(directory).filter(f=>/^plan-modes-\d+\.json$/.test(f))) {
    for(const item of JSON.parse(fs.readFileSync(path.join(directory,filename),'utf8'))) {
        const joins=[];
        const walk=node=>{
            if(node['Node Type'].includes('Loop')) joins.push({type:node['Node Type'],estimatedRows:node['Plan Rows'],actualRows:node['Actual Rows'],loops:node['Actual Loops'],totalMs:node['Actual Total Time'],rowsRemoved:node['Rows Removed by Join Filter']});
            for(const child of node.Plans??[]) walk(child);
        };
        walk(item.observation.Plan);
        preparedPlans.push({filename,scenario:item.scenario,mode:item.mode,executed:item.executed,error:item.error,executionMs:item.observation['Execution Time'],joins});
    }
}
const result={description:'Local synthetic baseline, not production performance evidence.',summaries,preparedPlans};
fs.writeFileSync(path.join(directory,'public-summary.json'),JSON.stringify(result,null,2));
console.log('Wrote public-summary.json; review before copying to documentation.');
