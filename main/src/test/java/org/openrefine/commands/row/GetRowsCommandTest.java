/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.openrefine.commands.row;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrefine.RefineTest;
import org.openrefine.commands.Command;
import org.openrefine.model.Project;
import org.openrefine.util.TestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetRowsCommandTest extends RefineTest {
    
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    Command command = null;
    Project project = null;
    Project longerProject = null;
    StringWriter writer = null;
    
    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        project = createProject(new String[] {"foo", "bar"},
        		new Serializable[] {
        			"a", "b",
        			null, "c",
        			"d", "e",
        			"", "f",
        			"g", "h"
        		});
        command = new GetRowsCommand();
        writer = new StringWriter();
        when(request.getParameter("project")).thenReturn(String.valueOf(project.getId()));
        try {
            when(response.getWriter()).thenReturn(new PrintWriter(writer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testJsonOutputRows() throws ServletException, IOException {
        String rowJson = "{\n" + 
                "       \"filtered\" : 5,\n" + 
                "       \"limit\" : 2,\n" + 
                "       \"mode\" : \"row-based\",\n" + 
                "       \"rows\" : [ {\n" + 
                "         \"cells\" : [ {\n" + 
                "           \"v\" : \"a\"\n" + 
                "         }, {\n" + 
                "           \"v\" : \"b\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 0,\n" + 
                "         \"starred\" : false\n" + 
                "       }, {\n" + 
                "         \"cells\" : [ null, {\n" + 
                "           \"v\" : \"c\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 1,\n" + 
                "         \"starred\" : false\n" + 
                "       } ],\n" + 
                "       \"start\" : 0,\n" + 
                "       \"total\" : 5,\n" +
                "       \"processed\": 5\n" + 
                "     }";
        
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[]}");
        when(request.getParameter("limit")).thenReturn("2");
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(rowJson, writer.toString());
    }
    
    @Test
    public void testAggregationLimitRows() throws ServletException, IOException {
    	String rowJson = "{\n" + 
                "       \"filtered\" : 2,\n" + 
                "       \"limit\" : 1,\n" + 
                "       \"mode\" : \"row-based\",\n" + 
                "       \"rows\" : [ {\n" + 
                "         \"cells\" : [ {\n" + 
                "           \"v\" : \"a\"\n" + 
                "         }, {\n" + 
                "           \"v\" : \"b\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 0,\n" + 
                "         \"starred\" : false\n" + 
                "       } ],\n" + 
                "       \"start\" : 0,\n" + 
                "       \"total\" : 5,\n" +
                "       \"processed\": 2\n" + 
                "     }";
        
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"row-based\",\"facets\":[],\"aggregationLimit\":2}");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(rowJson, writer.toString());
    }
    
    @Test
    public void testJsonOutputRecords() throws ServletException, IOException {
        String recordJson = "{\n" + 
                "       \"filtered\" : 3,\n" + 
                "       \"limit\" : 1,\n" + 
                "       \"mode\" : \"record-based\",\n" + 
                "       \"rows\" : [ {\n" + 
                "         \"cells\" : [ {\n" + 
                "           \"v\" : \"a\"\n" + 
                "         }, {\n" + 
                "           \"v\" : \"b\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 0,\n" + 
                "         \"j\" : 0,\n" + 
                "         \"starred\" : false\n" + 
                "       }, {\n" + 
                "         \"cells\" : [ null, {\n" + 
                "           \"v\" : \"c\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 1,\n" + 
                "         \"starred\" : false\n" + 
                "       } ],\n" + 
                "       \"start\" : 0,\n" + 
                "       \"total\" : 3,\n" +
                "       \"processed\": 3\n" + 
                "     }";
        
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[]}");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(recordJson, writer.toString());
    }
    
    @Test
    public void testAggregationLimitRecords() throws ServletException, IOException {
        String recordJson = "{\n" + 
                "       \"filtered\" : 2,\n" + 
                "       \"limit\" : 1,\n" + 
                "       \"mode\" : \"record-based\",\n" + 
                "       \"rows\" : [ {\n" + 
                "         \"cells\" : [ {\n" + 
                "           \"v\" : \"a\"\n" + 
                "         }, {\n" + 
                "           \"v\" : \"b\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 0,\n" + 
                "         \"j\" : 0,\n" + 
                "         \"starred\" : false\n" + 
                "       }, {\n" + 
                "         \"cells\" : [ null, {\n" + 
                "           \"v\" : \"c\"\n" + 
                "         } ],\n" + 
                "         \"flagged\" : false,\n" + 
                "         \"i\" : 1,\n" + 
                "         \"starred\" : false\n" + 
                "       } ],\n" + 
                "       \"start\" : 0,\n" + 
                "       \"total\" : 3,\n" +
                "       \"processed\": 2\n" + 
                "     }";
        
        when(request.getParameter("engine")).thenReturn("{\"mode\":\"record-based\",\"facets\":[],\"aggregationLimit\":2}");
        when(request.getParameter("limit")).thenReturn("1");
        command.doPost(request, response);
        TestUtils.assertEqualAsJson(recordJson, writer.toString());
    }
}
