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

package org.openrefine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.Change;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;

public class RowReorderChange implements Change {
    final protected List<Integer> _rowIndices;
    
    public RowReorderChange(List<Integer> rowIndices) {
    	super(EngineConfig.ALL_ROWS);
        _rowIndices = rowIndices;
    }
    
	@Override
	public boolean isImmediate() {
		return true;
	}

   
    @Override
    public void apply(Project project) {
        synchronized (project) {
            List<Row> oldRows = project.rows;
            List<Row> newRows = new ArrayList<Row>(oldRows.size());

            for (Integer oldIndex : _rowIndices) {
                newRows.add(oldRows.get(oldIndex));
            }

            project.rows.clear();
            project.rows.addAll(newRows);
            project.update();
        }
    }



}
