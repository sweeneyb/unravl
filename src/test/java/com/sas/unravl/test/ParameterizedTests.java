// Copyright (c) 2016, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.test;

import com.sas.unravl.assertions.JUnitWrapper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * This test demonstrates how use JUnit @Parameterized to run
 * scripts in a directory as individual JUnit tetss.
 * Change the accept() function in the FilenameFilter to
 * <pre>
 * return name.endsWith(".json") || name.endsWith(".unravl");
 * </pre>
 * <p>
 * to run all test scripts in the directory. This example
 * just runs a couple, to make builds faser (Other tests
 * run all scripts, and we don't want to duplicate them all.)
 * <p>
 * @author sasdjb
 */
@RunWith(Parameterized.class)
public class ParameterizedTests
{
    @Parameter
    public File scriptFile;
    
    @Parameters(name="{0}")
    public static List<File> scripts() {
        File dir = new File("src/test/scripts");
        File scripts[] = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name)
            {
                
                return name.equals("json-equals.json") || name.equals("jsonPath.json");
            }
        });
        return Arrays.asList(scripts);
    }
    
    @Test
    public void runScript() {
        JUnitWrapper.runScriptFiles(TestScripts.env(), scriptFile.getPath());
    }

}
