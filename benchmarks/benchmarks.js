// Renders the mapper benchmark history recorded by the "Mapper Profiling" workflow in MorphiaOrg/morphia.
// data.json is extracted from the benchmark-data branch's data.js at docs build time and is in the
// github-action-benchmark format: entries["Mapper: <variant>"] = [{ commit, date, benches: [{ name, value, unit }] }].
// The data is treated as untrusted: it is parsed as JSON, never executed, and only reaches the DOM through textContent
// and setAttribute.
(function () {
    'use strict';

    var VARIANTS = ['reflection', 'critter', 'critter-runtime'];
    var BASELINE = 'reflection';
    var REPO_URL = 'https://github.com/MorphiaOrg/morphia';
    var SHA = /^[0-9a-f]{7,40}$/;
    var COLORS = { 'reflection': '#2a78d6', 'critter': '#eb6834', 'critter-runtime': '#1baf7a' };
    var DASHES = { 'reflection': [], 'critter': [6, 4], 'critter-runtime': [2, 3] };
    var POINTS = { 'reflection': 'circle', 'critter': 'rectRot', 'critter-runtime': 'triangle' };

    function el(tag, attrs, text) {
        var node = document.createElement(tag);
        Object.keys(attrs || {}).forEach(function (key) {
            node.setAttribute(key, attrs[key]);
        });
        if (text !== undefined) {
            node.textContent = text;
        }
        return node;
    }

    // Returns the GitHub URL of a commit, or null if the id isn't a plain hex sha.
    function commitUrl(id) {
        return SHA.test(id) ? REPO_URL + '/commit/' + id : null;
    }

    // A link to the commit when its id is a valid sha, otherwise plain text.
    function commitLink(id, attrs) {
        var url = commitUrl(id);
        if (!url) {
            return el('span', attrs, String(id).substring(0, 8));
        }
        var link = el('a', attrs, id.substring(0, 8));
        link.setAttribute('href', url);
        return link;
    }

    function message(text) {
        var target = document.getElementById('benchmark-status');
        target.textContent = text;
        target.hidden = false;
    }

    // "dev.morphia.benchmarks.CodecBenchmark.decode ( {"model":"SIMPLE","variant":"critter"} )"
    //   -> { key: "CodecBenchmark.decode | SIMPLE", variant: "critter" }
    function parseName(name) {
        name = String(name);
        var match = /^(.*?)\s*\(\s*(\{.*\})\s*\)\s*$/.exec(name);
        var method = match ? match[1] : name;
        var params = {};
        if (match) {
            try {
                params = JSON.parse(match[2]);
            } catch (e) {
                params = {};
            }
        }
        var parts = method.split('.');
        var benchmark = parts.slice(-2).join('.');
        if (!params || typeof params !== 'object' || Array.isArray(params)) {
            params = {};
        }
        var variant = String(params.variant || 'unknown');
        delete params.variant;
        var values = Object.keys(params).sort().map(function (k) {
            return String(params[k]);
        });
        return { key: values.length ? benchmark + ' | ' + values.join(', ') : benchmark, variant: variant };
    }

    function firstLine(text) {
        return String(text || '').split('\n')[0];
    }

    function formatDate(timestamp) {
        var date = new Date(timestamp);
        return isNaN(date) ? '' : date.toISOString().substring(0, 10);
    }

    function formatValue(value) {
        if (value === undefined || value === null) {
            return '—';
        }
        if (value >= 100) {
            return value.toFixed(0);
        }
        if (value >= 10) {
            return value.toFixed(1);
        }
        return value.toFixed(2);
    }

    // Collects results into: commits (ordered by run time) and series[key][variant][commitId] = { value, unit }.
    function collect(data) {
        var commits = Object.create(null);
        var series = new Map();
        var order = [];
        VARIANTS.forEach(function (variant) {
            var runs = data.entries['Mapper: ' + variant];
            if (!Array.isArray(runs)) {
                return;
            }
            runs.forEach(function (run) {
                if (!run || !run.commit || typeof run.commit.id !== 'string' || !Array.isArray(run.benches)) {
                    return;
                }
                var id = run.commit.id;
                if (!Object.prototype.hasOwnProperty.call(commits, id)) {
                    commits[id] = { id: id, message: firstLine(run.commit.message), date: Number(run.date) };
                    order.push(id);
                }
                run.benches.forEach(function (bench) {
                    if (!bench || typeof bench.value !== 'number' || !isFinite(bench.value)) {
                        return;
                    }
                    var parsed = parseName(bench.name);
                    if (parsed.variant !== variant) {
                        return;
                    }
                    var byVariant = series.get(parsed.key);
                    if (!byVariant) {
                        byVariant = {};
                        series.set(parsed.key, byVariant);
                    }
                    var byCommit = byVariant[variant] = byVariant[variant] || new Map();
                    byCommit.set(id, { value: bench.value, unit: String(bench.unit || '') });
                });
            });
        });
        order.sort(function (a, b) {
            return (commits[a].date || 0) - (commits[b].date || 0);
        });
        return { commits: commits, order: order, series: series };
    }

    function sortedKeys(map) {
        return Array.from(map.keys()).sort();
    }

    // The result for a variant at a commit, or undefined.
    function resultAt(byVariant, variant, id) {
        return byVariant[variant] ? byVariant[variant].get(id) : undefined;
    }

    function latestFor(byVariant, order) {
        for (var i = order.length - 1; i >= 0; i--) {
            var id = order[i];
            if (VARIANTS.some(function (v) { return resultAt(byVariant, v, id); })) {
                return id;
            }
        }
        return null;
    }

    function renderTable(model) {
        var container = document.getElementById('benchmark-latest');
        var lastId = model.order[model.order.length - 1];
        var last = model.commits[lastId];
        var caption = el('p', { 'class': 'benchmark-caption' });
        caption.appendChild(document.createTextNode('Most recent run: '));
        caption.appendChild(commitLink(lastId));
        caption.appendChild(document.createTextNode(' on ' + formatDate(last.date) +
            '. Ratios are relative to the reflection mapper; lower is faster.'));
        container.appendChild(caption);

        var table = el('table', { 'class': 'tableblock frame-all grid-all stretch benchmark-table' });
        var head = el('thead');
        var headRow = el('tr');
        ['Benchmark', 'Unit'].concat(VARIANTS).forEach(function (label, index) {
            headRow.appendChild(el('th', { 'class': 'tableblock ' + (index > 1 ? 'halign-right' : 'halign-left') }, label));
        });
        head.appendChild(headRow);
        table.appendChild(head);

        var body = el('tbody');
        sortedKeys(model.series).forEach(function (key) {
            var byVariant = model.series.get(key);
            var id = latestFor(byVariant, model.order);
            var row = el('tr');
            row.appendChild(el('td', { 'class': 'tableblock halign-left' }, key));
            var baseline = resultAt(byVariant, BASELINE, id);
            var unit = VARIANTS.map(function (v) {
                var result = resultAt(byVariant, v, id);
                return result ? result.unit : null;
            }).filter(Boolean)[0] || '';
            row.appendChild(el('td', { 'class': 'tableblock halign-left' }, unit));
            VARIANTS.forEach(function (variant) {
                var result = resultAt(byVariant, variant, id);
                var cell = el('td', { 'class': 'tableblock halign-right' }, result ? formatValue(result.value) : '—');
                if (result && baseline && variant !== BASELINE) {
                    var ratio = result.value / baseline.value;
                    var badge = el('span', { 'class': 'benchmark-ratio ' + (ratio <= 1 ? 'faster' : 'slower') },
                        ' ×' + ratio.toFixed(2));
                    cell.appendChild(badge);
                }
                row.appendChild(cell);
            });
            body.appendChild(row);
        });
        table.appendChild(body);
        container.appendChild(table);
    }

    function renderCharts(model) {
        var container = document.getElementById('benchmark-charts');
        if (typeof Chart === 'undefined') {
            container.appendChild(el('p', {}, 'The charting library could not be loaded; the table above has the latest numbers.'));
            return;
        }
        Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
        Chart.defaults.color = '#52514e';

        var labels = model.order.map(function (id) {
            return id.substring(0, 8);
        });

        sortedKeys(model.series).forEach(function (key) {
            var byVariant = model.series.get(key);
            var unit = '';
            var datasets = VARIANTS.filter(function (v) { return byVariant[v]; }).map(function (variant) {
                return {
                    label: variant,
                    data: model.order.map(function (id) {
                        var result = byVariant[variant].get(id);
                        if (result) {
                            unit = result.unit;
                        }
                        return result ? result.value : null;
                    }),
                    borderColor: COLORS[variant],
                    backgroundColor: COLORS[variant],
                    borderDash: DASHES[variant],
                    pointStyle: POINTS[variant],
                    borderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBorderColor: '#fff',
                    pointBorderWidth: 1,
                    spanGaps: true,
                    tension: 0
                };
            });

            var figure = el('figure', { 'class': 'benchmark-chart' });
            figure.appendChild(el('figcaption', {}, key + ' (' + unit + ')'));
            var holder = el('div', { 'class': 'benchmark-canvas' });
            var canvas = el('canvas', { role: 'img', 'aria-label': key + ' over time, in ' + unit + ', per mapper variant' });
            holder.appendChild(canvas);
            figure.appendChild(holder);
            container.appendChild(figure);

            new Chart(canvas, {
                type: 'line',
                data: { labels: labels, datasets: datasets },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    interaction: { mode: 'index', intersect: false },
                    onClick: function (event, elements) {
                        var url = elements.length ? commitUrl(model.order[elements[0].index]) : null;
                        if (url) {
                            window.open(url, '_blank', 'noopener');
                        }
                    },
                    onHover: function (event, elements) {
                        event.native.target.style.cursor = elements.length ? 'pointer' : 'default';
                    },
                    scales: {
                        x: { grid: { display: false }, ticks: { font: { family: 'monospace' } } },
                        y: {
                            beginAtZero: true,
                            grid: { color: 'rgba(0, 0, 0, 0.06)' },
                            border: { display: false },
                            title: { display: true, text: unit }
                        }
                    },
                    plugins: {
                        legend: {
                            position: 'top',
                            align: 'end',
                            labels: { usePointStyle: true, boxWidth: 8, boxHeight: 8, color: '#0b0b0b' }
                        },
                        tooltip: {
                            backgroundColor: '#fff',
                            titleColor: '#0b0b0b',
                            bodyColor: '#0b0b0b',
                            footerColor: '#52514e',
                            borderColor: 'rgba(0, 0, 0, 0.15)',
                            borderWidth: 1,
                            usePointStyle: true,
                            callbacks: {
                                title: function (items) {
                                    var run = model.commits[model.order[items[0].dataIndex]];
                                    return run.id.substring(0, 8) + ' · ' + formatDate(run.date);
                                },
                                label: function (item) {
                                    return formatValue(item.parsed.y) + ' ' + unit + '  ' + item.dataset.label;
                                },
                                footer: function (items) {
                                    var run = model.commits[model.order[items[0].dataIndex]];
                                    return [run.message, 'Click to open the commit'];
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    function renderRuns(model) {
        var list = document.getElementById('benchmark-runs');
        model.order.slice().reverse().forEach(function (id) {
            var run = model.commits[id];
            var item = el('li');
            item.appendChild(commitLink(id, { 'class': 'benchmark-sha' }));
            item.appendChild(document.createTextNode(' ' + formatDate(run.date) + ' — ' + run.message));
            list.appendChild(item);
        });
    }

    var UNAVAILABLE = 'Benchmark results are not available right now. They are published from the Mapper Profiling ' +
        'workflow; please check back later.';

    function render(data) {
        if (!data || typeof data !== 'object' || !data.entries || typeof data.entries !== 'object') {
            message(UNAVAILABLE);
            return;
        }
        var model = collect(data);
        if (!model.order.length) {
            message('No benchmark runs have been recorded yet.');
            return;
        }
        renderTable(model);
        renderCharts(model);
        renderRuns(model);
        document.getElementById('benchmark-results').hidden = false;
    }

    function load() {
        if (typeof fetch !== 'function') {
            message(UNAVAILABLE);
            return;
        }
        fetch('data.json', { cache: 'no-cache' })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(render, function () {
                message(UNAVAILABLE);
            });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', load);
    } else {
        load();
    }
})();
