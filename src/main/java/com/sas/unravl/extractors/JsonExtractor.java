package com.sas.unravl.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.annotations.UnRAVLExtractorPlugin;
import com.sas.unravl.generators.Text;
import com.sas.unravl.util.Json;

import org.apache.log4j.Logger;

/**
 * An extractor for <code>{ "json" : "varName" }</code> or
 * <code>{ "json" : "@file-name" }</code>
 * 
 * <p>
 * In both cases, the JSON body is also bound to the variable "responseBody"
 * 
 * @author David.Biesack@sas.com
 */

@UnRAVLExtractorPlugin("json")
public class JsonExtractor extends BaseUnRAVLExtractor {

    private static final Logger logger = Logger.getLogger(JsonExtractor.class);

    @Override
    public void extract(UnRAVL current, ObjectNode extractor, ApiCall call)
            throws UnRAVLException {
        super.extract(current, extractor, call);
        JsonNode target = Json.firstFieldValue(extractor);
        if (!target.isTextual())
            throw new UnRAVLException(
                    "json binding value must be a var name or a @file-name string");
        String to = target.textValue();

        JsonNode json = Json.parse(Text.utf8ToString(call.getResponseBody()
                .toByteArray()));
        current.bind("responseBody", json);
        if (to.startsWith(UnRAVL.REDIRECT_PREFIX)) {
            String where = to.substring(UnRAVL.REDIRECT_PREFIX.length());
            where = getScript().expand(where);
            Json.extractToStream(json, where);
            if (!where.equals("-"))
                logger.info("Wrote JSON to file " + where);
        } else {
            current.bind(to, json);
        }
    }

}
