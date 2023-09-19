package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.List;

import org.openrefine.RefineTest;
import org.openrefine.history.Change;
import org.openrefine.history.Change.DoesNotApplyException;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class MultiValuedCellJoinChangeTests extends RefineTest {
	
	GridState initialState;
	
	@BeforeTest
	public void setUpGrid() {
		initialState = createGrid(
				new String[] { "key", "foo", "bar" },
				new Serializable[][] {
			{ "record1", "a", "b" },
			{ null,      "c", "d" },
			{ "record2", "", "f" },
			{ null,      "g", ""  },
			{ null,      null, null }
		});
	}
	
	@Test(expectedExceptions = DoesNotApplyException.class)
	public void testInvalidColumn() throws DoesNotApplyException {
		Change SUT = new MultiValuedCellJoinChange("does_not_exist", ",");
		SUT.apply(initialState);
	}
	
	@Test
	public void testJoin() throws DoesNotApplyException {
		Change SUT = new MultiValuedCellJoinChange("foo", ",");
		GridState state = SUT.apply(initialState);
		
		GridState expected = createGrid(new String[] { "key", "foo", "bar" },
				new Serializable[][] {
			{ "record1", "a,c", "b" },
			{ null,      null, "d" },
			{ "record2", "g", "f" },
			{ null,      null, null } // this line is a record on its own, so it is preserved
		});
		
		Assert.assertEquals(state.getColumnModel(), initialState.getColumnModel());
		List<IndexedRow> rows = state.collectRows();
		List<IndexedRow> expectedRows = expected.collectRows();
		Assert.assertEquals(rows, expectedRows);
	}

}
