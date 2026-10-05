// Builds the human-readable scenario report written by handleSummary.

function metricValue(data, name) {
  const metric = data.metrics[name];
  if (!metric) {
    return undefined;
  }
  const v = metric.values;
  if (v.count !== undefined) return v.count;
  if (v.value !== undefined) return v.value;
  if (v.rate !== undefined) return v.rate;
  if (v.max !== undefined) return `max ${v.max}`;
  return undefined;
}

function thresholdsPassed(data, name) {
  const metric = data.metrics[name];
  if (!metric || !metric.thresholds) {
    return true;
  }
  return Object.values(metric.thresholds).every((t) => t.ok);
}

/**
 * @param title    report title
 * @param intro    lines describing the scenario
 * @param rows     [{ label, metric, expected }]
 * @param notes    extra lines appended to the report
 */
export function buildReport(data, title, intro, rows, notes) {
  const timestamp = new Date().toISOString();
  const lines = [`# ${title}`, '', `Run at: ${timestamp}`, ''];
  intro.forEach((line) => lines.push(line));
  lines.push('', '| Check | Expected | Actual | Result |', '|---|---|---|---|');
  let allPassed = true;
  rows.forEach((row) => {
    const actual = metricValue(data, row.metric);
    const passed = thresholdsPassed(data, row.metric);
    allPassed = allPassed && passed;
    lines.push(`| ${row.label} | ${row.expected} | ${actual === undefined ? 'n/a' : actual} | ${passed ? 'PASS' : 'FAIL'} |`);
  });
  const duration = data.metrics.http_req_duration;
  if (duration) {
    const d = duration.values;
    lines.push('', `HTTP latency (all requests): avg ${d.avg.toFixed(1)} ms, p95 ${d['p(95)'].toFixed(1)} ms, max ${d.max.toFixed(1)} ms`);
  }
  (notes || []).forEach((line) => lines.push(line));
  lines.push('', `**Overall: ${allPassed ? 'PASS' : 'FAIL'}**`, '');
  return { text: lines.join('\n'), passed: allPassed, timestamp };
}

export function reportFiles(dir, name, report, data) {
  const stamp = report.timestamp.replace(/[:.]/g, '-');
  const files = {};
  files[`${dir}/${name}-${stamp}.md`] = report.text;
  files[`${dir}/${name}-latest.md`] = report.text;
  files[`${dir}/${name}-latest.json`] = JSON.stringify(data, null, 2);
  files.stdout = `\n${report.text}\n`;
  return files;
}
