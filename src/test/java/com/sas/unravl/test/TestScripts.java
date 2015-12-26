// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVLRuntime;
import com.sas.unravl.assertions.JUnitWrapper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
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

    @Test(expected = AssertionError.class)
    public void testScriptsBadDir() {
        JUnitWrapper.runScriptsInDirectory(env(), "no-such/directory");
    }

    @Test
    public void testScriptsWithRuntime() {
        UnRAVLRuntime runtime = new UnRAVLRuntime(env());
        String[] scripts = new String[] { "src/test/scripts/parts/env.json",
                "src/test/scripts/parts/assert-env.json",
                "src/test/scripts/parts/assert-junit.json" };
        JUnitWrapper.runScriptFiles(runtime, scripts);
        // now verify that both used the same runtime
        List<ApiCall> calls = runtime.getApiCalls();
        assertEquals(scripts.length, calls.size());
        for (ApiCall call : calls) {
            assertEquals(0, call.getFailedAssertions().size());
        }
        // Warning: following is based on the values assigned in
        // src/test/scripts/parts/env.json
        assertEquals(0, ((Number) runtime.binding("x")).intValue());
        assertEquals(1, ((Number) runtime.binding("y")).intValue());
        assertEquals(-1, ((Number) runtime.binding("z")).intValue());
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

    @Test(expected = AssertionError.class)
    public void tryScriptsBadDir() {
        JUnitWrapper.tryScriptsInDirectory(env(), "no-such/directory");
    }

    @Test
    public void assertionLogged1() {
        assertionLogged("wrong-http-status-code.json", "this is an invalid assertion, this really returns 200");
    }


    @Test
    public void assertionLogged2() {
        assertionLogged("implicit.json", "count < 3");
    }

    public void assertionLogged(String testName, String expected) {
        // Run a script which fails with a status assertion,
        // and verify that the body of the exception gets printed to the console.
        // This assumes the unit tests run with Log4J directed to the console.
        PrintStream out = System.out;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream capturedOut = new PrintStream(os);
        try {
            System.setOut(capturedOut);
            JUnitWrapper.runScriptsInDirectory(env(), "src/test/scripts/fail",
                    testName);
        } catch (Throwable t) {
            capturedOut.flush();
            String stdout = new String(os.toString());
            assertTrue(stdout
                    .contains(expected));
        } finally {
            System.setOut(out);
        }
    }
}
