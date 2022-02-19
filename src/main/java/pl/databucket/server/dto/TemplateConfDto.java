package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TemplateConfDto {
    private List<Map<String, Object>> teams;
    private List<Map<String, Object>> tags;
    private List<Map<String, Object>> enums;
    private List<Map<String, Object>> groups;
    private List<Map<String, Object>> buckets;
    private List<Map<String, Object>> classes;
    private List<Map<String, Object>> columns;
    private List<Map<String, Object>> filters;
    private List<Map<String, Object>> views;
    private List<Map<String, Object>> tasks;
}
