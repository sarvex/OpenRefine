package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function2;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.util.StringUtils;

public class MassEditChange extends EngineDependentChange {
	
	protected final Evaluable                 _evaluable;
	protected final String                    _columnName;
	protected final Map<String, Serializable> _fromTo;
	protected final Serializable              _fromBlankTo;
	protected final Serializable              _fromErrorTo;

	public MassEditChange(
			EngineConfig engineConfig,
			Evaluable evaluable,
			String columnName,
			Map<String, Serializable> fromTo,
			Serializable fromBlankTo,
			Serializable fromErrorTo) {
		super(engineConfig);
		_evaluable = evaluable;
		_columnName = columnName;
		_fromTo = fromTo;
		_fromBlankTo = fromBlankTo;
		_fromErrorTo = fromErrorTo;
	}

	@Override
	public GridState applyToFilteredState(GridState filteredState) {
		
		ColumnModel columnModel = filteredState.getColumnModel();
		int columnIdx = columnModel.getColumnIndexByName(_columnName);
		if (columnIdx == -1) {
			throw new IllegalStateException(String.format("Column name %s not found in column model", _columnName));
		}
		
		Function2<Long, Row, Row> function = mapper(columnIdx, _evaluable, _columnName, _fromTo, _fromBlankTo, _fromErrorTo);
		JavaPairRDD<Long,Row> newRdd = GridState.mapKeyValuesToValues(filteredState.getGrid(), function );
		return new GridState(columnModel, newRdd, filteredState.getOverlayModels());
	}

	private static Function2<Long, Row, Row> mapper(int columnIdx, Evaluable evaluable, String columnName,
			Map<String,Serializable> fromTo, Serializable fromBlankTo, Serializable fromErrorTo) {
		return new Function2<Long, Row, Row>() {

			private static final long serialVersionUID = 1L;

			@Override
			public Row call(Long rowIndex, Row row) throws Exception {
				Cell cell = row.getCell(columnIdx);
                Cell newCell = null;
                
                Properties bindings = ExpressionUtils.createBindings();
                ExpressionUtils.bind(bindings, null, row, rowIndex, columnName, cell);
                
                Object v = evaluable.evaluate(bindings);
                if (ExpressionUtils.isError(v)) {
                    if (fromErrorTo != null) {
                        newCell = new Cell(fromErrorTo, (cell != null) ? cell.recon : null);
                    }
                } else if (ExpressionUtils.isNonBlankData(v)) {
                    String from = StringUtils.toString(v);
                    Serializable to = fromTo.get(from);
                    if (to != null) {
                        newCell = new Cell(to, (cell != null) ? cell.recon : null);
                    }
                } else {
                    if (fromBlankTo != null) {
                        newCell = new Cell(fromBlankTo, (cell != null) ? cell.recon : null);
                    }
                }
                return row.withCell(columnIdx, newCell);
			}
			
		};
	}

}
