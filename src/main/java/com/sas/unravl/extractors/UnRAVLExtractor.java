package com.sas.unravl.extractors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;

/**
 * The interface for all UnRAVL extractors which extract content from an REST
 * API response body, status code, or headers and store those values in the
 * UnRAVL script's environment or elsewhere, such as in a file. Implementors
 * should extend {@link BaseUnRAVLExtractor}.
 * 
 * @author David.Biesack@sas.com
 *
 */
public interface UnRAVLExtractor {

    public void extract(UnRAVL current, ObjectNode extractor, ApiCall call)
            throws UnRAVLException;

    public void setExtractor(ObjectNode extractor);

    public ObjectNode extractor();

    public void setScript(UnRAVL script);

    public UnRAVL getScript();
}
