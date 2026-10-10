/** Deterministic, constraint-preserving synthetic report fixtures. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import crypto from 'node:crypto';
import { decimal } from './oracle.mjs';

const EPOCH = Date.parse('2026-06-01T03:00:00Z');
const CREATED = '2026-05-01T03:00:00Z';
/** Stable UUIDs and integer mixing depend only on seed and logical record identity. */
export function identity(seed, kind, index) {
    const hex = crypto.createHash('sha256').update(seed + ':' + kind + ':' + index).digest('hex');
    return hex.slice(0, 8) + '-' + hex.slice(8, 12) + '-4' + hex.slice(13, 16) + '-a' + hex.slice(17, 20) + '-' + hex.slice(20, 32);
}
const iso = time => new Date(time).toISOString();
const copyValue = value => value === null ? '\\N' : String(value).replaceAll('\\', '\\\\').replaceAll('\t', '\\t').replaceAll('\n', '\\n').replaceAll('\r', '\\r');

/** Produces COPY batches with triggers/constraints enabled and terminal transitions in order. */
export function generate({ seed = 669850, executions = 1000 } = {}) {
    if (!Number.isSafeInteger(seed) || seed < 0 || seed > 2147483647 || !Number.isSafeInteger(executions) || executions < 10 || executions > 1000000) throw new Error('Use integer seed 0..2147483647 and execution volume 10..1000000.');
    const id = (kind, index) => identity(seed, kind, index);
    const mix = index => {
        let value = (index ^ seed) >>> 0;
        value = Math.imul(value ^ value >>> 16, 0x45d9f3b);
        value = Math.imul(value ^ value >>> 16, 0x45d9f3b);
        return (value ^ value >>> 16) >>> 0;
    };
    const tables = new Map();
    const add = (table, columns, values) => {
        if (!tables.has(table)) tables.set(table, { columns, rows: [] });
        if (columns.split(',').length !== values.length || columns !== tables.get(table).columns) throw new Error('Fixture column mismatch: ' + table);
        tables.get(table).rows.push(values.map(copyValue).join('\t'));
    };
    const actor = id('actor', 0);
    add('users', 'id,email,password_hash,full_name,user_role,created_at,updated_at', [actor, 'benchmark@example.test', 'synthetic-not-a-login', 'Synthetic Reporter', 'REPRESENTATIVE', CREATED, CREATED]);
    const refs = [];
    for (let o = 0; o < 2; o++) {
        const org = id('organization', o), vehicle = id('vehicle', o), driver = id('driver', o), crew = [id('crew', o * 2), id('crew', o * 2 + 1)];
        const materials = [id('material', o * 2), id('material', o * 2 + 1)], lots = materials.map((_, j) => id('lot', o * 2 + j));
        add('organization', 'id,name,organization_type,created_by,created_at,updated_at', [org, 'Synthetic organization ' + o, 'Association', actor, CREATED, CREATED]);
        add('organization_membership', 'organization_id,user_id,role,granted_by,updated_at', [org, actor, 'MEMBER', actor, CREATED]);
        add('vehicle', 'id,organization_id,license_plate,creator_id,created_at,updated_at', [vehicle, org, 'BENCH' + o, actor, CREATED, CREATED]);
        for (const [member, name, role] of [[driver, 'Driver', 'DRIVER'], ...crew.map((c, j) => [c, 'Collector ' + j, 'COLLECTOR'])]) add('team_member', 'id,organization_id,name,role,creator_id,created_at,updated_at', [member, org, name, role, actor, CREATED, CREATED]);
        const category = id('category', o), type = id('type', o);
        add('material_category', 'id,organization_id,name,creator_id,created_at,updated_at', [category, org, 'Recyclable', actor, CREATED, CREATED]);
        add('material_type', 'id,organization_id,category_id,name,creator_id,created_at,updated_at', [type, org, category, 'Dry material', actor, CREATED, CREATED]);
        for (let j = 0; j < 2; j++) add('material_subtype', 'id,organization_id,type_id,name,creator_id,created_at,updated_at', [materials[j], org, type, 'Material ' + j, actor, CREATED, CREATED]);
        const cities = [];
        for (let c = 0; c < 2; c++) {
            const city = id('city', o * 2 + c), neighborhood = id('neighborhood', o * 2 + c);
            add('municipality', 'id,ibge_code,name,state_code,created_by,created_at', [city, '41' + String(10000 + o * 2 + c), 'Synthetic city ' + o + '-' + c, 'PR', actor, CREATED]);
            add('neighborhood', 'id,municipality_id,name,created_by,created_at', [neighborhood, city, 'Synthetic neighborhood', actor, CREATED]);
            cities.push({ city, neighborhood, code: '41' + String(10000 + o * 2 + c) });
        }
        const routes = [];
        for (let r = 0; r < 5; r++) {
            const route = id('route', o * 5 + r), city = cities[r % 2], areas = [];
            add('route', 'id,organization_id,municipality_id,name,status,created_by,updated_by,created_at,updated_at', [route, org, city.city, 'Synthetic route ' + r, 'ACTIVE', actor, actor, CREATED, CREATED]);
            for (let a = 0; a < 3; a++) {
                const area = id('area', o * 15 + r * 3 + a); areas.push(area);
                add('route_area', 'id,organization_id,route_id,municipality_id,neighborhood_id,sequence,street', [area, org, route, city.city, city.neighborhood, a + 1, 'Synthetic street ' + a]);
            }
            routes.push({ route, ...city, areas });
        }
        const buyer = id('buyer', o), donor = id('donor', o), attachment = id('attachment', o);
        add('buyer', 'id,organization_id,name,creator_id,created_at,updated_at', [buyer, org, 'Synthetic buyer', actor, CREATED, CREATED]);
        add('donor', 'id,organization_id,name,document,donor_type,creator_id,created_at,updated_at', [donor, org, 'Synthetic donor', 'BENCHMARK-' + o, 'PJ', actor, CREATED, CREATED]);
        add('attachment', 'id,organization_id,file_name,file_type,storage_url,creator_id,created_at,updated_at', [attachment, org, 'synthetic.pdf', 'application/pdf', attachment, actor, CREATED, CREATED]);
        refs.push({ org, vehicle, driver, crew, materials, lots, routes, buyer, donor, attachment });
    }
    const data = { seed, volume: executions, actor, refs, executions: [], receipts: [], sales: [] };
    const event = (e, action, version, time) => add('route_event', 'id,organization_id,route_id,execution_id,actor_id,action,version,recorded_at', [id('event-' + action, e.index), e.org, e.route, e.id, actor, action, version, iso(time)]);
    for (let i = 0; i < executions; i++) {
        const o = mix(i + 23) % 10 < 8 ? 0 : 1, f = refs[o], r = f.routes[mix(i + 91) % 5], execution = id('execution', i);
        const status = i % 10 < 7 ? 'COMPLETED' : i % 10 === 7 ? 'PLANNED' : i % 10 === 8 ? 'IN_PROGRESS' : 'CANCELLED';
        const finish = EPOCH + (mix(i + 13) % 120) * 86400000 + (mix(i + 49) % 20) * 3600000;
        const seconds = BigInt(i % 37 === 0 ? 0 : 900 + mix(i + 31) % 6301);
        const km = i % 11 === 0 ? null : BigInt(i % 17 === 0 ? 0 : 1000 + mix(i + 51) % 89001);
        const plannedKm = i % 13 === 0 ? null : BigInt(i % 19 === 0 ? 0 : 1000 + mix(i + 61) % 70001);
        const minutes = i % 7 === 0 ? null : 30 + mix(i + 71) % 90;
        const start = finish - Number(seconds) * 1000;
        const e = { id: execution, index: i, org: f.org, route: r.route, status, finish, seconds, km, plannedKm, plannedSeconds: minutes === null ? null : BigInt(minutes * 60), sections: [] };
        data.executions.push(e);
        add('route_execution', 'id,organization_id,route_id,route_version,route_name,municipality_id,municipality_name,municipality_ibge_code,municipality_state_code,planned_distance_km,planned_duration_minutes,planned_start_at,vehicle_id,vehicle_license_plate,driver_id,driver_name,created_by,updated_by,created_at,updated_at',
            [execution, f.org, r.route, 0, 'Synthetic route ' + f.routes.indexOf(r), r.city, 'Synthetic city ' + o + '-' + (f.routes.indexOf(r) % 2), r.code, 'PR', decimal(plannedKm, 3), minutes, iso(start), f.vehicle, 'BENCH' + o, f.driver, 'Driver', actor, actor, iso(start), iso(start)]);
        add('benchmark_states', 'id,state,start_at,finish_at,km', [execution, status, iso(start), iso(finish), decimal(km, 3)]);
        for (const [j, member] of f.crew.entries()) add('route_execution_team', 'organization_id,execution_id,team_member_id,member_name,member_role', [f.org, execution, member, 'Collector ' + j, 'COLLECTOR']);
        if (i % 23 !== 0) for (let a = 0; a < 3 + (i % 29 === 0 ? 1 : 0); a++) {
            const source = r.areas[a % 3], area = id('execution-area', i * 4 + a), covered = (i + a % 3) % 3 !== 0;
            e.sections.push({ key: execution + ':' + source, covered });
            add('route_execution_area', 'id,organization_id,execution_id,route_id,source_area_id,neighborhood_id,neighborhood_name,sequence,street', [area, f.org, execution, r.route, source, r.neighborhood, 'Synthetic neighborhood', a + 1, 'Synthetic street ' + a % 3]);
            add('benchmark_coverage', 'id,covered', [area, covered]);
        }
        event(e, 'PLAN', 0, start);
        if (status !== 'PLANNED') event(e, 'START', 1, start);
        if (status === 'COMPLETED') event(e, 'COMPLETE', 2, finish);
        if (status === 'CANCELLED') event(e, 'CANCEL', 2, finish + 1000);
        const receipt = (k, linked) => {
            const receiptId = id(linked ? 'receipt' : 'ad-hoc', i * 3 + k), kg = BigInt(10000 + mix(i * 3 + k + 97) % 9900000), active = (i + k) % 8 !== 0;
            const time = finish + ((i % 3) - 1) * 86400000;
            data.receipts.push({ id: receiptId, execution: linked ? execution : null, org: f.org, route: linked ? r.route : null, kg, active, time });
            add('collection', 'id,organization_id,realization_date,total_weight_kg,vehicle_id,driver_id,creator_id,route_execution_id,departure_at,arrival_at,distance_km,is_active,created_at,updated_at', [receiptId, f.org, iso(time), decimal(kg, 4), f.vehicle, f.driver, actor, linked ? execution : null, linked ? iso(start) : null, linked ? iso(finish) : null, linked ? decimal(km, 3) : null, active, iso(time), iso(time)]);
            for (const member of f.crew) add('collection_team', 'collection_id,team_member_id', [receiptId, member]);
            const first = kg * 6n / 10n;
            for (let j = 0; j < 2; j++) add('input_item', 'id,organization_id,collection_id,donation_id,material_subtype_id,weight_kg,volume_m3,is_active,created_at,updated_at', [id('receipt-item', (i * 3 + k) * 2 + j + (linked ? 0 : executions * 6)), f.org, receiptId, null, f.materials[j], decimal(j ? kg - first : first, 4), '0.1', active, iso(time), iso(time)]);
        };
        if (status === 'COMPLETED' && i % 9 !== 0) for (let k = 0; k < 1 + mix(i + 111) % 3; k++) receipt(k, true);
        if (i % 10 === 0) receipt(0, false);
        if (i % 5 === 0) {
            const sale = id('sale', i), state = i % 20 === 0 ? 'REVERSED' : i % 15 === 0 ? 'DRAFT' : 'POSTED', active = i % 55 !== 0;
            const weight = BigInt(100000 + mix(i + 151) % 400000), price = 12500n + BigInt(mix(i + 171) % 8) * 2500n;
            const weights = [weight * 6n / 10n, weight - weight * 6n / 10n];
            const cents = weights.reduce((sum, w) => sum + (w * price + 500000n) / 1000000n, 0n);
            // Inactive records are editable drafts; terminal commercial history is retained.
            const saleStatus = active ? state : 'DRAFT';
            data.sales.push({ org: f.org, time: finish, status: saleStatus, active, cents, weights, id: sale, o });
            add('sale', 'id,organization_id,buyer_id,nfe_attachment_id,sale_date,total_value,status,is_active,creator_id,created_at,updated_at', [sale, f.org, f.buyer, f.attachment, iso(finish), decimal(cents, 2), saleStatus, active, actor, iso(finish), iso(finish + (saleStatus === 'REVERSED' ? 40 * 86400000 : 0))]);
            for (let j = 0; j < 2; j++) add('sale_item', 'id,organization_id,sale_id,material_subtype_id,stock_lot_id,weight_kg,volume_m3,unit_price,is_active,created_at,updated_at', [id('sale-item', i * 2 + j), f.org, sale, f.materials[j], f.lots[j], decimal(weights[j], 4), '0', decimal(price, 4), active, iso(finish), iso(finish)]);
            if (saleStatus !== 'DRAFT') {
                const operation = id('sale-operation', i);
                add('stock_operation', 'id,organization_id,actor_id,kind,sorting_id,sale_id,reversal_of,occurred_at,recorded_at', [operation, f.org, actor, 'SALE', null, sale, null, iso(finish), iso(finish)]);
                for (let j = 0; j < 2; j++) add('stock_movement', 'id,organization_id,operation_id,material_subtype_id,weight_delta_kg,volume_delta_m3', [id('sale-movement', i * 2 + j), f.org, operation, f.materials[j], decimal(-weights[j], 4), '0']);
                if (saleStatus === 'REVERSED') {
                    const reversal = id('reversal', i);
                    add('stock_operation', 'id,organization_id,actor_id,kind,sorting_id,sale_id,reversal_of,occurred_at,recorded_at', [reversal, f.org, actor, 'REVERSAL', null, null, operation, iso(finish + 40 * 86400000), iso(finish + 40 * 86400000)]);
                    for (let j = 0; j < 2; j++) add('stock_movement', 'id,organization_id,operation_id,material_subtype_id,weight_delta_kg,volume_delta_m3', [id('reversal-movement', i * 2 + j), f.org, reversal, f.materials[j], decimal(weights[j], 4), '0']);
                }
            }
        }
    }
    for (let o = 0; o < 2; o++) {
        const f = refs[o];
        for (let j = 0; j < 2; j++) {
            const sales = data.sales.filter(s => s.o === o), original = sales.reduce((s, sale) => s + sale.weights[j], 10000000n), consumed = sales.filter(s => s.status === 'POSTED' && s.active).reduce((s, sale) => s + sale.weights[j], 0n);
            const donation = id('donation', o * 2 + j), input = id('opening-input', o * 2 + j), sorting = id('sorting', o * 2 + j), sorted = id('sorted-item', o * 2 + j), operation = id('sorting-operation', o * 2 + j);
            add('donation', 'id,organization_id,donor_id,donation_date,total_weight_kg,creator_id,created_at,updated_at', [donation, f.org, f.donor, CREATED, decimal(original, 4), actor, CREATED, CREATED]);
            add('input_item', 'id,organization_id,collection_id,donation_id,material_subtype_id,weight_kg,volume_m3,is_active,created_at,updated_at', [input, f.org, null, donation, f.materials[j], decimal(original, 4), '0', true, CREATED, CREATED]);
            add('sorting', 'id,organization_id,sorting_date,sorting_type,creator_id,created_at,updated_at', [sorting, f.org, CREATED, 'DONATION', actor, CREATED, CREATED]);
            add('sorted_item', 'id,organization_id,sorting_id,input_item_id,material_subtype_id,weight_kg,volume_m3,created_at,updated_at', [sorted, f.org, sorting, input, f.materials[j], decimal(original, 4), '0', CREATED, CREATED]);
            add('stock_lot', 'id,organization_id,material_subtype_id,sorted_item_id,original_weight_kg,original_volume_m3,available_weight_kg,available_volume_m3,created_at', [f.lots[j], f.org, f.materials[j], sorted, decimal(original, 4), '0', decimal(original - consumed, 4), '0', CREATED]);
            add('stock_operation', 'id,organization_id,actor_id,kind,sorting_id,sale_id,reversal_of,occurred_at,recorded_at', [operation, f.org, actor, 'SORTING', sorting, null, null, CREATED, CREATED]);
            add('stock_movement', 'id,organization_id,operation_id,material_subtype_id,weight_delta_kg,volume_delta_m3', [id('sorting-movement', o * 2 + j), f.org, operation, f.materials[j], decimal(original, 4), '0']);
            add('inventory_balance', 'id,organization_id,material_subtype_id,current_weight_kg,current_volume_m3,last_updated_at', [id('balance', o * 2 + j), f.org, f.materials[j], decimal(original - consumed, 4), '0', '2026-11-15T03:00:00Z']);
        }
    }
    const block = table => {
        const t = tables.get(table);
        return t ? 'COPY ' + table + '(' + t.columns + ') FROM stdin;\n' + t.rows.join('\n') + '\n\\.\n' : '';
    };
    const parents = ['users','organization','organization_membership','vehicle','team_member','material_category','material_type','material_subtype','municipality','neighborhood','route','route_area'];
    let sql = 'BEGIN;\nCREATE TEMP TABLE benchmark_states(id uuid,state text,start_at timestamptz,finish_at timestamptz,km numeric);\nCREATE TEMP TABLE benchmark_coverage(id uuid,covered boolean);\n' + parents.map(block).join('');
    sql += ['route_execution','route_execution_team','route_execution_area','benchmark_states','benchmark_coverage'].map(block).join('');
    sql += "UPDATE route_execution e SET status='IN_PROGRESS',started_at=s.start_at,version=1 FROM benchmark_states s WHERE e.id=s.id AND s.state<>'PLANNED';\nUPDATE route_execution_area a SET covered=c.covered FROM benchmark_coverage c,route_execution e WHERE a.id=c.id AND e.id=a.execution_id AND e.status='IN_PROGRESS';\nUPDATE route_execution e SET status='COMPLETED',finished_at=s.finish_at,distance_km=s.km,updated_at=s.finish_at,version=2 FROM benchmark_states s WHERE e.id=s.id AND s.state='COMPLETED';\nUPDATE route_execution e SET status='CANCELLED',cancelled_at=s.finish_at+interval '1 second',cancellation_reason='Synthetic cancellation',updated_at=s.finish_at+interval '1 second',version=2 FROM benchmark_states s WHERE e.id=s.id AND s.state='CANCELLED';\n";
    sql += ['donor','buyer','attachment','donation','collection','collection_team','input_item'].map(block).join('');
    sql += ['sorting','sorted_item','stock_lot','sale','sale_item','stock_operation','stock_movement','inventory_balance','route_event'].map(block).join('') + 'COMMIT;\nANALYZE;\n';
    const counts = Object.fromEntries([...tables].filter(([name]) => !name.startsWith('benchmark_')).map(([name, t]) => [name, t.rows.length]));
    return { data, sql, counts, digest: crypto.createHash('sha256').update(sql).digest('hex') };
}
