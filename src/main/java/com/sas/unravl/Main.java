package com.sas.unravl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * The main command-line interface for running {@link UnRAVL} scripts. You can
 * run via <code>/u/sasdjb/bin/unravl</code> or
 * <code>\\\\dntsrc\\u\\sasdjb\\bin\\unravl</code>
 *
 * <pre>
 * unravl script-file [... script-file]
 * </pre>
 *
 * Each argument on the command line names an UnRAVL script file Each such file
 * may contain the JSON representation of a script, or a script suite, which is
 * a JSON array of UnRAVL scripts.
 * <p>
 * By default, unravl has a built-in Log4j configuration file, which has tracing
 * enabled for the com.sas.unravl package, but you can pass your own with
 *
 * <pre>
 * java -Djog4j.configuration=mylog4j.properties -jar sas.unravl.jar script.json
 * </pre>
 *
 * You can also use this java invocation if you want to pass initial variable
 * bindings for the {@link UnRAVLRuntime} environment.
 *
 * <pre>
 * java -Dvar1=value1 -Dvar2=value2 -jar sas.unravl.jar script.json
 * </pre>
 *
 * @author sasdjb
 */
public final class Main {

    /**
     * Man entry point. Each arg is the name of an UnRAVL script file or URL All
     * scripts will run in the same shared UnRAVLRuntime and thus share a common
     * environment and set of variables.
     */
    public static void main(String argv[]) {
        argv = preProcessArgs(argv);
        configureLog4j();
        UnRAVLRuntime.configure();
        int rc = new Main().run(argv);
        System.exit(rc);
    }

    // Scan for --v|-verbose|-q|--quiet and set the log4j configuration
    // remove those args from the arg list and return the remainder
    private static String[] preProcessArgs(String[] argv) {
        ArrayList<String> args = new ArrayList<String>();
        String log4j = null;
        for (String arg : argv) {
            if (arg.matches("^--?q(uiet)?"))
                log4j = "log4j-quiet.properties";
            else if (arg.matches("^--?v(erbose)?"))
                log4j = "log4j-trace.properties";
            else
                args.add(arg);
        }
        if (log4j != null)
            System.setProperty("log4j.configuration", log4j);
        return args.toArray(new String[args.size()]);
    }

    private static void configureLog4j() {
        if (System.getProperty("log4j.configuration") == null) {
            try {
                Properties properties = new Properties();
                properties.load(Main.class
                        .getResourceAsStream("/log4j.properties"));
                org.apache.log4j.PropertyConfigurator.configure(properties);
            } catch (IOException e) {
                System.err.println("Could not load log4j config");
                System.exit(1);
            }
        } else {
            org.apache.log4j.BasicConfigurator.configure();
        }
    }

    public int run(String argv[]) {
        UnRAVLRuntime runtime = new UnRAVLRuntime();
        try {
            return runtime.execute(argv).report();
        } catch (UnRAVLException e) {
            int rc = runtime.report();
            return rc != 0 ? rc : 1;
        } catch (Throwable t) {
            System.err.println(t.getMessage());
            int rc = runtime.report();
            t.printStackTrace(System.err);
            return rc != 0 ? rc : 1;
        }
    }

}