package pl.databucket.server.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import pl.databucket.server.dto.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a dataset as a file like those in src/main/resources/datasets: the objects indented, one data row per line
 * (a dataset can hold thousands of rows). Users and audit fields (who and when created or modified) are left out:
 * they are not part of a dataset.
 */
public final class DatasetWriter {

    @JsonIgnoreProperties({"usersIds", "createdBy", "createdAt", "modifiedBy", "modifiedAt"})
    private abstract static class WithoutUsersAndAudit {
    }

    private static final List<Class<?>> OBJECTS = List.of(TeamDto.class, DataEnumDto.class, DataClassDto.class,
            GroupDto.class, BucketDto.class, TagDto.class, DataColumnsDto.class, DataFilterDto.class, ViewDto.class,
            TaskDto.class, SvgDto.class);

    private static final String NL = "\n";

    private final ObjectWriter objects;
    private final ObjectWriter compact;

    public DatasetWriter(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy();
        // only on the dataset's objects, never on maps: data properties may have keys like "createdBy"
        OBJECTS.forEach(type -> mapper.addMixIn(type, WithoutUsersAndAudit.class));

        DefaultIndenter indenter = new DefaultIndenter("  ", NL);
        Separators separators = Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withObjectEmptySeparator("")
                .withArrayEmptySeparator("");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withSeparators(separators);
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);

        this.objects = mapper.writer(printer);
        this.compact = mapper.writer();
    }

    public String write(DatasetDto dataset) throws IOException {
        Map<String, List<DataCreateDTO>> data = dataset.getData();
        String text;
        try {
            dataset.setData(new LinkedHashMap<>());
            text = objects.writeValueAsString(dataset);
        } finally {
            dataset.setData(data);
        }

        // "data" is the last field: its empty object is replaced by the rows, one per line
        String empty = "\"data\": {}";
        int at = text.lastIndexOf(empty);
        if (at < 0)
            throw new IllegalStateException("No data field in the written dataset");

        StringBuilder rows = new StringBuilder("\"data\": {");
        boolean firstBucket = true;
        for (Map.Entry<String, List<DataCreateDTO>> bucket : data.entrySet()) {
            rows.append(firstBucket ? "" : ",").append(NL)
                    .append("    ").append(compact.writeValueAsString(bucket.getKey())).append(": [");
            boolean firstRow = true;
            for (DataCreateDTO row : bucket.getValue()) {
                rows.append(firstRow ? "" : ",").append(NL).append("      ").append(compact.writeValueAsString(row));
                firstRow = false;
            }
            rows.append(bucket.getValue().isEmpty() ? "]" : NL + "    ]");
            firstBucket = false;
        }
        rows.append(data.isEmpty() ? "}" : NL + "  }");

        return text.substring(0, at) + rows + text.substring(at + empty.length()) + NL;
    }
}
