package pl.databucket.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.exception.*;
import pl.databucket.database.C;
import pl.databucket.database.COL;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.service.ColumnService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.ColumnResponse;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/columns")
@RestController
public class ColumnController {

    private final CustomExceptionFormatter customExceptionFormatter;
    private final ColumnService service;

    public ColumnController(ColumnService service) {
        this.service = service;
        this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(ColumnController.class));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> createColumns(
            @RequestParam String userName, 
            @RequestBody Map<String, Object> body) {
        
        ColumnResponse response = new ColumnResponse();

        try {
            String columnsName = FieldValidator.validateColumnsName(body, true);
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            List<Map<String, Object>> columns = FieldValidator.validateColumns(body, true);
            String description = FieldValidator.validateDescription(body, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

            int columnsId = service.createColumns(columnsName, bucketId, userName, columns, description, classId);
            response.setColumnsId(columnsId);
            response.setMessage("The new columns definition has been successfully created.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ItemDoNotExistsException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException | ColumnsAlreadyExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @DeleteMapping(value =
            "/{columnsId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> deleteColumns(
            @PathVariable Integer columnsId,
            @RequestParam String userName) {

        ColumnResponse response = new ColumnResponse();

        try {
            service.deleteColumns(userName, columnsId);
            response.setMessage("The columns definition has been removed.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @GetMapping(value = {
            "",
            "/{columnsId}",
            "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> getColumns(
            @PathVariable Optional<String> bucketName,
            @PathVariable Optional<Integer> columnsId,
            @RequestParam(required = false) Optional<Integer> page,
            @RequestParam(required = false) Optional<Integer> limit,
            @RequestParam(required = false) Optional<String> sort,
            @RequestParam(required = false) Optional<String> filter) {

        ColumnResponse response = new ColumnResponse();
        try {
            if (page.isPresent()) {
                FieldValidator.mustBeGraterThen0("page", page.get());
                response.setPage(page.get());
            }

            if (limit.isPresent()) {
                FieldValidator.mustBeGraterOrEqual0("limit", limit.get());
                response.setLimit(limit.get());
            }

            if (sort.isPresent()) {
                FieldValidator.validateSort(sort.get());
                response.setSort(sort.get());
            }

            List<Condition> urlConditions = null;
            if (filter.isPresent()) {
                urlConditions = FieldValidator.validateFilter(filter.get());
            }

            Map<String, Object> result = service.getColumns(bucketName, columnsId, page, limit, sort, urlConditions);

            long total = (long) result.get(C.TOTAL);
            response.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            response.setColumns((List<Map<String, Object>>) result.get(C.COLUMNS));

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
        } catch (IncorrectValueException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @PutMapping(value =
            "/{columnsId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> modifyColumns(
            @PathVariable("columnsId") Integer columnsId,
            @RequestParam String userName,
            @RequestBody LinkedHashMap<String, Object> body) {

        ColumnResponse response = new ColumnResponse();

        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
            String columnsName = FieldValidator.validateColumnsName(body, false);
            String description = FieldValidator.validateDescription(body, false);
            List<Map<String, Object>> columns = FieldValidator.validateColumns(body, false);

            service.modifyColumns(userName, columnsId, columnsName, bucketId, classId, description, columns);
            response.setMessage("Columns with id '" + columnsId + "' have been successfully modified.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }
}
