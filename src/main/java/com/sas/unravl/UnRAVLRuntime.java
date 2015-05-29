package com.sas.unravl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.annotations.UnRAVLExtractorPlugin;
import com.sas.unravl.assertions.UnRAVLAssertion;
import com.sas.unravl.assertions.UnRAVLAssertionException;
import com.sas.unravl.generators.UnRAVLRequestBodyGenerator;
import com.sas.unravl.util.Json;
import com.sas.unravl.util.VariableResolver;

import groovy.lang.Binding;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 * The runtime environment for running UnRAVL scripts. The runtime contains the
 * bindings which are accessed and set by scripts, and provides environment
 * expansion of strings. The runtime also contains the global mappings of
 * assertions, extractors, and request body generators, and a map of scripts and
 * templates
 * 
 * @author DavidBiesack@sas.com
 */
@Component
public class UnRAVLRuntime {

    private static final Logger logger = Logger.getLogger(UnRAVLRuntime.class);
    private Binding env; // script variables
    private Map<String, UnRAVL> scripts = new LinkedHashMap<String, UnRAVL>();
    private Map<String, UnRAVL> templates = new LinkedHashMap<String, UnRAVL>();
    // a history of the API calls we've made in this runtime
    private ArrayList<ApiCall> calls = new ArrayList<ApiCall>();
    private int failedAssertionCount;

    // used to expand variable references {varName} in strings:
    private VariableResolver variableResolver;

    public UnRAVLRuntime() {
        this(new Binding());
    }

    public UnRAVLRuntime(Map<String, Object> environment) {
        this(new Binding(environment));
    }

    private UnRAVLRuntime(Binding environment) {
        configure();
        this.env = environment;
        // dont't pass System.getProperies() directly to the Binding(Map)
        // constructor;
        // see http://gitlab.sas.com/sasdjb/unravl/issues/13
        for (Map.Entry<Object, Object> e : System.getProperties().entrySet())
            bind(e.getKey().toString(), e.getValue());
        bind("failedAssertionCount", Integer.valueOf(0));
        resetBindings();
    }

    public Binding getBindings() {
        return env;
    }

    public int getFailedAssertionCount() {
        return failedAssertionCount;
    }

    public void incrementFailedAssertionCount() {
        failedAssertionCount++;
        bind("failedAssertionCount", Integer.valueOf(failedAssertionCount));
    }

    @SuppressWarnings("unchecked")
    public Map<String, ?> getEnv() {
        return env.getVariables();
    }

    public Map<String, UnRAVL> getScripts() {
        return scripts;
    }

    private Map<String, UnRAVL> getTemplates() {
        return templates;
    }

    private static ClassPathXmlApplicationContext ctx = null;

    /**
     * UnRAVL can be configured with Spring by loading the Spring config
     * classpath:/META-INF/spring/unravlApplicationContext.xml . If that has not
     * been done, this method initializes Spring. Spring performs a
     * component-scan for assertions, body generators, and extractors. As each
     * such component is loaded, it's runtime property is {@literal @}autowired
     * to a UnRAVLRuntime instance this calls the register*() methods. See
     * {@link UnRAVLRequestBodyGenerator}, {@link UnRAVLAssertionPlugin}, and
     * {@link UnRAVLExtractorPlugin}.
     */
    public static synchronized void configure() {
        if (ctx != null)
            return;

        // Configure Spring. Works on Unix; fails on Windows?
        String[] contextXml = new String[] { "/META-INF/spring/unravlApplicationContext.xml" };
        ctx = new ClassPathXmlApplicationContext(contextXml);
        assert (ctx != null);
    }

    public UnRAVLRuntime execute(String[] argv) throws UnRAVLException {
        // for now, assume each command line arg is an UnRAVL script
        for (String scriptFile : argv) {
            try {
                List<JsonNode> roots = read(scriptFile);
                execute(roots);
            } catch (IOException e) {
                logger.error(e.getMessage() + " while running UnRAVL script "
                        + scriptFile);
                throw new UnRAVLException(e);
            } catch (UnRAVLException e) {
                logger.error(e.getMessage() + " while running UnRAVL script "
                        + scriptFile);
                throw (e);
            }
        }
        return this;
    }

    private void execute(List<JsonNode> roots) throws JsonProcessingException,
            IOException, UnRAVLException {
        for (JsonNode root : roots) {
            String label = "";
            try {
                UnRAVL u = null;
                if (root.isTextual()) {
                    String name = root.textValue();
                    label = name;
                    u = getScripts().get(name);
                    if (u == null) {
                        throw new UnRAVLException(String.format(
                                "No such UnRAVL script named '%s'", name));
                    }
                } else
                    u = new UnRAVL(this, root);
                label = u.getName();
                u.run();
            } catch (UnRAVLAssertionException e) {
                logger.error(e.getMessage() + " while running UnRAVL script "
                        + label);
                incrementFailedAssertionCount();
            } catch (RuntimeException rte) {
                if (rte.getCause() instanceof UnRAVLException) { // tunneled
                                                                 // exception
                    UnRAVLException e = (UnRAVLException) rte.getCause();
                    throw e;
                } else
                    throw rte;
            }
        }
    }

    public UnRAVLRuntime execute(String scriptFile) throws UnRAVLException {
        // for now, assume each command line arg is an UnRAVL script
        try {
            List<JsonNode> roots = read(scriptFile);
            execute(roots);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new UnRAVLException(e);
        } catch (UnRAVLException e) {
            logger.error(e.getMessage());
            throw (e);
        }
        return this;
    }

    /**
     * Expand environment variables in a string. For example, if string is
     * 
     * <pre>
     * &quot;{time} is the time for {which} {who} to come to the aid of their {where}&quot;
     * </pre>
     * 
     * and the environment contains the bindings
     * 
     * <pre>
     * time = "Mon, Aug 4, 2014"
     * which = 16
     * who = "hackers
     * where = "API"
     * </pre>
     * 
     * the result will be
     * 
     * <pre>
     * &quot;Mon, Aug 4, 2014 is the time for 16 hackers to come to the aid of their API&quot;
     * </pre>
     * 
     * The toString() value of each binding in the environment is substituted.
     * 
     * An optional notation is allowed to provide a default value if a variable
     * is not bound. <code>{varName|alt text}</code> will resolve to the value
     * of varName if it is bound, or to alt text if varName is not bound. The
     * alt text may also contain embedded variable expansions.
     * 
     * @param text
     *            an input string
     * @return the string, with environment variables replaced.
     */
    public String expand(String text) {
        if (variableResolver == null) {
            prepareEnvironmentExpansion();
        }
        return variableResolver.expand(text);
    }

    // The contract for Bindings is not clear. We perhaps do not
    // have to reset the variable resolver when Bindings have changed,
    // as the current implementation returns the internal LinkedHashMap.
    // But we reconstruct the VariableResolver to be safe.
    private void prepareEnvironmentExpansion() {
        @SuppressWarnings("unchecked")
        Map<String, Object> substitutions = getBindings().getVariables();
        variableResolver = new VariableResolver(substitutions);
    }

    public void bind(String varName, Object value) {
        if (VariableResolver.isUnicodeCodePointName(varName)) {
            UnRAVLException ue = new UnRAVLException(String.format(
                    "Cannot rebind special Unicode variable %s", varName));
            throw new RuntimeException(ue);
        }
        getBindings().setVariable(varName, value);
        logger.trace("bind(" + varName + "," + value + ")");
        // reset the pattern so it gets rebuilt on demand
        // in expand(String)
        resetBindings();
    }

    public boolean bound(String varName) {
        return getBindings().hasVariable(varName);
    }

    public Object binding(String varName) {
        return getBindings().getVariable(varName);
    }

    /**
     * Call this when bindings have changed.
     */
    public void resetBindings() {
        // null signals that we need to recreate the resolver since
        // the Bindings object has changed.
        // this gets bound again if needed in expand(String)
        variableResolver = null;
    }

    public static List<JsonNode> read(String scriptFile)
            throws JsonProcessingException, IOException, UnRAVLException {
        JsonNode root;
        List<JsonNode> roots = new ArrayList<JsonNode>();
        ObjectMapper mapper = new ObjectMapper();
        URL url = null;
        try {
            url = new URL(scriptFile);
        } catch (MalformedURLException e) {
        }
        if (url != null)
            root = mapper.readTree(url);
        else {
            File f = new File(scriptFile);
            root = mapper.readTree(f);
        }

        if (root.isArray()) {
            for (JsonNode next : Json.array(root)) {
                if (next.isTextual()) {
                    String ref = next.textValue();
                    if (ref.startsWith("@"))
                        roots.addAll(read(ref.substring(1)));
                    else {
                        roots.add(next);
                    }
                } else
                    roots.add(Json.object(next));
            }
        } else
            roots.add(root);
        return roots;
    }

    public int report() {
        int failed = (calls.size() == 0 ? 1 : 0);
        String separator = "";
        for (ApiCall call : calls) {
            UnRAVL script = call.getScript();
            String title = "Script '"
                    + script.getName()
                    + "' "
                    + (call.getMethod() == null ? "<no method>" : script
                            .getMethod().toString()) + " "
                    + (call.getURI() == null ? "<no URI>" : call.getURI());
            System.out.print(separator);
            for (int i = title.length(); i > 0; i--)
                System.out.print('-');
            System.out.println();
            System.out.println(title);

            if (call.getException() != null) {
                System.out.println("Caught exception running test "
                        + script.getName() + " " + call.getMethod() + " "
                        + call.getURI());
                System.out.println(call.getException().getMessage());
                System.out.flush();
                failed++;
            }
            report(call.getPassedAssertions(), "Passed");
            failed += report(call.getFailedAssertions(), "Failed");
            report(call.getSkippedAssertions(), "Skipped");
            separator = System.getProperty("line.separator").toString();
        }
        return failed;
    }

    private int report(List<UnRAVLAssertion> as, String label) {
        if (as.size() > 0) {
            System.out.println(as.size() + " " + label + ":");
            for (UnRAVLAssertion a : as) {
                System.out.println(label + " " + a.getStage().getName() + " "
                        + a);
                System.out.flush();
                UnRAVLAssertionException e = a.getUnRAVLAssertionException();
                if (e != null) {
                    System.out.println(e.getClass().getName() + " "
                            + e.getMessage());
                }
            }
        }
        return as.size();
    }

    public List<ApiCall> getApiCalls() {
        return calls;
    }

    public void addApiCall(ApiCall apiCall) {
        calls.add(apiCall);
    }

    public UnRAVLPlugins getPlugins() {
        return ctx.getBean(UnRAVLPlugins.class);
    }

    public UnRAVL getTemplate(String templateName) {
        // TODO Auto-generated method stub
        return getTemplates().get(templateName);
    }

    public void setTemplate(String name, UnRAVL template) {
        // TODO: check for template cycles
        if (hasTemplate(name)) {
            logger.warn("Replacing template " + name);
        }
        getTemplates().put(name, template);

    }

    public boolean hasTemplate(String name) {
        return getTemplates().containsKey(name);
    }

}