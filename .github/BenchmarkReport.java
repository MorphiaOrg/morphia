///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import static java.lang.String.format;

/**
 * Renders the JMH JSON results of each mapper variant as side-by-side Markdown tables.
 *
 * Usage: jbang BenchmarkReport.java <directory of jmh-*.json files>
 */
public class BenchmarkReport {
    private static final List<String> VARIANTS = List.of("reflection", "critter", "critter-runtime");
    private static final String BASELINE = "reflection";
    private static final String ALLOC = "gc.alloc.rate.norm";

    record Result(double score, double error, String unit, Double allocPerOp) {
    }

    public static void main(String... args) throws IOException {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        File dir = new File(args.length > 0 ? args[0] : ".");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            System.out.println("No benchmark results found in " + dir);
            return;
        }
        Arrays.sort(files);

        // row key -> variant -> result
        Map<String, Map<String, Result>> rows = new TreeMap<>();
        List<String> seen = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (File file : files) {
            for (JsonNode node : mapper.readTree(file)) {
                Map<String, String> params = new TreeMap<>();
                String variant = "unknown";
                for (Iterator<Entry<String, JsonNode>> it = node.path("params").fields(); it.hasNext();) {
                    Entry<String, JsonNode> param = it.next();
                    if (param.getKey().equals("variant")) {
                        variant = param.getValue().asText();
                    } else {
                        params.put(param.getKey(), param.getValue().asText());
                    }
                }
                if (!seen.contains(variant)) {
                    seen.add(variant);
                }
                String benchmark = node.get("benchmark").asText();
                benchmark = benchmark.substring(benchmark.lastIndexOf('.', benchmark.lastIndexOf('.') - 1) + 1);
                String key = params.isEmpty() ? benchmark : benchmark + " | " + String.join(", ", params.values());

                JsonNode primary = node.get("primaryMetric");
                JsonNode alloc = node.path("secondaryMetrics").get(ALLOC);
                rows.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(variant, new Result(
                        primary.get("score").asDouble(),
                        primary.path("scoreError").isNumber() ? primary.get("scoreError").asDouble() : Double.NaN,
                        primary.get("scoreUnit").asText(),
                        alloc == null ? null : alloc.get("score").asDouble()));
            }
        }

        List<String> variants = new ArrayList<>(VARIANTS.stream().filter(seen::contains).toList());
        seen.stream().filter(v -> !variants.contains(v)).forEach(variants::add);

        System.out.println("## Mapper profiling results");
        System.out.println();
        System.out.println("Lower is better for every benchmark here. Ratios compare each variant against `" + BASELINE
                + "`: below 1.00× means faster. The legs ran on different runners, so read small differences with care.");
        System.out.println();
        printTable(rows, variants, false);
        if (rows.values().stream().flatMap(m -> m.values().stream()).anyMatch(r -> r.allocPerOp() != null)) {
            System.out.println();
            System.out.println("### Allocation per operation (`gc.alloc.rate.norm`)");
            System.out.println();
            printTable(rows, variants, true);
        }
    }

    private static void printTable(Map<String, Map<String, Result>> rows, List<String> variants, boolean alloc) {
        List<String> ratioVariants = variants.stream().filter(v -> !v.equals(BASELINE)).toList();
        StringBuilder header = new StringBuilder("| Benchmark | Params |");
        StringBuilder rule = new StringBuilder("|---|---|");
        for (String variant : variants) {
            header.append(' ').append(variant).append(" |");
            rule.append("---:|");
        }
        if (variants.contains(BASELINE)) {
            for (String variant : ratioVariants) {
                header.append(' ').append(variant).append(" / ").append(BASELINE).append(" |");
                rule.append("---:|");
            }
        }
        System.out.println(header);
        System.out.println(rule);

        for (Entry<String, Map<String, Result>> row : rows.entrySet()) {
            String[] key = row.getKey().split(" \\| ", 2);
            StringBuilder line = new StringBuilder(format("| `%s` | %s |", key[0], key.length > 1 ? key[1] : ""));
            Map<String, Result> results = row.getValue();
            for (String variant : variants) {
                Result result = results.get(variant);
                line.append(' ').append(result == null ? "—" : alloc ? bytes(result.allocPerOp()) : score(result)).append(" |");
            }
            if (variants.contains(BASELINE)) {
                Result baseline = results.get(BASELINE);
                for (String variant : ratioVariants) {
                    Result result = results.get(variant);
                    line.append(' ').append(ratio(baseline, result, alloc)).append(" |");
                }
            }
            System.out.println(line);
        }
    }

    private static String score(Result result) {
        String error = Double.isNaN(result.error()) ? "" : format(" ± %s", number(result.error()));
        return format("%s%s %s", number(result.score()), error, result.unit());
    }

    private static String bytes(Double value) {
        return value == null ? "—" : format("%,.0f B/op", value);
    }

    private static String ratio(Result baseline, Result result, boolean alloc) {
        if (baseline == null || result == null) {
            return "—";
        }
        double base = alloc ? valueOrZero(baseline.allocPerOp()) : baseline.score();
        double value = alloc ? valueOrZero(result.allocPerOp()) : result.score();
        if (base == 0) {
            return "—";
        }
        double ratio = value / base;
        String marker = ratio <= 0.95 ? " 🟢" : ratio >= 1.05 ? " 🔴" : "";
        return format("%.2f×%s", ratio, marker);
    }

    private static double valueOrZero(Double value) {
        return value == null ? 0 : value;
    }

    private static String number(double value) {
        return Math.abs(value) >= 100 ? format("%,.0f", value) : format("%,.2f", value);
    }
}
