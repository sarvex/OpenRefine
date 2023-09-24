
package org.openrefine.wikibase.exporters;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Properties;

import org.openrefine.ProjectMetadata;
import org.openrefine.RefineTest;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Grid;
import org.openrefine.util.TestUtils;
import org.testng.annotations.Test;

public class SchemaExporterTest extends RefineTest {

    private SchemaExporter exporter = new SchemaExporter();

    @Test
    public void testNoSchema()
            throws IOException {
        // TODO instead of returning an empty (and invalid) schema, we should just return an error
        Grid grid = this.createGrid(
                new String[] { "a", "b" },
                new Serializable[][] { { "c", "d" } });
        Engine engine = new Engine(grid, EngineConfig.ALL_ROWS, 1234L);

        StringWriter writer = new StringWriter();
        Properties properties = new Properties();
        exporter.export(grid, new ProjectMetadata(), 1234L, properties, engine, writer);
        TestUtils.assertEqualsAsJson(writer.toString(),
                "{\"entityEdits\":[],\"siteIri\":null,\"mediaWikiApiEndpoint\":null,\"entityTypeSiteIRI\":{}}");
    }

}
