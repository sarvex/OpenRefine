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

package org.openrefine.operations.row;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.GridPreservation;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Grid;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.EngineDependentChange;
import org.openrefine.operations.EngineDependentOperation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RowRemovalOperation extends EngineDependentOperation {

    @JsonCreator
    public RowRemovalOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig) {
        super(engineConfig);
    }

    @Override
    public String getDescription() {
        return "Remove rows";
    }

    @Override
    public Change createChange() {
        return new RowRemovalChange(_engineConfig);
    }

    public class RowRemovalChange extends EngineDependentChange {

        public RowRemovalChange(EngineConfig engineConfig) {
            super(engineConfig);
        }

        @Override
        public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
            Engine engine = getEngine(projectState);
            Grid result;
            if (Mode.RowBased.equals(engine.getMode())) {
                result = projectState.removeRows(engine.combinedRowFilters());
            } else {
                result = projectState.removeRecords(engine.combinedRecordFilters());
            }
            return new ChangeResult(result, GridPreservation.NO_ROW_PRESERVATION);
        }

        @Override
        public boolean isImmediate() {
            return true;
        }

    }

}
