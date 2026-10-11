// Renders the mapper benchmark history recorded by the "Mapper Profiling" workflow in MorphiaOrg/morphia.
// data.js is fetched from the benchmark-data branch at docs build time and sets window.BENCHMARK_DATA in the
// github-action-benchmark format: entries["Mapper: <variant>"] = [{ commit, date, benches: [{ name, value, unit }] }].
(function () {
    'use strict';

    var VARIANTS = ['reflection', 'critter', 'critter-runtime'];
    var BASELINE = 'reflection';
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

    function message(text) {
        var target = document.getElementById('benchmark-status');
        target.textContent = text;
        target.hidden = false;
    }

    // "dev.morphia.benchmarks.CodecBenchmark.decode ( {"model":"SIMPLE","variant":"critter"} )"
    //   -> { key: "CodecBenchmark.decode | SIMPLE", variant: "critter" }
    function parseName(name) {
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
        var variant = params.variant || 'unknown';
        delete params.variant;
        var values = Object.keys(params).sort().map(function (k) {
            return params[k];
        });
        return { key: values.length ? benchmark + ' | ' + values.join(', ') : benchmark, variant: variant };
    }

    function shortSha(commit) {
        return commit.id.substring(0, 8);
    }

    function firstLine(text) {
        return (text || '').split('\n')[0];
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
        var commits = {};
        var series = {};
        var order = [];
        VARIANTS.forEach(function (variant) {
            var runs = (data.entries || {})['Mapper: ' + variant] || [];
            runs.forEach(function (run) {
                var id = run.commit.id;
                if (!commits[id]) {
                    commits[id] = { commit: run.commit, date: run.date };
                    order.push(id);
                }
                run.benches.forEach(function (bench) {
                    var parsed = parseName(bench.name);
                    var byVariant = series[parsed.key] = series[parsed.key] || {};
                    var byCommit = byVariant[variant] = byVariant[variant] || {};
                    byCommit[id] = { value: bench.value, unit: bench.unit };
                });
            });
        });
        order.sort(function (a, b) {
            return commits[a].date - commits[b].date;
        });
        return { commits: commits, order: order, series: series };
    }

    function latestFor(byVariant, order) {
        for (var i = order.length - 1; i >= 0; i--) {
            var id = order[i];
            if (VARIANTS.some(function (v) { return byVariant[v] && byVariant[v][id]; })) {
                return id;
            }
        }
        return null;
    }

    function renderTable(model, repoUrl) {
        var container = document.getElementById('benchmark-latest');
        var lastId = model.order[model.order.length - 1];
        var last = model.commits[lastId];
        var caption = el('p', { 'class': 'benchmark-caption' });
        caption.appendChild(document.createTextNode('Most recent run: '));
        caption.appendChild(el('a', { href: repoUrl + '/commit/' + lastId }, shortSha(last.commit)));
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
        Object.keys(model.series).sort().forEach(function (key) {
            var byVariant = model.series[key];
            var id = latestFor(byVariant, model.order);
            var row = el('tr');
            row.appendChild(el('td', { 'class': 'tableblock halign-left' }, key));
            var baseline = byVariant[BASELINE] && byVariant[BASELINE][id];
            var unit = VARIANTS.map(function (v) {
                return byVariant[v] && byVariant[v][id] ? byVariant[v][id].unit : null;
            }).filter(Boolean)[0] || '';
            row.appendChild(el('td', { 'class': 'tableblock halign-left' }, unit));
            VARIANTS.forEach(function (variant) {
                var result = byVariant[variant] && byVariant[variant][id];
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

    function renderCharts(model, repoUrl) {
        var container = document.getElementById('benchmark-charts');
        if (typeof Chart === 'undefined') {
            container.appendChild(el('p', {}, 'The charting library could not be loaded; the table above has the latest numbers.'));
            return;
        }
        Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
        Chart.defaults.color = '#52514e';

        var labels = model.order.map(function (id) {
            return shortSha(model.commits[id].commit);
        });

        Object.keys(model.series).sort().forEach(function (key) {
            var byVariant = model.series[key];
            var unit = '';
            var datasets = VARIANTS.filter(function (v) { return byVariant[v]; }).map(function (variant) {
                return {
                    label: variant,
                    data: model.order.map(function (id) {
                        var result = byVariant[variant][id];
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
                        if (elements.length) {
                            window.open(repoUrl + '/commit/' + model.order[elements[0].index], '_blank', 'noopener');
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
                                    return shortSha(run.commit) + ' · ' + formatDate(run.date);
                                },
                                label: function (item) {
                                    return formatValue(item.parsed.y) + ' ' + unit + '  ' + item.dataset.label;
                                },
                                footer: function (items) {
                                    var run = model.commits[model.order[items[0].dataIndex]];
                                    return [firstLine(run.commit.message), 'Click to open the commit'];
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    function renderRuns(model, repoUrl) {
        var list = document.getElementById('benchmark-runs');
        model.order.slice().reverse().forEach(function (id) {
            var run = model.commits[id];
            var item = el('li');
            item.appendChild(el('a', { href: repoUrl + '/commit/' + id, 'class': 'benchmark-sha' }, shortSha(run.commit)));
            item.appendChild(document.createTextNode(' ' + formatDate(run.date) + ' — ' + firstLine(run.commit.message)));
            list.appendChild(item);
        });
    }

    function render() {
        var data = window.BENCHMARK_DATA;
        if (!data || !data.entries) {
            message('Benchmark results are not available right now. They are published from the Mapper Profiling workflow; ' +
                'please check back later.');
            return;
        }
        var model = collect(data);
        if (!model.order.length) {
            message('No benchmark runs have been recorded yet.');
            return;
        }
        var repoUrl = data.repoUrl || 'https://github.com/MorphiaOrg/morphia';
        renderTable(model, repoUrl);
        renderCharts(model, repoUrl);
        renderRuns(model, repoUrl);
        document.getElementById('benchmark-results').hidden = false;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', render);
    } else {
        render();
    }
})();
