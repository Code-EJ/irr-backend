/** Independent exact-integer report oracle. @author Enzo Ribas (https://github.com/oEnzoRibas) */
import assert from 'node:assert/strict';

/** Formats fixed-point integers without binary floating-point business arithmetic. */
export function decimal(value, scale = 0) {
    if (value === null) return null;
    const negative = value < 0n; let text = (negative ? -value : value).toString().padStart(scale + 1, '0');
    if (scale) text = text.slice(0, -scale) + '.' + text.slice(-scale);
    return canonical((negative ? '-' : '') + text);
}
export function canonical(value) {
    let text = String(value);
    if (text.includes('.')) text = text.replace(/0+$/, '').replace(/\.$/, '');
    return text === '-0' ? '0' : text;
}
/** Integer HALF_UP, including negative deviations, rounded only after summing the cohort. */
export function ratio(numerator, denominator, scaleDifference = 0, multiplier = 1n) {
    if (numerator === null || denominator === null || denominator <= 0n) return null;
    const signed = numerator * multiplier;
    let n = signed < 0n ? -signed : signed, d = denominator;
    const exponent = 6 + scaleDifference;
    if (exponent >= 0) n *= 10n ** BigInt(exponent); else d *= 10n ** BigInt(-exponent);
    return decimal((signed < 0n ? -1n : 1n) * ((n + d / 2n) / d), 6);
}
const sum = (rows, get) => rows.reduce((total, row) => total + get(row), 0n);
const inPeriod = (time, query) => time >= Date.parse(query.from + 'T03:00:00Z') && time < Date.parse(query.to + 'T03:00:00Z') + 86400000;
const matches = (row, query) => row.org === query.org && (!query.route || row.route === query.route);

/** Selects modeled facts in JavaScript; no production query or formula implementation is invoked. */
export function expected(data, query, endpoint) {
    const executions = data.executions.filter(e => matches(e, query) && e.status === 'COMPLETED' && inPeriod(e.finish, query));
    const receipts = data.receipts.filter(c => c.active && matches(c, query) && inPeriod(c.time, query));
    const measured = executions.filter(e => e.km !== null);
    const operations = {
        completedExecutions: executions.length, measuredExecutions: measured.length,
        missingDistanceExecutions: executions.length - measured.length,
        reportedDistanceKm: measured.length ? decimal(sum(measured, e => e.km), 3) : null,
        durationSeconds: decimal(sum(executions, e => BigInt(e.seconds))),
        collectionCount: receipts.length, collectedWeightKg: decimal(sum(receipts, c => c.kg), 4),
        unlinkedCollections: receipts.filter(c => c.execution === null).length,
    };
    if (endpoint === 'operations') return operations;
    const comparison = (plannedKey, actualKey, scale) => {
        const rows = executions.filter(e => e[plannedKey] !== null && e[actualKey] !== null);
        const planned = rows.length ? sum(rows, e => e[plannedKey]) : null;
        const actual = rows.length ? sum(rows, e => e[actualKey]) : null;
        const delta = rows.length ? actual - planned : null;
        return { consideredExecutions: rows.length, excludedExecutions: executions.length - rows.length,
            planned: decimal(planned, scale), actual: decimal(actual, scale),
            signedDeviation: decimal(delta, scale), absoluteDeviation: decimal(delta === null ? null : delta < 0n ? -delta : delta, scale),
            percentageDeviation: ratio(delta, planned, 0, 100n) };
    };
    const linked = new Map();
    for (const c of data.receipts) if (c.active && c.execution) {
        const list = linked.get(c.execution) ?? []; list.push(c); linked.set(c.execution, list);
    }
    const productivity = (key, scale, multiplier) => {
        const withReceipts = executions.filter(e => linked.has(e.id));
        const rows = withReceipts.filter(e => e[key] !== null && e[key] > 0n);
        const kg = rows.length ? sum(rows, e => sum(linked.get(e.id), c => c.kg)) : null;
        const denominator = rows.length ? sum(rows, e => e[key]) : null;
        return { consideredExecutions: rows.length, collectionCount: rows.reduce((n, e) => n + linked.get(e.id).length, 0),
            excludedExecutions: executions.length - rows.length, withoutLinkedCollections: executions.length - withReceipts.length,
            missingDenominatorExecutions: withReceipts.filter(e => e[key] === null).length,
            zeroDenominatorExecutions: withReceipts.filter(e => e[key] === 0n).length,
            collectedWeightKg: decimal(kg, 4), denominator: decimal(denominator, scale), value: ratio(kg, denominator, scale - 4, multiplier) };
    };
    const declarations = executions.flatMap(e => e.sections);
    const unique = new Map();
    for (const section of declarations) unique.set(section.key, section.covered);
    const covered = [...unique.values()].filter(Boolean).length;
    const sales = data.sales.filter(s => s.org === query.org && s.active && inPeriod(s.time, query));
    const available = !query.route;
    return {
        completedExecutions: executions.length,
        unlinkedCollectionsInPeriod: data.receipts.filter(c => c.org === query.org && c.execution === null && c.active && inPeriod(c.time, query)).length,
        distanceComparison: comparison('plannedKm', 'km', 3), durationComparison: comparison('plannedSeconds', 'seconds', 0),
        coverage: { consideredExecutions: executions.filter(e => e.sections.length).length,
            excludedExecutions: executions.filter(e => !e.sections.length).length, declaredSections: unique.size,
            coveredSections: covered, duplicateRows: declarations.length - unique.size, conflictingSections: 0,
            percentage: ratio(BigInt(covered), BigInt(unique.size), 0, 100n) },
        hourlyProductivity: productivity('seconds', 0, 3600n), distanceProductivity: productivity('km', 3, 1n),
        revenue: { available, availabilityReason: available ? 'AVAILABLE' : 'NO_SALE_EXECUTION_PROVENANCE',
            consideredSales: available ? sales.filter(s => s.status === 'POSTED').length : null,
            excludedDraftSales: available ? sales.filter(s => s.status === 'DRAFT').length : null,
            excludedReversedSales: available ? sales.filter(s => s.status === 'REVERSED').length : null,
            amount: available ? decimal(sum(sales.filter(s => s.status === 'POSTED'), s => s.cents), 2) : null },
    };
}

/** Checks every expected count and decimal, including nulls; metadata is checked separately. */
export function assertReport(actual, wanted, prefix = '') {
    for (const [key, value] of Object.entries(wanted)) {
        const location = prefix + key;
        if (value && typeof value === 'object') assertReport(actual[key], value, location + '.');
        else if (typeof value === 'string' && /^-?\d+(\.\d+)?$/.test(value)) {
            assert.equal(typeof actual[key], 'string', location + ' must serialize an exact decimal string');
            assert.equal(canonical(actual[key]), canonical(value), location);
        } else assert.deepEqual(actual[key], value, location);
    }
}
