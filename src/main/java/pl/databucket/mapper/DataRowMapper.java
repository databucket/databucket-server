package pl.databucket.mapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;
import pl.databucket.service.data.COL;
import pl.databucket.dto.DataDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

public final class DataRowMapper implements RowMapper<DataDTO> {

    @SneakyThrows
    @Override
    public DataDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        DataDTO dataDto = new DataDTO();
        dataDto.setId(rs.getLong(COL.DATA_ID));
        dataDto.setTagId(rs.getLong(COL.TAG_ID));
        dataDto.setReserved(rs.getBoolean(COL.RESERVED));
        dataDto.setOwner(rs.getString(COL.RESERVED_BY));
        dataDto.setProperties(convertPGObjectToMap((PGobject) rs.getObject(COL.PROPERTIES)));
        Timestamp createdAt = rs.getTimestamp(COL.CREATED_AT);
        if (createdAt != null)
            dataDto.setCreatedAt(new Date(createdAt.getTime()));
        dataDto.setCreatedBy(rs.getString(COL.CREATED_BY));
        Timestamp modifiedAt = rs.getTimestamp(COL.MODIFIED_AT);
        if (modifiedAt != null)
            dataDto.setModifiedAt(new Date(modifiedAt.getTime()));
        dataDto.setModifiedBy(rs.getString(COL.MODIFIED_BY));
        return dataDto;
    }

    public Map<String, Object> convertPGObjectToMap(PGobject source) throws JsonProcessingException {
        return new ObjectMapper().readValue(source.getValue(), new TypeReference<Map<String, Object>>() {
        });
    }
}

