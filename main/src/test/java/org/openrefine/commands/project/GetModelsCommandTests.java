
package org.openrefine.commands.project;

import com.fasterxml.jackson.databind.JsonNode;
import org.openrefine.commands.CommandTestBase;
import org.openrefine.commands.row.GetRowsCommand;
import org.openrefine.model.Project;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;

import static org.mockito.Mockito.when;

public class GetModelsCommandTests extends CommandTestBase {

    Project project;

    @BeforeMethod
    public void setUp() {
        command = new GetModelsCommand();
        project = createProject(new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { null, "c" },
                        { "d", "e" },
                        { "", "f" },
                        { "g", "h" }
                });

        when(request.getParameter("project")).thenReturn(String.valueOf(project.getId()));
    }

    @Test
    public void testCommand() throws ServletException, IOException {
        String expectedJson = ParsingUtilities.mapper.writeValueAsString(project.getColumnModel());
        command.doGet(request, response);

        JsonNode parsedResponse = ParsingUtilities.mapper.readTree(writer.toString());
        TestUtils.assertEqualsAsJson(parsedResponse.get("columnModel").toString(), expectedJson);
    }
}
