/** Compares PostgreSQL custom/generic prepared plans without changing application configuration. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import {readRun,guard,sql} from './common.mjs';
import {productionSql} from './run.mjs';

const {directory,state}=readRun(process.argv[2]);
const volume=Number(process.argv[3]);
if(!Number.isSafeInteger(volume)||volume<10||volume>1000000) throw new Error('Supply the loaded execution volume.');
guard(state);
const fixture=JSON.parse(fs.readFileSync(path.join(directory,'scenario-'+volume+'.json'),'utf8'));
if(Number(sql(state,'SELECT count(*) FROM route_execution'))!==volume) throw new Error('Load the matching scenario before explaining it.');
const parameters={org:'$1',municipality:'$2',route:'$3',vehicle:'$4',start:'$5',end:'$6',salesAvailable:'$7'};
const result=[];
for(const scenario of fixture.scenarios) {
    const q=scenario.query;
    // All values originate from the deterministic generator, never user-provided SQL.
    const values=[q.org,null,q.route??null,null,q.from+'T03:00:00Z',new Date(Date.parse(q.to+'T03:00:00Z')+86400000).toISOString(),!q.route];
    const literals=values.map(v=>v===null?'NULL':typeof v==='boolean'?String(v):"'"+v.replaceAll("'","''")+"'").join(',');
    for(const mode of ['force_custom_plan','force_generic_plan']) {
        const query=`SET statement_timeout='120s'; SET plan_cache_mode=${mode}; PREPARE benchmark(uuid,uuid,uuid,uuid,timestamptz,timestamptz,boolean) AS ${productionSql('analytics',q,parameters)}; EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) EXECUTE benchmark(${literals});`;
        const raw=sql(state,query);
        const observation=JSON.parse(raw.slice(raw.indexOf('[\n')))[0];
        result.push({scenario:q.name,mode,observation});
        fs.writeFileSync(path.join(directory,'plan-modes-'+volume+'.json'),JSON.stringify(result,null,2));
        console.log(JSON.stringify({executions:volume,scenario:q.name,mode,executionMs:observation['Execution Time']}));
    }
}
