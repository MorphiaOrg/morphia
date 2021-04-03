package util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Mojo(name = "revapi-config", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class RevApiConfig extends AbstractMojo {
    @Parameter(name = "input", defaultValue = "${project.basedir}/config/revapi-input.json")
    private File input;
    @Parameter(name = "output", defaultValue = "${project.basedir}/config/revapi.json")
    private File output;

    public static void main(String[] args) throws MojoExecutionException {
        RevApiConfig config = new RevApiConfig();
        config.input = new File("../config/revapi-input-new.json");
        config.output = new File("../config/revapi.json");

        config.execute();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {
        try {
            ObjectMapper mapper = new ObjectMapper(new JsonFactory());
            Map<String, Object> inputMap = (Map<String, Object>) mapper.readValue(input, HashMap.class);
            //            List<Map<String, String>> seedMap = (List<Map<String, String>>) mapper.readValue(seed, ArrayList.class);
            Map config = new LinkedHashMap();
            config.put("extension", "revapi.ignore");
            final List<Map> nodes = new ArrayList<>();

            for (Entry<String, Object> entry : inputMap.entrySet()) {
                Map<String, List<Map<String, String>>> flatten = flatten(entry);
                flatten.forEach((code, instances) -> {
                    nodes.addAll(instances.stream()
                                          .map(instance -> {
                                              Map node = new LinkedHashMap();
                                              node.put("code", "java." + code);
                                              node.putAll(instance);
                                              return node;
                                          })
                                          .collect(Collectors.toList()));
                });
            }

            config.put("configuration", nodes);

            mapper.writer()
                  .withDefaultPrettyPrinter()
                  .writeValue(output, singletonList(config));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Map<String, List<Map<String, String>>> flatten(Entry<String, Object> entry) {
        String key = entry.getKey();
        Map<String, List<Map<String, String>>> flat = new TreeMap<>();
        if (entry.getValue() instanceof Map) {
            for (Object object : ((Map) entry.getValue()).entrySet()) {
                flatten((Entry<String, Object>) object)
                    .forEach((k, v) -> {
                        flat.put(key + "." + k, v);
                    });
            }
        } else if (entry.getValue() instanceof List) {
            flat.put(key, (List<Map<String, String>>) entry.getValue());
        }

        return flat;
    }
}

