/**

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

package org.openrefine.browsing;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.browsing.facets.AllFacetsAggregator;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.browsing.facets.RecordAggregator;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Faceted browsing engine.
 * Given a GridState and facet configurations,
 * it can be used to compute facet statistics and
 * obtain a filtered view of the grid according to the facets.
 */
public class Engine  {
    static public enum Mode {
        @JsonProperty(MODE_ROW_BASED)
        RowBased,
        @JsonProperty(MODE_RECORD_BASED)
        RecordBased
    }

    public final static String INCLUDE_DEPENDENT = "includeDependent";
    public final static String MODE = "mode";
    public final static String MODE_ROW_BASED = "row-based";
    public final static String MODE_RECORD_BASED = "record-based";

    protected final GridState _state;
    protected final List<Facet> _facets;
    protected final EngineConfig _config;


    static public String modeToString(Mode mode) {
        return mode == Mode.RowBased ? MODE_ROW_BASED : MODE_RECORD_BASED;
    }
    static public Mode stringToMode(String s) {
        return MODE_ROW_BASED.equals(s) ? Mode.RowBased : Mode.RecordBased;
    }

    public Engine(GridState state, EngineConfig config) {
        _state  = state;
        _config = config;
        _facets = config.getFacetConfigs().stream()
        		.map(fc -> fc.apply(state.getColumnModel()))
        		.collect(Collectors.toList());
        
    }

    @JsonProperty("engine-mode")
    public Mode getMode() {
        return _config.getMode();
    }

    @JsonIgnore
    public GridState getGridState() {
    	return _state;
    }
    
    @JsonIgnore
    public EngineConfig getConfig() {
        return _config;
    }

    @JsonProperty("facets")
    public ImmutableList<FacetResult> getFacetResults() {
        ImmutableList<FacetState> states = _state.aggregateRows(allFacetsAggregator(), allFacetsInitialState());
        
        Builder<FacetResult> facetResults = ImmutableList.<FacetResult>builder();
        for(int i = 0; i != states.size(); i++) {
           facetResults.add(_facets.get(i).getFacetResult(states.get(i))); 
        }
        return facetResults.build();
    }
    
    @JsonIgnore
    public Iterable<IndexedRow> getMatchingRows() {
        if (Mode.RowBased.equals(getMode())) {
            return _state.iterateRows(combinedRowFilters());
        } else {
            Iterable<Record> records = _state.iterateRecords(combinedRecordFilters());
            return new Iterable<IndexedRow>() {

                @Override
                public Iterator<IndexedRow> iterator() {
                    Iterator<Record> recordIterator = records.iterator();
                    return new Iterator<IndexedRow>() {
                        
                        private Iterator<IndexedRow> currentRecord = null;

                        @Override
                        public boolean hasNext() {
                            return (currentRecord != null && currentRecord.hasNext()) || recordIterator.hasNext();
                        }

                        @Override
                        public IndexedRow next() {
                            if (currentRecord == null || !currentRecord.hasNext()) {
                                currentRecord = recordIterator.next().getIndexedRows().iterator();
                            }
                            return currentRecord.next();
                        }
                        
                    };
                }
                
            };
        }
    }
    
    /**
     * @return a row filter obtained from all applied facets
     */
    @JsonIgnore
    public RowFilter combinedRowFilters() {
        return RowFilter.conjunction(facetRowFilters());
    }
    
    /**
     * @return a record filter obtained from all applied facets
     */
    @JsonIgnore
    public RecordFilter combinedRecordFilters() {
        return RecordFilter.conjunction(facetRecordFilters());
    }
    
    /**
     * Runs an aggregator only on the rows that are selected by facets.
     * 
     * @param <T>
     * @param aggregator
     * @param initialState
     * @return
     */
    public <T extends Serializable> T aggregateFilteredRows(RowAggregator<T> aggregator, T initialState) {
        return _state.aggregateRows(restrictAggregator(aggregator, combinedRowFilters()), initialState);
    }
    
    /**
     * Runs an aggregator only on the records that are selected by facets.
     * 
     * @param <T>
     * @param aggregator
     * @param initialState
     * @return
     */
    public <T extends Serializable> T aggregateFilteredRecords(RecordAggregator<T> aggregator, T initialState) {
        return _state.aggregateRecords(restrictAggregator(aggregator, combinedRecordFilters()), initialState);
    }
    
    @JsonIgnore
	private List<RowFilter> facetRowFilters() {
		return _facets.stream()
        		.map(facet -> facet.getAggregator())
        		.map(aggregator -> aggregator == null ? null : aggregator.getRowFilter())
        		.filter(filter -> filter != null)
        		.collect(Collectors.toList());
	}
    
    @JsonIgnore
    private List<RecordFilter> facetRecordFilters() {
        return _facets.stream()
                .map(facet -> facet.getAggregator())
                .map(aggregator -> aggregator == null ? null : aggregator.getRecordFilter())
                .filter(filter -> filter != null)
                .collect(Collectors.toList());
    }
    
    @JsonIgnore
	private ImmutableList<FacetState> allFacetsInitialState() {
		return ImmutableList.copyOf(
		        _facets
    			.stream().map(facet -> facet.getInitialFacetState())
    			.collect(Collectors.toList()));
	}
    
    @JsonIgnore
    private AllFacetsAggregator allFacetsAggregator() {
        return new AllFacetsAggregator(_facets
                .stream().map(facet -> facet.getAggregator())
                .collect(Collectors.toList()));
    }
    
    private static <T> RowAggregator<T> restrictAggregator(RowAggregator<T> aggregator, RowFilter filter) {
        return new RowAggregator<T>() {

            private static final long serialVersionUID = 8407224640500910094L;

            @Override
            public T sum(T first, T second) {
                return aggregator.sum(first, second);
            }

            @Override
            public T withRow(T state, long rowId, Row row) {
                if (filter.filterRow(rowId, row)) {
                    return aggregator.withRow(state, rowId, row);
                } else {
                    return state;
                }
            }
            
        };
    }
    
    private static <T> RecordAggregator<T> restrictAggregator(RecordAggregator<T> aggregator, RecordFilter filter) {
        return new RecordAggregator<T>() {

            private static final long serialVersionUID = 8407224640500910094L;

            @Override
            public T sum(T first, T second) {
                return aggregator.sum(first, second);
            }

            @Override
            public T withRecord(T state, Record record) {
                if (filter.filterRecord(record)) {
                    return aggregator.withRecord(state, record);
                } else {
                    return state;
                }
            }
            
        };
    }
}
