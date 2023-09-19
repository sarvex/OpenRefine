package org.openrefine.exporters;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.spark.Partition;
import org.openrefine.browsing.Engine;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import scala.Tuple2;

/**
 * Base class for a tabular exporter which respects the facets applied
 * to the grid.
 * 
 * @author Antonin Delpeuch
 */
public abstract class EngineDependentExporter implements WriterExporter {
	
    static public class CellData {
        final public String columnName;
        final public Object value;
        final public String text;
        final public String link;
        
        public CellData(String columnName, Object value, String text, String link) {
            this.columnName = columnName;
            this.value = value;
            this.text = text;
            this.link = link;
        }
    }
    
    public abstract void startFile(JsonNode options, Properties params, ColumnModel columnModel, Writer writer);
    
    public abstract void endFile() throws IOException;
    
    public abstract void addRow(List<CellData> cells, boolean isHeader);

	@Override
	public void export(GridState grid, Properties options, Engine engine, Writer writer) throws IOException {
    
	    String optionsString = (options != null) ? options.getProperty("options") : null;
	    JsonNode optionsTemp = null;
	    if (optionsString != null) {
	        try {
	            optionsTemp = ParsingUtilities.mapper.readTree(optionsString);
	        } catch (IOException e) {
	            // Ignore and keep options null.
	        }
	    }
	    final JsonNode jsonOptions = optionsTemp;
	    
	    final boolean outputColumnHeaders = jsonOptions == null ? true :
	        JSONUtilities.getBoolean(jsonOptions, "outputColumnHeaders", true);
	    final boolean outputEmptyRows = jsonOptions == null ? false :
	        JSONUtilities.getBoolean(jsonOptions, "outputBlankRows", true);
	    final int limit = jsonOptions == null ? -1 :
	        JSONUtilities.getInt(jsonOptions, "limit", -1);
	    
	    final List<String> columnNames;
	    final Map<String, CustomizableTabularExporterUtilities.CellFormatter> columnNameToFormatter =
	        new HashMap<String, CustomizableTabularExporterUtilities.CellFormatter>();
	    
	    List<JsonNode> columnOptionArray = jsonOptions == null ? null :
	        JSONUtilities.getArray(jsonOptions, "columns");
	    if (columnOptionArray == null) {
	        List<ColumnMetadata> columns = grid.getColumnModel().getColumns();
	        
	        columnNames = new ArrayList<String>(columns.size());
	        for (ColumnMetadata column : columns) {
	            String name = column.getName();
	            columnNames.add(name);
	            columnNameToFormatter.put(name, new CustomizableTabularExporterUtilities.CellFormatter());
	        }
	    } else {
	        int count = columnOptionArray.size();
	        
	        columnNames = new ArrayList<String>(count);
	        for (int i = 0; i < count; i++) {
	            JsonNode columnOptions = columnOptionArray.get(i);
	            if (columnOptions != null) {
	                String name = JSONUtilities.getString(columnOptions, "name", null);
	                if (name != null) {
	                    columnNames.add(name);
	                    try {
							columnNameToFormatter.put(name, ParsingUtilities.mapper.treeToValue(columnOptions, CustomizableTabularExporterUtilities.ColumnOptions.class));
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
	                }
	            }
	        }
	    }
	    
	    
	    GridState filtered = engine.getMatchingRows();
	    ColumnModel columnModel = grid.getColumnModel();
	    /*
	     * Only load project data partition by partition to save memory.
	     * We do not use Spark's foreach method here as it would not preserve
	     * row order and would run the exporter on the executors, where the
	     * file to export might not be accessible.
	     */
	    List<Partition> partitions = filtered.getGrid().partitions();
	    
	    startFile(jsonOptions, options, columnModel, writer);
	    if (outputColumnHeaders) {
            List<CellData> cells = new ArrayList<CellData>(columnNames.size());
            for (String name : columnNames) {
                cells.add(new CellData(name, name, name, null));
            }
            addRow(cells, true);
        }
	    
	    long rowCount = 0;
	    for(Partition partition : partitions) {
	    	List<Tuple2<Long, Row>> rows = filtered.getGrid().collectPartitions(new int[] { partition.index() })[0];
	    	for(Tuple2<Long,Row> rowTuple : rows) {
	    		Row row = rowTuple._2;
	    		
	            List<CellData> cells = new ArrayList<CellData>(columnNames.size());
	            int nonNullCount = 0;
	            
	            for (int cellIndex = 0; cellIndex < columnModel.getColumns().size(); cellIndex++) {
	                ColumnMetadata column = columnModel.getColumns().get(cellIndex);
	                CustomizableTabularExporterUtilities.CellFormatter formatter = columnNameToFormatter.get(column.getName());
	                CellData cellData = formatter.format(
	                    column,
	                    row.getCell(cellIndex));
	                
	                cells.add(cellData);
	                if (cellData != null) {
	                    nonNullCount++;
	                }
	            }
	            
	            if (nonNullCount > 0 || outputEmptyRows) {
	                addRow(cells, false);
	                rowCount++;
	            }
	            if (limit > 0 && rowCount >= limit) {
	            	break;
	            }
	    	}
	    	if (limit > 0 && rowCount >= limit) {
            	break;
            }
	    }
	    endFile();
	}

}
