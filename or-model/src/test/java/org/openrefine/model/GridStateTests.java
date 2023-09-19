package org.openrefine.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaPairRDD;
import org.mockito.Mockito;
import org.openrefine.SparkBasedTest;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.overlay.OverlayModelResolver;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import scala.Tuple2;

public class GridStateTests extends SparkBasedTest {
	
	protected static class MyOverlayModel implements OverlayModel {
		@JsonProperty("foo")
		String myProperty = "bar";
		
		public boolean equals(Object other) {
			return other instanceof MyOverlayModel;
		}
	}
	
	protected GridState state;
	
	@BeforeMethod
	public void createGrid() {
		// Create sample grid
		JavaPairRDD<Long, Row> grid = rowRDD(new Serializable[][] {
			{ 1, 2, "3" },
			{ 4, "5", true }
		});
		// and a column model
		ColumnModel cm = new ColumnModel(
				Arrays.asList(
				new ColumnMetadata("a"),
				new ColumnMetadata("b"),
				new ColumnMetadata("c")));
		
		OverlayModelResolver.registerOverlayModel("mymodel", MyOverlayModel.class);
		state = new GridState(cm, grid, Collections.singletonMap("mymodel", new MyOverlayModel()));
	}
	
	@Test
	public void testSize() {
		Assert.assertEquals(state.size(), 2);
	}
	
	@Test
	public void testToString() {
		Assert.assertEquals(state.toString(), "[GridState, 3 columns, 2 rows]");
	}
	
	@Test
	public void testGetGrid() {
		JavaPairRDD<Long, Row> grid = state.getGrid();
		Row row1 = grid.lookup(0L).get(0);
		Assert.assertEquals(row1.getCellValue(0), 1);
		Assert.assertEquals(row1.getCellValue(1), 2);
		Assert.assertEquals(row1.getCellValue(2), "3");
		Row row2 = grid.lookup(1L).get(0);
		Assert.assertEquals(row2.getCellValue(0), 4);
		Assert.assertEquals(row2.getCellValue(1), "5");
		Assert.assertEquals(row2.getCellValue(2), true);
	}
	
	@Test
	public void testSaveAndLoad() throws IOException {
		File tempFile = TestUtils.createTempDirectory("testgrid");
		state.saveToFile(tempFile);
		String tempFilePath = tempFile.getAbsolutePath();
		
		GridState loaded = GridState.loadFromFile(context(), tempFile);
		
		Assert.assertEquals(loaded.getOverlayModels(), state.getOverlayModels());
		List<Tuple2<Long, Row>> loadedGrid = loaded.getGrid().collect();
        Assert.assertEquals(loadedGrid, state.getGrid().collect());
	}

	@Test
	public void testUnion() {
		List<Tuple2<Long,Row>> list = Arrays.asList(
				new Tuple2<Long,Row>(4L, new Row(
						Arrays.asList(new Cell(134, null), new Cell(24, null), new Cell("34", null) ))));
		JavaPairRDD<Long,Row> rdd = context().parallelize(list)
				.keyBy(t -> (Long)t._1)
				.mapValues(t -> t._2);
		GridState other = new GridState(state.getColumnModel(), rdd, state.getOverlayModels());
		
		GridState union = state.union(other);
		
		Assert.assertEquals(union.size(), 3);
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testUnionMismatchingColumnModels() {
		List<Tuple2<Long,Row>> list = Arrays.asList(
				new Tuple2<Long,Row>(4L, new Row(
						Arrays.asList(new Cell(134, null)))));
		JavaPairRDD<Long,Row> rdd = context().parallelize(list)
				.keyBy(t -> (Long)t._1)
				.mapValues(t -> t._2);
		
		ColumnModel otherModel = new ColumnModel(Arrays.asList(new ColumnMetadata("some column")));
		
		GridState other = new GridState(otherModel, rdd, state.getOverlayModels());
		
		state.union(other);
	}
	
	@Test
	public void testLimitPartitions() {
	    List<Tuple2<Long,Row>> list = new ArrayList<>();
	    for(int i = 0; i < 10; i++) {
	        list.add(new Tuple2<Long,Row>((long)i, new Row(
                    Arrays.asList(new Cell(i, null) ))));
	    }
        JavaPairRDD<Long,Row> rdd = context().parallelize(list, 2)
                .keyBy(t -> (Long)t._1)
                .mapValues(t -> t._2);
        
        JavaPairRDD<Long,Row> capped = GridState.limitPartitions(rdd, 3);
        List<Tuple2<Long,Row>> rows = capped.collect();
        Assert.assertEquals(rows.size(), 6);
        Assert.assertEquals(rows.stream().map(t -> t._1).collect(Collectors.toList()),
                Arrays.asList(0L, 1L, 2L, 5L, 6L, 7L));
	}
	
	@Test
	public void testGetRows() {
        List<Tuple2<Long,Row>> rows = new ArrayList<>();
        rows.add(new Tuple2<Long,Row>(0L, new Row(Arrays.asList(new Cell(1, null), new Cell(2, null), new Cell("3", null)))));
        rows.add(new Tuple2<Long,Row>(1L, new Row(Arrays.asList(new Cell(4, null), new Cell("5", null), new Cell(true, null)))));
	    
	    Assert.assertEquals(state.getRows(0, 2), rows);
	    Assert.assertEquals(state.getRows(1, 2), rows.subList(1, 2));
	    Assert.assertEquals(state.getRows(5, 5), Collections.emptyList());
	}
}