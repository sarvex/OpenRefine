/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.importers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingUtilities;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Row;
import org.openrefine.util.TrackingInputStream;

import scala.Tuple2;

public class ImporterUtilities {

    static public Serializable parseCellValue(String text) {
        if (text.length() > 0) {
            String text2 = text.trim();
            if (text2.length() > 0) {
                try {
                    return Long.parseLong(text2);
                } catch (NumberFormatException e) {
                }
    
                try {
                    double d = Double.parseDouble(text2);
                    if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                        return d;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return text;
    }

    static public int getIntegerOption(String name, Properties options, int def) {
        int value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            if (s != null) {
                try {
                    value = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                }
            }
        }
        return value;
    }

    static public boolean getBooleanOption(String name, Properties options, boolean def) {
        boolean value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            if (s != null) {
                value = "on".equalsIgnoreCase(s) || "1".equals(s) || Boolean.parseBoolean(s);
            }
        }
        return value;
    }

    static public void appendColumnName(List<String> columnNames, int index, String name) {
        name = name.trim();

        while (columnNames.size() <= index) {
            columnNames.add("");
        }

        if (!name.isEmpty()) {
            String oldName = columnNames.get(index);
            if (!oldName.isEmpty()) {
                name = oldName + " " + name;
            }

            columnNames.set(index, name);
        }
    }

    static public void ensureColumnsInRowExist(List<String> columnNames, Row row) {
        int count = row.cells.size();
        while (count > columnNames.size()) {
            columnNames.add("");
        }
    }

    static public ColumnModel setupColumns(List<String> columnNames) {
        Map<String, Integer> nameToIndex = new HashMap<String, Integer>();
        for (int c = 0; c < columnNames.size(); c++) {
            String cell = columnNames.get(c).trim();
            if (cell.isEmpty()) {
                cell = "Column";
            } else if (cell.startsWith("\"") && cell.endsWith("\"")) {
                // FIXME: is trimming quotation marks appropriate?
                cell = cell.substring(1, cell.length() - 1).trim();
            }
            
            if (nameToIndex.containsKey(cell)) {
                int index = nameToIndex.get(cell);
                nameToIndex.put(cell, index + 1);

                cell = cell.contains(" ") ? (cell + " " + index) : (cell + index);
            } else {
                nameToIndex.put(cell, 2);
            }
            
            columnNames.set(c, cell);
        }
        
        return new ColumnModel(columnNames.stream().map(name -> new ColumnMetadata(name)).collect(Collectors.toList()));
        
    }
    
    static public interface MultiFileReadingProgress {
        public void startFile(String fileSource);
        public void readingFile(String fileSource, long bytesRead);
        public void endFile(String fileSource, long bytesRead);
    }
    
    static public MultiFileReadingProgress createMultiFileReadingProgress(
            final ImportingJob job, List<ImportingFileRecord> fileRecords) {
        long totalSize = 0;
        for (ImportingFileRecord fileRecord : fileRecords) {
            File file = fileRecord.getFile(job.getRawDataDir());
            totalSize += file.length();
        }
        
        final long totalSize2 = totalSize;
        return new MultiFileReadingProgress() {
            long totalBytesRead = 0;
            
            void setProgress(String fileSource, long bytesRead) {
                job.setProgress(totalSize2 == 0 ? -1 : (int) (100 * (totalBytesRead + bytesRead) / totalSize2),
                    "Reading " + fileSource);
            }
            
            @Override
            public void startFile(String fileSource) {
                setProgress(fileSource, 0);
            }

            @Override
            public void readingFile(String fileSource, long bytesRead) {
                setProgress(fileSource, bytesRead);
            }

            @Override
            public void endFile(String fileSource, long bytesRead) {
                totalBytesRead += bytesRead;
            }
        };
    }
    
    static public InputStream openAndTrackFile(
            final String fileSource,
            final File file,
            final MultiFileReadingProgress progress) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(file);
        return progress == null ? inputStream : new TrackingInputStream(inputStream) {
            @Override
            protected long track(long bytesRead) {
                long l = super.track(bytesRead);
                
                progress.readingFile(fileSource, this.bytesRead);
                
                return l;
            }
        };
    }

    /**
     * Adds columns to a column model if it misses one column
     * to store a cell at a given index.
     * 
     * @param columnModel
     * @param c
     * @return
     */
	public static ColumnModel expandColumnModelIfNeeded(ColumnModel columnModel, int c) {
		List<ColumnMetadata> columns = columnModel.getColumns();
		if(c < columns.size()) {
			return columnModel;
		} else if (c == columns.size()){
			List<ColumnMetadata> newColumns = new LinkedList<>(columns);
			String prefix = "Column ";
            int i = c + 1;
            while (true) {
                String columnName = prefix + i;
                ColumnMetadata column = columnModel.getColumnByName(columnName);
                if (column != null) {
                	i++;
                } else {
                    column = new ColumnMetadata(columnName);
                    newColumns.add(column);
                    return new ColumnModel(newColumns);
                }
            }
		} else {
			throw new IllegalStateException(String.format("Column model has too few columns to store cell at index %d", c));
		}
	}
	
	/**
	 * Turns an array of rows into a Spark RDD suitable for a GridState.
	 * 
	 * @param context
	 *     the Spark context used to created the RDD
	 * @param cells
	 *     the list of rows to turn into a RDD
	 * @return
	 *      a RDD indexed by row ids
	 */
    public static JavaPairRDD<Long, Row> createRowRDD(JavaSparkContext context, List<Row> rows) {
    	List<Tuple2<Long,Row>> rdd = new ArrayList<>(rows.size());
    	for (int i = 0; i != rows.size(); i++) {
    		rdd.add(new Tuple2<Long,Row>((long)i, rows.get(i)));
    	}
		return context.parallelize(rdd)
				.keyBy(t -> (Long)t._1)
				.mapValues(t -> t._2);
    }
}
