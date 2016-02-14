// Copyright (c) 2016, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.test;

import com.sas.unravl.UnRAVLRuntime;
import com.sas.unravl.assertions.JUnitWrapper;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

public class TestPropertyListener implements PropertyChangeListener {

    HashMap<String, PropertyChangeEvent> changes;
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
       changes.put(evt.getPropertyName(), evt);
    }
    
    @Before
    public void before() {
        changes = new HashMap<String, PropertyChangeEvent>();
    }
    
    @Test 
    public void propertyChange() {
        UnRAVLRuntime rt = new UnRAVLRuntime(TestScripts.env());
        rt.addPropertyChangeListener(this);
        JUnitWrapper.runScriptsInDirectory(rt, TestScripts.TEST_SCRIPTS_DIR, "env.json");
        assertNotNull(changes.get("name"));
        assertNotNull(changes.get("x"));
    }


}
