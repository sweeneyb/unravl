// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl;

import com.sas.unravl.annotations.UnRAVLAssertionPlugin;
import com.sas.unravl.annotations.UnRAVLAuthPlugin;
import com.sas.unravl.annotations.UnRAVLExtractorPlugin;
import com.sas.unravl.annotations.UnRAVLRequestBodyGeneratorPlugin;
import com.sas.unravl.assertions.UnRAVLAssertion;
import com.sas.unravl.auth.UnRAVLAuth;
import com.sas.unravl.extractors.UnRAVLExtractor;
import com.sas.unravl.generators.UnRAVLRequestBodyGenerator;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

/**
 * Manages the mappings of keywords to plugin implementation classes.
 * @author David.Biesack@sas.com
 */
public class UnRAVLPlugins {

    private static final Logger logger = Logger.getLogger(UnRAVLRuntime.class);

    private Map<String, Class<? extends UnRAVLRequestBodyGenerator>> requestBodyGenerators = new HashMap<String, Class<? extends UnRAVLRequestBodyGenerator>>();
    private Map<String, Class<? extends UnRAVLAssertion>> assertions = new HashMap<String, Class<? extends UnRAVLAssertion>>();
    private Map<String, Class<? extends UnRAVLExtractor>> extractors = new HashMap<String, Class<? extends UnRAVLExtractor>>();
    private Map<String, Class<? extends UnRAVLAuth>> auth = new HashMap<String, Class<? extends UnRAVLAuth>>();

    @Value("#{systemProperties['unravl.script.language'] ?: 'groovy'}")
    // must be "Groovy", "groovy", "JavaScript", "js", "javascript", or another valid ScriptEngine name
    String scriptLanguage = "groovy";

    public String scriptLanguage() {
        if (scriptLanguage == null || scriptLanguage.trim().length() == 0)
            return "groovy";
        else
            return scriptLanguage;
    }

    public ScriptEngine interpreter(String lang) throws UnRAVLException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName(lang == null ? scriptLanguage() : lang);
        if (engine == null) {
            logSupportedScriptEngines();
            throw new UnRAVLException(String.format(
                    "No script engine available for %sscript lanaguge %s",
                    lang == null ? "unravl.script.langauge " : "",
                    scriptLanguage()));
        }
        return engine;
    }

    public void addAssertion(Class<? extends UnRAVLAssertion> class1) {
        UnRAVLAssertionPlugin a = class1
                .getAnnotation(UnRAVLAssertionPlugin.class);
        String[] keys = a.value();
        for (String key : keys) {
            logger.trace("Define assertion '" + key + "' via " + class1);
            assertions.put(key, class1);
        }
    }

    public void addAuth(Class<? extends UnRAVLAuth> class1) {
        UnRAVLAuthPlugin a = class1.getAnnotation(UnRAVLAuthPlugin.class);
        String[] keys = a.value();
        for (String key : keys) {
            logger.trace("Define auth '" + key + "' via " + class1);
            auth.put(key, class1);
        }
    }

    public void addExtractor(Class<? extends UnRAVLExtractor> class1) {
        UnRAVLExtractorPlugin a = class1
                .getAnnotation(UnRAVLExtractorPlugin.class);
        for (String key : a.value()) {
            logger.trace("Define extractor '" + key + "' via " + class1);
            extractors.put(key, class1);
        }

    }

    public void addRequestBodyGenerator(
            Class<? extends UnRAVLRequestBodyGenerator> class1) {
        UnRAVLRequestBodyGeneratorPlugin a = class1
                .getAnnotation(UnRAVLRequestBodyGeneratorPlugin.class);
        for (String key : a.value()) {
            logger.trace("Define body generator '" + key + "' via " + class1);
            requestBodyGenerators.put(key, class1);
        }
    }

    public Map<String, Class<? extends UnRAVLAssertion>> getAssertions() {
        return assertions;
    }

    public Map<String, Class<? extends UnRAVLRequestBodyGenerator>> getBodyGenerators() {
        return requestBodyGenerators;
    }

    public Map<String, Class<? extends UnRAVLExtractor>> getExtractors() {
        return extractors;
    }

    public Map<String, Class<? extends UnRAVLAuth>> getAuth() {
        return auth;
    }

    /**
     * log availability of scripting engines supported in this environment.
     */
    public static void logSupportedScriptEngines() {
        ScriptEngineManager manager = new ScriptEngineManager();
        logger.error("Available Script Engines:");
        for (final ScriptEngineFactory scriptEngine : manager.getEngineFactories()) {
            logger.error(scriptEngine.getEngineName() + " "
                    + scriptEngine.getEngineVersion());
            logger.error("\tLanguage: " + scriptEngine.getLanguageName() + " "
                    + scriptEngine.getLanguageVersion());
            StringBuilder es = new StringBuilder();
            for (final String engineAlias : scriptEngine.getNames()) {
                es.append(engineAlias).append(",");
            }
            logger.error("\tAliases: " + es.toString());
        }
    }

}
