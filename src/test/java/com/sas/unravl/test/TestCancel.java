// Copyright (c) 2015, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.test;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLRuntime;
import com.sas.unravl.util.Json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class TestCancel extends TestBase {

    @Test
    public void test() throws UnRAVLException, JsonProcessingException,
            IOException {

        ArrayList<JsonNode> l = new ArrayList<JsonNode>(3);
        l.add(mockJson("{ 'env' : { 'shouldBeSet' : true }  }"));
        l.add(mockJson("{ 'bind' : { 'groovy' :  { 'canceledScript' : 'unravlScript.cancel(); true' }}}"));
        l.add(mockJson("{ 'env' : { 'shouldNotBeSet' : true }  }"));
        UnRAVLRuntime rt = new UnRAVLRuntime();
        rt.execute(l);
        assertTrue(rt.isCanceled());
        assertEquals(Boolean.TRUE, rt.binding("shouldBeSet"));
        assertEquals(Boolean.TRUE, rt.binding("canceledScript"));
        assertFalse(rt.bound("shouldNotBeSet"));
    }
}
