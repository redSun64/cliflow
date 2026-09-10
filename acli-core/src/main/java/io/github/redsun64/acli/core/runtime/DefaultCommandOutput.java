package io.github.redsun64.acli.core.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.redsun64.acli.api.CliException;
import io.github.redsun64.acli.api.CommandOutput;
import io.github.redsun64.acli.api.OutputFormat;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultCommandOutput implements CommandOutput {
    private final ObjectMapper mapper;
    private final PrintWriter out;
    private final OutputFormat format;

    public DefaultCommandOutput(ObjectMapper mapper, PrintWriter out, OutputFormat format) {
        this.mapper = mapper;
        this.out = out;
        this.format = format;
    }

    @Override
    public void success(Object data) {
        try {
            if (format == OutputFormat.JSON) {
                Map<String, Object> envelope = new LinkedHashMap<>();
                envelope.put("ok", true);
                envelope.put("data", data);
                out.println(mapper.writeValueAsString(envelope));
            } else {
                out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
            }
            out.flush();
        } catch (JsonProcessingException e) {
            throw new CliException("OUTPUT_SERIALIZATION_FAILED", e.getMessage());
        }
    }
}
