
package org.openrefine.commands.history;

import com.fasterxml.jackson.databind.JsonNode;
import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.commands.Command;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.model.Project;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertEquals;

public class CancelProcessCommandTests extends CommandTestBase {

    long projectId = 1234L;
    int processId = 5678;
    int missingProcessId = 9876;
    Project project;
    ProjectMetadata projectMetadata;
    ProcessManager processManager;
    Process process;

    @BeforeMethod
    public void setUpCommand() {
        command = new CancelProcessCommand();
        project = mock(Project.class);
        when(project.getId()).thenReturn(projectId);
        projectMetadata = mock(ProjectMetadata.class);
        when(projectMetadata.getTags()).thenReturn(new String[] {});
        Instant now = Instant.now();
        when(projectMetadata.getModified()).thenReturn(now);
        when(project.getLastSave()).thenReturn(now);
        processManager = mock(ProcessManager.class);
        when(project.getProcessManager()).thenReturn(processManager);
        process = mock(Process.class);
        when(processManager.getProcess(processId)).thenReturn(process);
        when(processManager.getProcess(missingProcessId)).thenThrow(new IllegalArgumentException("missing"));

        ProjectManager.singleton.registerProject(project, projectMetadata);
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }

    @Test
    public void testSuccessfulPause() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(projectId));
        when(request.getParameter("id")).thenReturn(Integer.toString(processId));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        verify(process, times(1)).cancel();
        TestUtils.assertEqualsAsJson(writer.toString(), "{\"code\":\"ok\"}");
    }

    @Test
    public void testProcessNotFound() throws ServletException, IOException {
        when(request.getParameter("project")).thenReturn(Long.toString(projectId));
        when(request.getParameter("id")).thenReturn(Integer.toString(missingProcessId));
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        command.doPost(request, response);

        JsonNode response = ParsingUtilities.mapper.readTree(writer.toString());
        assertEquals(response.get("code").asText(), "error");
    }
}
