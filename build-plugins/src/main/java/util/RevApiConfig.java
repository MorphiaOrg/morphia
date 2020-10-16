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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Mojo(name = "revapi-config", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class RevApiConfig extends AbstractMojo {
    @Parameter(name = "input", defaultValue = "${project.basedir}/config/revapi-input.json")
    private File input;
    @Parameter(name = "seed", defaultValue = "${project.basedir}/config/revapi-seed.json")
    private File seed;
    @Parameter(name = "output", defaultValue = "${project.basedir}/config/revapi.json")
    private File output;

    @Override
    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {

        try {
            ObjectMapper mapper = new ObjectMapper(new JsonFactory());
            Map<String, List<Map<String, String>>> inputMap = (Map<String, List<Map<String, String>>>) mapper.readValue(input,
                LinkedHashMap.class);
            List<Map<String, String>> seedMap = (List<Map<String, String>>) mapper.readValue(seed, ArrayList.class);
            Map config = new LinkedHashMap();
            config.put("extension", "revapi.ignore");

            final List<Map> nodes = new ArrayList<>(seedMap);
            inputMap.forEach((code, instances) -> {
                nodes.addAll(instances.stream()
                                      .map(instance -> {
                                          Map node = new LinkedHashMap();
                                          node.put("code", code);
                                          node.putAll(instance);
                                          return node;
                                      })
                                      .collect(Collectors.toList()));
            });
            config.put("configuration", nodes);

            mapper.writer()
                  .withDefaultPrettyPrinter()
                  .writeValue(output, singletonList(config));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}

