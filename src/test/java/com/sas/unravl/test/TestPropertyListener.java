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
import static org.junit.Assert.assertNull;

public class TestPropertyListener implements PropertyChangeListener {

    HashMap<String, PropertyChangeEvent> changes = new HashMap<String, PropertyChangeEvent>();
    
    class OtherListener implements PropertyChangeListener {

        HashMap<String, PropertyChangeEvent> changes = new HashMap<String, PropertyChangeEvent>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
           changes.put(evt.getPropertyName(), evt);
        }
    }
    
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
        OtherListener other = new OtherListener();
        rt.addPropertyChangeListener(this);
        rt.addPropertyChangeListener(other);
        JUnitWrapper.runScriptsInDirectory(rt, TestScripts.TEST_SCRIPTS_DIR, "env.json");
        assertNotNull(changes.get("env.name"));
        assertNotNull(changes.get("env.x"));
        assertNotNull(other.changes.get("env.name"));
        assertNotNull(other.changes.get("env.x"));
        
        // now remove the other listener, and verify that it does not see changes
        
        rt.removePropertyChangeListener(other);
        // Reset the listener history
        changes.clear();
        other.changes.clear();

        assertNull(changes.get("env.name"));
        assertNull(changes.get("env.x"));

        assertNull(other.changes.get("env.name"));
        assertNull(other.changes.get("env.x"));

        JUnitWrapper.runScriptsInDirectory(rt, TestScripts.TEST_SCRIPTS_DIR, "env.json");

        assertNotNull(changes.get("env.name"));
        assertNotNull(changes.get("env.x"));
        assertNull(other.changes.get("env.name"));
        assertNull(other.changes.get("env.x"));
    }


}
