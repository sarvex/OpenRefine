
package org.openrefine.model.local.util.logging;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.process.ProgressReporter;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class LoggedChangeData<T> implements ChangeData<T> {

    protected final ChangeData<T> changeData;
    protected final LoggedDatamodelRunner runner;

    public LoggedChangeData(LoggedDatamodelRunner runner, ChangeData<T> changeData) {
        this.runner = runner;
        this.changeData = changeData;
    }

    @Override
    public T get(long rowId) {
        return runner.exec("get", () -> changeData.get(rowId));
    }

    @Override
    public DatamodelRunner getDatamodelRunner() {
        return runner;
    }

    @Override
    public void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException, InterruptedException {
        runner.exec("saveToFile", () -> {
            try {
                changeData.saveToFile(file, serializer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void saveToFile(File file, ChangeDataSerializer<T> serializer, ProgressReporter progressReporter)
            throws IOException, InterruptedException {
        runner.exec("saveToFile", () -> {
            try {
                changeData.saveToFile(file, serializer, progressReporter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Iterator<IndexedData<T>> iterator() {
        return runner.exec("iterator", () -> changeData.iterator());
    }
}
