
package org.openrefine.model.local.util.logging;

import org.openrefine.model.Runner;
import org.openrefine.model.RunnerTestBase;
import org.openrefine.model.TestingRunner;

import java.io.IOException;

public class LoggedRunnerTests extends RunnerTestBase {

    @Override
    public Runner getDatamodelRunner() throws IOException {
        return new LoggedRunner(new TestingRunner());
    }

}
