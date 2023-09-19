package org.openrefine.browsing.facets;

import java.util.Arrays;
import java.util.List;

import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class AllFacetsAggregatorTests {
	
	protected RowFilter filterRowId = new RowFilter() {
	    private static final long serialVersionUID = 1L;

		@Override
		public boolean filterRow(long rowIndex, Row row) {
			return rowIndex < 2;
		}
	};
	protected RowFilter filterFoo = new RowFilter() {
	    private static final long serialVersionUID = 1L;

		@Override
		public boolean filterRow(long rowIndex, Row row) {
			return "foo".equals(row.getCellValue(0));
		}
	};
	protected RowFilter filterBar = new RowFilter() {
        private static final long serialVersionUID = 1L;

        @Override
		public boolean filterRow(long rowIndex, Row row) {
			return "bar".equals(row.getCellValue(1));
		}
	};
	
	protected AllFacetsAggregator SUT;
	protected FacetAggregator<FacetStateStub> aggregatorRowId;
	protected FacetAggregator<FacetStateStub> aggregatorFoo;
	protected FacetAggregator<FacetStateStub> aggregatorBar;
    private ImmutableList<FacetState> initial;
	
	@BeforeMethod
	public void setUpAllFacetsState() {
	    aggregatorRowId = new FacetAggregatorStub(filterRowId);
	    aggregatorFoo = new FacetAggregatorStub(filterFoo);
	    aggregatorBar = new FacetAggregatorStub(filterBar);
		SUT = new AllFacetsAggregator(
				Arrays.asList(
				aggregatorRowId,
				aggregatorFoo,
				aggregatorBar));
		initial = ImmutableList.of(
                new FacetStateStub(0, 0),
                new FacetStateStub(0, 0),
                new FacetStateStub(0, 0));
	}

	@Test
	public void testMerge() {
		ImmutableList<FacetState> statesA = 
				ImmutableList.of(
				new FacetStateStub(1, 2),
				new FacetStateStub(3, 4),
				new FacetStateStub(5, 6));
		ImmutableList<FacetState> statesB = 
				ImmutableList.of(
				new FacetStateStub(7, 8),
				new FacetStateStub(9, 10),
				new FacetStateStub(11, 12));
		ImmutableList<FacetState> expected = 
				ImmutableList.of(
				new FacetStateStub(8, 10),
				new FacetStateStub(12, 14),
				new FacetStateStub(16, 18));
		Assert.assertEquals(SUT.sum(statesA, statesB), expected);
	}
	
	@Test
	public void testIncrementAllMatching() {
		Row row = new Row(Arrays.asList(
				new Cell("foo", null), new Cell("bar", null)));
		
        List<FacetState> result = SUT.withRow(initial, 1, row);
		Assert.assertEquals(result, Arrays.asList(
				new FacetStateStub(1, 0),
				new FacetStateStub(1, 0),
				new FacetStateStub(1, 0)));
	}
	
	@Test
	public void testIncrementAllButOneMatching() {
		Row row = new Row(Arrays.asList(
				new Cell("wrong", null), new Cell("bar", null)));
		
        List<FacetState>  result = SUT.withRow(initial, 1, row);
		Assert.assertEquals(result, Arrays.asList(
				new FacetStateStub(0, 0),
				new FacetStateStub(0, 1),
				new FacetStateStub(0, 0)));
	}
	
	@Test
	public void testIncrementTwoMismatching() {
		Row row = new Row(Arrays.asList(
				new Cell("wrong", null), new Cell("fail", null)));
		
		List<FacetState> result = SUT.withRow(initial, 1, row);
		Assert.assertEquals(result, initial);
	}
	
	@Test
	public void testIncrementAllMismatching() {
		Row row = new Row(Arrays.asList(
				new Cell("wrong", null), new Cell("fail", null)));
		
		List<FacetState> result = SUT.withRow(initial, 43, row);
		Assert.assertEquals(result, initial);
	}
	
	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEquals() {
		Assert.assertFalse(SUT.equals(34));
	}
}
