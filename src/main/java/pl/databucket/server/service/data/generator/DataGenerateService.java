package pl.databucket.server.service.data.generator;

import org.springframework.stereotype.Service;
import pl.databucket.server.dto.DataCreateDTO;
import pl.databucket.server.dto.DataGenerateDTO;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.service.data.DataService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates data rows with {@link DataGenerator} and inserts them into a bucket in one batch (all rows or none).
 * Used by the REST API, the web application and the MCP tool generate_data. Errors in the request are
 * {@link IllegalArgumentException}s saying what to change.
 */
@Service
public class DataGenerateService {

    public static final int MAX_ROWS = 10_000;
    public static final int PREVIEW_ROWS = 10;
    public static final int SAMPLE = 3;

    private final DataService dataService;

    public DataGenerateService(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * @param inserted rows inserted (0 for a preview)
     * @param firstN   {n} of the first row
     * @param lastN    {n} of the last row
     * @param rows     the first rows (a sample), or all rows of a preview
     */
    public record Generated(int inserted, long firstN, long lastN, List<Map<String, Object>> rows) {
    }

    /** Generates and inserts the rows; returns the first {@link #SAMPLE} of them. */
    public Generated generate(User user, Bucket bucket, DataGenerateDTO request) {
        Integer count = request.getCount();
        if (count == null || count < 1 || count > MAX_ROWS)
            throw new IllegalArgumentException("Give count 1.." + MAX_ROWS + "; generate again for more rows");
        DataGenerator generator = DataGenerator.of(request.getFields(), request.getLocale(), request.getSeed());
        long firstN = firstN(bucket, request);

        List<Map<String, Object>> generated = generator.next(firstN, count);
        List<DataCreateDTO> rows = new ArrayList<>(count);
        for (Map<String, Object> properties : generated) {
            DataCreateDTO row = new DataCreateDTO();
            row.setProperties(properties);
            row.setTagId(request.getTagId());
            row.setReserved(request.getReserved());
            rows.add(row);
        }
        dataService.createData(user, bucket, rows);
        return new Generated(count, firstN, firstN + count - 1, generated.subList(0, Math.min(SAMPLE, count)));
    }

    /**
     * The first rows that {@link #generate} would make (the same rows with a seed), at most {@link #PREVIEW_ROWS};
     * nothing is inserted.
     */
    public Generated preview(Bucket bucket, DataGenerateDTO request) {
        Integer count = request.getCount();
        if (count != null && (count < 1 || count > MAX_ROWS))
            throw new IllegalArgumentException("Give count 1.." + MAX_ROWS);
        int rows = Math.min(count != null ? count : PREVIEW_ROWS, PREVIEW_ROWS);
        DataGenerator generator = DataGenerator.of(request.getFields(), request.getLocale(), request.getSeed());
        long firstN = firstN(bucket, request);
        return new Generated(0, firstN, firstN + rows - 1, generator.next(firstN, rows));
    }

    private long firstN(Bucket bucket, DataGenerateDTO request) {
        return request.getStartN() != null ? request.getStartN() : dataService.lastDataId(bucket) + 1;
    }
}
