/** Loads an exact scenario only into an owned disposable database. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import fs from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';
import { fileURLToPath } from 'node:url';
import crypto from 'node:crypto';
import { readRun,guard,sql,docker } from './common.mjs';
import { generate } from './generate.mjs';
import { expected } from './oracle.mjs';

export function queries(data) {
    return [
        {name:'hot-broad',org:data.refs[0].org,from:'2026-06-01',to:'2026-09-30'},
        {name:'hot-month',org:data.refs[0].org,from:'2026-09-01',to:'2026-09-30'},
        {name:'hot-route',org:data.refs[0].org,route:data.refs[0].routes[2].route,from:'2026-06-01',to:'2026-09-30'},
        {name:'cold-broad',org:data.refs[1].org,from:'2026-06-01',to:'2026-09-30'},
    ];
}
/** Wipes only the generated benchmark's disposable business schema; Flyway remains intact. */
export function seedRun(directory,state,{seed=669850,executions=1000}={}) {
    guard(state);
    const started = performance.now(), fixture = generate({seed,executions});
    const tables = JSON.parse(sql(state,"SELECT coalesce(json_agg(tablename ORDER BY tablename),'[]') FROM pg_tables WHERE schemaname='public' AND tablename<>'flyway_schema_history'"));
    if (tables.some(name => !/^[a-z_][a-z0-9_]*$/.test(name))) throw new Error('Unexpected benchmark table.');
    sql(state,'TRUNCATE ' + tables.map(table => '"' + table + '"').join(',') + ' CASCADE;');
    const artifact = path.join(directory,'dataset-' + executions + '.sql');
    fs.writeFileSync(artifact,fixture.sql);
    sql(state,fixture.sql);
    const pdf = fileURLToPath(new URL('./fixtures/verification.pdf',import.meta.url));
    for (const reference of fixture.data.refs) docker(['cp',pdf,state.backend+':/data/uploads/'+reference.attachment]);
    // Real constraints remain enabled. These independent conservation checks catch malformed seeds.
    const consistency = JSON.parse(sql(state,`WITH
      inputs AS (SELECT collection_id,sum(weight_kg) kg FROM input_item WHERE collection_id IS NOT NULL GROUP BY collection_id),
      lines AS (SELECT sale_id,sum(round(weight_kg*unit_price,2)) value FROM sale_item GROUP BY sale_id),
      lots AS (SELECT organization_id,material_subtype_id,sum(available_weight_kg) kg FROM stock_lot GROUP BY organization_id,material_subtype_id),
      ledger AS (SELECT organization_id,material_subtype_id,sum(weight_delta_kg) kg FROM stock_movement GROUP BY organization_id,material_subtype_id)
      SELECT json_build_object(
      'inputMismatch',(SELECT count(*) FROM collection c LEFT JOIN inputs i ON i.collection_id=c.id WHERE c.total_weight_kg IS DISTINCT FROM i.kg),
      'saleMismatch',(SELECT count(*) FROM sale s LEFT JOIN lines i ON i.sale_id=s.id WHERE s.total_value IS DISTINCT FROM i.value),
      'stockMismatch',(SELECT count(*) FROM inventory_balance b LEFT JOIN lots l USING(organization_id,material_subtype_id) LEFT JOIN ledger m USING(organization_id,material_subtype_id) WHERE b.current_weight_kg IS DISTINCT FROM l.kg OR b.current_weight_kg IS DISTINCT FROM m.kg)
    )`));
    if (Object.values(consistency).some(count => count !== 0)) throw new Error('Synthetic domain conservation failed.');
    const scenarios = queries(fixture.data).map(query => ({query,operations:expected(fixture.data,query,'operations'),analytics:expected(fixture.data,query,'analytics')}));
    const snapshot = {scenarioVersion:'synthetic-reports-1',seed,executions,sqlSha256:fixture.digest,attachmentSha256:crypto.createHash('sha256').update(fs.readFileSync(pdf)).digest('hex'),counts:fixture.counts,consistency,preparationMs:performance.now()-started,actor:fixture.data.actor,scenarios};
    fs.writeFileSync(path.join(directory,'scenario-' + executions + '.json'),JSON.stringify(snapshot,null,2));
    return snapshot;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    const {values,positionals} = parseArgs({allowPositionals:true,options:{seed:{type:'string',default:'669850'},executions:{type:'string',default:'1000'}}});
    const {directory,state} = readRun(positionals[0]);
    const result = seedRun(directory,state,{seed:Number(values.seed),executions:Number(values.executions)});
    console.log(JSON.stringify({executions:result.executions,sqlSha256:result.sqlSha256,counts:result.counts,consistency:result.consistency},null,2));
}
