package pl.databucket.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.DatasetDto;
import pl.databucket.server.dto.DatasetInfoDto;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.DatasetInvalidException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ProjectNotEmptyException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.security.CustomUserDetails;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Datasets, from the super-admin area: the ones shipped with Databucket (src/main/resources/datasets/*.json, e.g.
 * DEMO), the ones in a folder of the server (databucket.datasets-dir) and files uploaded from a disk; their import
 * into an empty project and the export of a project as a dataset.
 */
@Service
public class DatasetService {

    public static final int MAX_FILE_SIZE = 50 * 1024 * 1024;

    private static final String BUILT_IN = "built-in";
    private static final String SERVER = "server";
    private static final String LOCATION = "classpath:datasets/";
    private final Logger logger = LoggerFactory.getLogger(DatasetService.class);

    @Value("${databucket.datasets-dir:}")
    private String datasetsDir;

    @Autowired private ObjectMapper objectMapper;
    @Autowired private Validator validator;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectContentService projectContentService;
    @Autowired private UserService userService;
    @Autowired private DatasetImporter datasetImporter;
    @Autowired private DatasetExporter datasetExporter;

    // the datasets to choose from; a file in the server folder replaces a built-in one of the same name
    public List<DatasetInfoDto> getDatasets() throws IOException {
        Map<String, DatasetInfoDto> datasets = new LinkedHashMap<>();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources(LOCATION + "*.json")) {
            String id = Objects.requireNonNull(resource.getFilename()).replaceFirst("\\.json$", "");
            try (InputStream in = resource.getInputStream()) {
                datasets.put(id, DatasetInfoDto.of(id, BUILT_IN, objectMapper.readValue(in, DatasetDto.class)));
            } catch (IOException e) {
                logger.error("The dataset " + resource.getFilename() + " can't be read", e);
            }
        }
        for (Path file : serverFiles()) {
            String id = file.getFileName().toString().replaceFirst("\\.json$", "");
            try (InputStream in = Files.newInputStream(file)) {
                datasets.put(id, DatasetInfoDto.of(id, SERVER, objectMapper.readValue(in, DatasetDto.class)));
            } catch (IOException e) {
                logger.error("The dataset " + file + " can't be read", e);
            }
        }
        List<DatasetInfoDto> list = new ArrayList<>(datasets.values());
        list.sort(Comparator.comparing(DatasetInfoDto::getName, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    // a built-in dataset or one from the server folder
    public int importDataset(int projectId, String datasetId) throws Exception {
        Project project = emptyProject(projectId);
        return importInto(project, readDataset(datasetId));
    }

    /**
     * A dataset file uploaded from a disk (the JSON body of the request), at most 50 MB. It is checked like the others
     * before anything is created.
     */
    public int importDatasetFile(int projectId, InputStream body) throws Exception {
        Project project = emptyProject(projectId);
        byte[] content = body.readNBytes(MAX_FILE_SIZE + 1);
        if (content.length > MAX_FILE_SIZE)
            throw new DatasetInvalidException("The file is larger than " + MAX_FILE_SIZE / 1024 / 1024 + " MB");
        DatasetDto dataset;
        try {
            dataset = objectMapper.readValue(content, DatasetDto.class);
        } catch (JsonProcessingException e) {
            throw new DatasetInvalidException("The file is not a dataset: " + e.getOriginalMessage());
        }
        if (dataset == null)
            throw new DatasetInvalidException("The file is empty");
        return importInto(project, dataset);
    }

    private Project emptyProject(int projectId) throws ItemNotFoundException, ProjectNotEmptyException {
        Project project = projectRepository.findByIdAndDeleted(projectId, false);
        if (project == null)
            throw new ItemNotFoundException(Project.class, projectId);
        if (!projectContentService.isEmpty(projectId))
            throw new ProjectNotEmptyException(project.getName(), "importing a dataset");
        return project;
    }

    /**
     * Imports the dataset into the empty project. Data rows are created by the current user (a reserved row is
     * reserved for them), who also joins the teams of the dataset.
     */
    private int importInto(Project project, DatasetDto dataset) throws Exception {
        List<String> problems = DatasetCheck.problems(dataset, validator);
        if (!problems.isEmpty())
            throw new DatasetInvalidException(problems);

        User user = userService.getCurrentUser();

        // The project of the security context is the tenant of the sessions opened in it (ProjectTenantResolver):
        // the import runs as the current user, signed in to the target project, and the original context comes back
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(contextOfProject(original, project.getId()));
            int rows = datasetImporter.importDataset(dataset, user);
            AuditLog.log("project.dataset-import", "id={} name={} dataset={} rows={}",
                    project.getId(), project.getName(), dataset.getName(), rows);
            return rows;
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    /**
     * The content of the project as a dataset file, in the format of the shipped ones, e.g. to keep a project for a
     * training in a file, or to add it to src/main/resources/datasets. The dataset is named after the project.
     */
    public String exportDataset(int projectId) throws Exception {
        Project project = projectRepository.findByIdAndDeleted(projectId, false);
        if (project == null)
            throw new ItemNotFoundException(Project.class, projectId);

        // read in the project, like the import (the project of the security context is the tenant)
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(contextOfProject(original, projectId));
            DatasetDto dataset = datasetExporter.exportDataset(project.getName(), project.getDescription());
            int rows = dataset.getData().values().stream().mapToInt(List::size).sum();
            AuditLog.log("project.dataset-export", "id={} name={} rows={}", project.getId(), project.getName(), rows);
            return new DatasetWriter(objectMapper).write(dataset);
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    private DatasetDto readDataset(String datasetId) throws IOException, ItemNotFoundException {
        // the id is a file name, never a path
        if (datasetId == null || !datasetId.matches("[a-zA-Z0-9_-]+"))
            throw new ItemNotFoundException(DatasetDto.class, datasetId);

        Path dir = serverDir();
        if (dir != null && Files.isRegularFile(dir.resolve(datasetId + ".json")))
            try (InputStream in = Files.newInputStream(dir.resolve(datasetId + ".json"))) {
                return objectMapper.readValue(in, DatasetDto.class);
            }

        Resource resource = new PathMatchingResourcePatternResolver().getResource(LOCATION + datasetId + ".json");
        if (!resource.exists())
            throw new ItemNotFoundException(DatasetDto.class, datasetId);
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, DatasetDto.class);
        }
    }

    // the folder of the server's datasets, when it is configured and exists
    private Path serverDir() {
        if (datasetsDir == null || datasetsDir.isBlank())
            return null;
        Path dir = Path.of(datasetsDir);
        if (!Files.isDirectory(dir)) {
            logger.warn("databucket.datasets-dir is not a folder: {}", dir.toAbsolutePath());
            return null;
        }
        return dir;
    }

    // the *.json files of the server folder whose names can be dataset ids
    private List<Path> serverFiles() throws IOException {
        Path dir = serverDir();
        if (dir == null)
            return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().matches("[a-zA-Z0-9_-]+\\.json"))
                    .sorted()
                    .toList();
        }
    }

    private static SecurityContext contextOfProject(SecurityContext original, int projectId) {
        CustomUserDetails current = (CustomUserDetails) original.getAuthentication().getPrincipal();
        CustomUserDetails details = new CustomUserDetails(current.getUsername(), "", current.getAuthorities(),
                current.isEnabled(), current.isSuperUser());
        details.setProjectId(projectId);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(details, "", current.getAuthorities()));
        return context;
    }
}
