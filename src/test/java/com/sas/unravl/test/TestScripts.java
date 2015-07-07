// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.test;

import com.sas.unravl.assertions.JUnitWrapper;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestScripts {

    @Test
    public void testScripts() {
        System.out.println(System.getProperty("user.dir"));
        JUnitWrapper.runScriptsInDirectory(env(), "src/test/scripts");
    }

    @Test
    public void testScriptsViaPattern() {
        JUnitWrapper.runScriptsInDirectory(env(), "src/test/scripts",
                "GoogleEverestElevation.json");
    }

    private Map<String, Object> env() {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put("JUnit", Boolean.TRUE);
        return env;
    }

    @Test
    public void tryScripts() {
        // All scripts in this folder should fail - i.e. bad scripts,
        // failed unRAVL assertions, etc.
        JUnitWrapper.tryScriptsInDirectory(env(), "src/test/scripts/fail");
    }

    @Test(expected = AssertionError.class)
    public void runFailingScripts() {
        // All scripts in this folder should fail - i.e. bad scripts,
        // failed unRAVL assertions, etc.
        JUnitWrapper.runScriptsInDirectory(env(), "src/test/scripts/fail");
    }
}
