package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.service.data.COL;

@Getter
@Setter
public class CustomColumnDto {
    private String field;
    private String title;

    public void setField(String field) {
        this.field = convertField(field);
    }

    private String convertField(String field) {
        switch (field) {
            case "id": return COL.DATA_ID;
            case "tagId": return COL.TAG_ID;
            case "owner": return COL.RESERVED_BY;
            case "createdBy": return COL.CREATED_BY;
            case "createdAt": return COL.CREATED_AT;
            case "modifiedBy": return COL.MODIFIED_BY;
            case "modifiedAt": return COL.MODIFIED_AT;
            default: return field;
        }
    }
}
