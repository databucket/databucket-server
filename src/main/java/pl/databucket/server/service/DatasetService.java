package pl.databucket.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ProjectNotEmptyException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.security.CustomUserDetails;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The datasets shipped with Databucket (src/main/resources/datasets/*.json, e.g. DEMO), their import into an
 * empty project and the export of a project as a dataset, from the super-admin area.
 */
@Service
public class DatasetService {

    private static final String LOCATION = "classpath:datasets/";
    private final Logger logger = LoggerFactory.getLogger(DatasetService.class);

    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectContentService projectContentService;
    @Autowired private UserService userService;
    @Autowired private DatasetImporter datasetImporter;
    @Autowired private DatasetExporter datasetExporter;

    public List<DatasetInfoDto> getDatasets() throws IOException {
        List<DatasetInfoDto> datasets = new ArrayList<>();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources(LOCATION + "*.json")) {
            String id = Objects.requireNonNull(resource.getFilename()).replaceFirst("\\.json$", "");
            try (InputStream in = resource.getInputStream()) {
                DatasetDto dataset = objectMapper.readValue(in, DatasetDto.class);
                int rows = dataset.getData().values().stream().mapToInt(List::size).sum();
                datasets.add(new DatasetInfoDto(id, dataset.getName(), dataset.getDescription(),
                        dataset.getBuckets().size(), dataset.getViews().size(), rows));
            } catch (IOException e) {
                logger.error("The dataset " + resource.getFilename() + " can't be read", e);
            }
        }
        datasets.sort(Comparator.comparing(DatasetInfoDto::getName));
        return datasets;
    }

    /**
     * Imports the dataset into the project, which must be empty. Data rows are created by the current user (a
     * reserved row is reserved for them), who also joins the teams of the dataset.
     */
    public int importDataset(int projectId, String datasetId) throws Exception {
        Project project = projectRepository.findByIdAndDeleted(projectId, false);
        if (project == null)
            throw new ItemNotFoundException(Project.class, projectId);
        if (!projectContentService.isEmpty(projectId))
            throw new ProjectNotEmptyException(project.getName(), "importing a dataset");

        DatasetDto dataset = readDataset(datasetId);
        User user = userService.getCurrentUser();

        // The project of the security context is the tenant of the sessions opened in it (ProjectTenantResolver):
        // the import runs as the current user, signed in to the target project, and the original context comes back
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(contextOfProject(original, projectId));
            int rows = datasetImporter.importDataset(dataset, user);
            AuditLog.log("project.dataset-import", "id={} name={} dataset={} rows={}",
                    project.getId(), project.getName(), dataset.getName(), rows);
            return rows;
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    /**
     * The content of the project as a dataset file, in the format of the shipped ones, e.g. to build a new dataset
     * in the UI and add it to src/main/resources/datasets. The dataset is named after the project.
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
        // only the shipped files: the id is a file name, never a path
        if (datasetId == null || !datasetId.matches("[a-zA-Z0-9_-]+"))
            throw new ItemNotFoundException(DatasetDto.class, datasetId);
        Resource resource = new PathMatchingResourcePatternResolver().getResource(LOCATION + datasetId + ".json");
        if (!resource.exists())
            throw new ItemNotFoundException(DatasetDto.class, datasetId);
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, DatasetDto.class);
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
