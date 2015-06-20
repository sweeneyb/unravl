// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.assertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.io.PatternFilenameFilter;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLRuntime;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * A convenience class for running UnRAVL scripts within a JUnit environment.
 * {@link UnRAVLException}s (including {@link UnRAVLAssertionException}s) are
 * wrapped in {#link AssertionErrors}.
 *
 * @author DavidBiesack@sas.com
 */
public class JUnitWrapper {

    static final Logger logger = Logger.getLogger(JUnitWrapper.class);

    /**
     * a FilenameFilter for selecting files that end in .json or .unravl
     */
    public static final FilenameFilter unravlScriptFile = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".json") || name.endsWith(".unravl");
        }
    };

    /**
     * Run all scripts in the directory. TODO: Add boolean recursive option
     * THis will try to run all scripts, even if some fail.
     * @param map initial environment
     * @param directoryName directory where scripts should be found
     */
    public static void runScriptsInDirectory(Map<String, Object> map,
            String directoryName) {
        runScriptsInDirectory(map, directoryName, null);
    }


    /**
     * Run scripts in the directory if the file names match the pattern. TODO: Add boolean recursive option
     * THis will try to run all scripts, even if some fail.
     * @param map initial environment
     * @param directoryName directory where scripts should be found
     * @param pattern Only run scripts whose name match this file name pattern.
     */
    public static void runScriptsInDirectory(Map<String, Object> map,
            String directoryName, final String pattern) {
        File dir = new File(directoryName);
        ArrayList<String> fileNames = new ArrayList<String>();
        FilenameFilter filter = pattern == null ? unravlScriptFile
                : new PatternFilenameFilter(pattern);
        if (dir.exists() && dir.isDirectory()) {
            File files[] = dir.listFiles(filter);
            for (File file : files) {
                if (!file.isDirectory())
                    fileNames.add(file.getAbsolutePath());
            }
            int count = runScriptFiles(map,
                    fileNames.toArray(new String[fileNames.size()]));
            System.out.println(String.format("Ran %s scripts in %s%s", count,
                    directoryName, pattern == null ? "" : " matching pattern "
                            + pattern));
        }
    }

    /**
     * Run a set of script files. Each runs in the initial env passed in (the
     * environments modified by each script is discarded). This method asserts
     * that each script has no assertion failures. Run each script,
     * even if earlier ones had failures.
     *
     * @param env
     *            The initial environment to pass to each.
     * @param scriptFileNames an array of script file names to run.
     * @return number of scripts which ran
     */
    public static int runScriptFiles(Map<String, Object> env,
            String... scriptFileNames) {
        // for now, assume each command line arg is an UnRAVL script
        int count = 0;
        Throwable caught = null;
        for (String scriptFile : scriptFileNames) {
            try {
                count++;
                UnRAVLRuntime runtime = new UnRAVLRuntime();
                Map<String, Object> newEnv = env == null ? new HashMap<String, Object>() : new HashMap<String, Object>(env);
                bind(runtime, newEnv);
                System.out.println("Run UnRAVL script " + scriptFile);
                runtime.execute(scriptFile);
                for (ApiCall call : runtime.getApiCalls()) {
                    assertEquals("script " + scriptFile
                            + " should have had 0 assertion failures.", 0, call
                            .getFailedAssertions().size());
                }
            } catch (Throwable t) {
                logger.error(t.getMessage());
                caught = t;
            }
        }
        if (caught != null) {
            caught.printStackTrace(System.err);
            fail(caught.getMessage());
        }
        return count;
    }

    private static void bind(UnRAVLRuntime runtime, Map<String, Object> env) {

        for (Map.Entry<String, Object> e : env.entrySet()) {
            runtime.bind(e.getKey(), e.getValue());
        }

    }

    /**
     * Run all scripts in the directory, but expect an UnRAVLException. This
     * should be used to test invalid scripts.
     *
     * TODO: Add boolean recursive option
     *
     * @param env Additional variable bindings
     * @param directoryName directory to scan for scripts
     */
    public static void tryScriptsInDirectory(Map<String, Object> env,
            String directoryName) {
        tryScriptsInDirectory(env, directoryName, null);
    }

    public static void tryScriptsInDirectory(Map<String, Object> env,
            String directoryName, String pattern) {
        File dir = new File(directoryName);
        ArrayList<String> fileNames = new ArrayList<String>();
        FilenameFilter filter = pattern == null ? unravlScriptFile
                : new PatternFilenameFilter(pattern);
        if (dir.exists() && dir.isDirectory()) {
            File files[] = dir.listFiles(filter);
            for (File file : files) {
                if (!file.isDirectory())
                    fileNames.add(file.getAbsolutePath());
            }
            int count = JUnitWrapper.tryScriptFiles(env,
                    fileNames.toArray(new String[fileNames.size()]));
            System.out.println(String.format("Tried %s scripts in %s%s", count,
                    directoryName, pattern == null ? "" : " matching pattern "
                            + pattern));
        }
    }

    /**
     * Run a set of script files, but expect an UnRAVLException. This should be
     * used to test invalid scripts
     * @param env Additional variable bindings
     * @param scriptFileNames script file names to scan for scripts
     * @return the number of scripts which ran
     */
    public static int tryScriptFiles(Map<String, Object> env,
            String... scriptFileNames) {
        int count = 0;
        // for now, assume each command line arg is an UnRAVL script
        for (String scriptFile : scriptFileNames) {
            try {
                count++;
                UnRAVLRuntime runtime = new UnRAVLRuntime();
                bind(runtime, env);
                runtime.execute(scriptFile);

                if (runtime.getFailedAssertionCount() > 0) {
                    logger.info("UnRAVL script file " + scriptFile
                            + " failed when expected.");
                    continue;
                } else
                    fail("UnRAVL script was expected to fail but did not: "
                            + scriptFile);
            } catch (UnRAVLException e) {

                logger.info("UnRAVL script file " + scriptFile
                        + " failed when expected.");
                continue;
            }
            fail("UnRAVL script was expected to fail but did not: "
                    + scriptFile);
        }
        return count;
    }

}
