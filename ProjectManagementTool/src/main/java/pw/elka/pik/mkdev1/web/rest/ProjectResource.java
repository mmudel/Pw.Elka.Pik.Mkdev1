package pw.elka.pik.mkdev1.web.rest;

import com.codahale.metrics.annotation.Timed;
import pw.elka.pik.mkdev1.domain.Project;
import pw.elka.pik.mkdev1.domain.User;
import pw.elka.pik.mkdev1.repository.ProjectRepository;
import pw.elka.pik.mkdev1.repository.UserRepository;
import pw.elka.pik.mkdev1.service.MailService;
import pw.elka.pik.mkdev1.service.ProjectService;
import pw.elka.pik.mkdev1.web.rest.dto.ManagedUserDTO;
import pw.elka.pik.mkdev1.web.rest.dto.ProjectDTO;
import pw.elka.pik.mkdev1.web.rest.util.HeaderUtil;
import pw.elka.pik.mkdev1.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ProjectResource {

    private final Logger log = LoggerFactory.getLogger(UserResource.class);

    @Inject
    private ProjectService projectService;

    /**
     * GET  /projects : get all projects.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and with body all projects
     * @throws URISyntaxException if the pagination headers couldnt be generated
     */
    @RequestMapping(value = "/projects",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProjectDTO>> getAllProjectsForCurrentUser(Pageable pageable)
        throws URISyntaxException {
        Page<ProjectDTO> page = projectService.getAllProjectsForCurrentUser(pageable);
        List<ProjectDTO> projectDTOs = page.getContent().stream()
            .collect(Collectors.toList());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/projects");
        return new ResponseEntity<>(projectDTOs, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<ProjectDTO> getProject(@PathVariable Long id){
        log.debug("REST request to get Project : {}", id);
        return projectService.getDetailsById(id)
            .map(projectDTO -> new ResponseEntity<>(projectDTO, HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/projects/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteProject(@PathVariable Long id){
        log.debug("REST request to delete Project : {}", id);
        projectService.deleteProject(id);
        return ResponseEntity.ok().headers(HeaderUtil.createAlert( "A project " + id + " deleted", id.toString())).build();
    }

    @RequestMapping(value = "/projects",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<?> createProject(@Validated(ProjectDTO.CreateChecks.class) @RequestBody ProjectDTO projectDTO, HttpServletRequest request) throws URISyntaxException {
        log.debug("REST request to save Project : {}", projectDTO);
        if (projectService.exists(projectDTO.getId())) {
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createFailureAlert("projectManagement", "projectexists", "Name of project already in use"))
                .body(null);
        } else {
            projectService.createProject(projectDTO);
            return ResponseEntity.created(new URI("/api/projects/" + projectDTO.getName()))
                .headers(HeaderUtil.createAlert( "A project is created with identifier " + projectDTO.getName(), projectDTO.getName()))
                .body(projectDTO);
        }
    }

    @RequestMapping(value = "/projects",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Transactional
    public ResponseEntity<ProjectDTO> updateProject(@Validated(ProjectDTO.UpdateChecks.class)@RequestBody ProjectDTO projectDTO) {
        log.debug("REST request to update Project : {}", projectDTO);
        return projectService.modifyProject(projectDTO);
    }

    @RequestMapping(value = "/projects/{id}/{login}",
        method = RequestMethod.DELETE)
    @Timed
    @Transactional
    public ResponseEntity<Void> deleteUserFromProject(@PathVariable Long id, @PathVariable String login){
        log.debug("REST request to delete User from Project : {}", id + ' ' + login);
        projectService.deleteUserFromProject(id, login);
        return ResponseEntity.ok().headers(HeaderUtil.createAlert( "A user " + login + " is deleted from project" + id, login)).build();
    }

    @RequestMapping(value = "/projects/{id}/{login}",
        method = RequestMethod.POST)
    @Timed
    @Transactional
    public ResponseEntity<Void> addUserToProject(@PathVariable Long id, @PathVariable String login){
        log.debug("REST request to add User to Project : {}", id + ' ' + login);
        projectService.addUserToProject(id, login);
        return ResponseEntity.ok().headers(HeaderUtil.createAlert( "A user " + login + " is added to project" + id, login)).build();
    }

}
