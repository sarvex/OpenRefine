
package org.openrefine.runners.local;

import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.runners.local.pll.Tuple2;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrefine.runners.local.LocalGrid.applyRowChangeDataMapperWithIncompleteData;

public class LocalGridTests {

    @Test
    public void testBatchFetchWithNull() {

        RowChangeDataProducer<String> rowMapper = new RowChangeDataProducer<String>() {

            @Override
            public String call(long rowId, Row row) {
                return null;
            }
        };
        List<Tuple2<Long, Tuple2<IndexedRow, String>>> batch = Arrays.asList(
                Tuple2.of(3L, Tuple2.of(new IndexedRow(3L, new Row(Collections.singletonList(null))), "foo")),
                Tuple2.of(4L, Tuple2.of(new IndexedRow(4L, new Row(Collections.singletonList(null))), null)));
        List<Tuple2<Long, String>> expected = Arrays.asList(
                Tuple2.of(3L, "foo"),
                Tuple2.of(4L, null));

        Stream<Tuple2<Long, String>> result = applyRowChangeDataMapperWithIncompleteData(rowMapper, batch);

        List<Tuple2<Long, String>> collected = result.collect(Collectors.toList());
        Assert.assertEquals(collected, expected);
    }
}
