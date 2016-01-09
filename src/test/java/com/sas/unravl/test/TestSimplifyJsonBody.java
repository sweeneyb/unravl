package com.sas.unravl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.ApiCall;
import com.sas.unravl.UnRAVL;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLRuntime;
import com.sas.unravl.generators.Binary;
import com.sas.unravl.util.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

public class TestSimplifyJsonBody extends TestBase {

    // object
    @Test
    public void testJsonObject() throws Exception {
        assertRequestBodyJson("{'name':'value'}",
                "{'body':{'json':{'name':'value'}}}");

    }

    @Test
    public void testObjectWithoutJsonKey() throws Exception {
        assertRequestBodyJson("{'name':'value'}",
                "{'body':{'name':'value'}}");

    }

    @Test
    public void testObjectBooleanValue() throws Exception {
        assertRequestBodyJson("{'name':true}",
                "{'body':{'json':{'name':true}}}");

    }

    @Test
    public void testObjectBooleanValueWithoutJsonKey() throws Exception {
        assertRequestBodyJson("{'name':true}", "{'body':{'name':true}}");

    }

    @Test
    public void testObjectNumericValue() throws Exception {
        assertRequestBodyJson("{'name':1}", "{'body':{'json':{'name':1}}}");

    }

    @Test
    public void testObjectNumericValueWithoutJsonKey() throws Exception {
        assertRequestBodyJson("{'name':1}", "{'body':{'name':1}}");

    }

    @Test
    public void testObjectNullValue() throws Exception {
        assertRequestBodyJson("{'name':null}",
                "{'body':{'json':{'name':null}}}");

    }

    @Test
    public void testObjectNullValueWithoutJsonKey() throws Exception {
        assertRequestBodyJson("{'name':null}", "{'body':{'name':null}}");

    }

    // empty object

    @Test
    public void testObjectEmpty() throws Exception {

        assertRequestBodyJson("{}", "{'body':{'json':{}}}");
    }

    @Test
    public void testObjectEmptyWithoutJsonKey() throws Exception {
        assertRequestBodyJson("{}", "{'body':{}}");

    }

    // array

    @Test
    public void testArray() throws Exception {

        assertRequestBodyJson("[{'name':'value'}]",
                "{'body':{'json':[{'name':'value'}]}}");

    }

    @Test
    public void testArrayWithoutJsonKey() throws Exception {

        assertRequestBodyJson("[{'name':'value'}]",
                "{'body':[{'name':'value'}]}");

    }

    @Test
    public void testArrayMultipleElements() throws Exception {

        assertRequestBodyJson("[{'name':'value'},{'name2':'value2'}]",
                "{'body':{'json':[{'name':'value'},{'name2':'value2'}]}}");

    }

    @Test
    public void testArrayMultipleElementsWithoutJsonKey() throws Exception {

        assertRequestBodyJson("[{'name':'value'},{'name2':'value2'}]",
                "{'body':[{'name':'value'},{'name2':'value2'}]}");

    }

    @Test
    public void testEmptyArray() throws Exception {

        assertRequestBodyJson("[]", "{'body':{'json':[]}}");

    }

    @Test
    public void testEmptyArrayWithoutJsonKey() throws Exception {

        assertRequestBodyJson("[]", "{'body':[]}");

    }

    // textual file references

    @Test
    public void testTextualFileReference() throws Exception {

        assertRequestBodyJson("{'name':'SimplifyJsonBody'}",
                "{'body': {'json':'@src/test/SimplifyJsonBody.json'}}");

    }

    @Test
    public void testTextualFileReferenceWithoutJsonKey() throws Exception {

        assertRequestBodyJson("{'name':'SimplifyJsonBody'}",
                "{'body':'@src/test/SimplifyJsonBody.json'}");

    }

    // textual: var name containing json

    @Test
    public void testVarNameContainJsonValue() throws Exception {

        assertRequestBodyJson("{'name1':'val1'}",
                "{'env':{'var1':{'name1':'val1'}}, 'body':{'json':'var1'}}");

    }

    @Test
    public void testVarNameContainJsonValueWithoutJsonKey() throws Exception {

        assertRequestBodyJson("{'name1':'val1'}",
                "{'env':{'var1':{'name1':'val1'}}, 'body':'var1'}");

    }

    // textual
    @Test
    public void testTextualContent() throws Exception {

        assertRequestBody("textValue1", "{'body':{'text':'textValue1'}}");

    }

    @Test
    public void testTextualContentCarriageReturn() throws Exception {

        assertRequestBody("textValue1\r\n",
                "{'body':{'text':'textValue1\\r\\n'}}");

    }

    @Test
    public void testTextualContentMultiline() throws Exception {

        assertRequestBody("textValue1\r\ntextValue2",
                "{'body':{'text':'textValue1\\r\\ntextValue2'}}");

    }

    @Test
    public void testBlankTextualContent() throws Exception {

        assertRequestBody("", "{'body':''}");

    }

    @Test
    public void testNullTextualContent() throws Exception {

        assertRequestBody(null, "{'body':null}");

    }

    // binary
    @Test
    public void testBinay() throws Exception {

        String actuals = getActuals("{'body':{'binary':'@src/test/data/Un.png'}}");
        assertNotNull(actuals);

    }

    private void assertRequestBodyJson(String expected, String input)
            throws Exception {

        String actual = getActuals(input);
        assertEquals(mockJson(expected).toString(), actual);
    }

    private void assertRequestBody(String expected, String input)
            throws Exception {

        String actual = getActuals(input);
        assertEquals(expected, actual);
    }

    private String getActuals(String input) throws UnRAVLException,
            IOException, JsonProcessingException {
        ApiCall apiCall = createApiCall(input);
        String actual = getRequestBodyContent(apiCall);
        return actual;
    }

    private String getRequestBodyContent(ApiCall apiCall)
            throws UnRAVLException {
        apiCall.run(); // These calls may not have a method (PUT or POST) that will consume the requestStream
        if (apiCall.getRequestStream() == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Binary.copy(apiCall.getRequestStream(), baos);
            String requestBodyString = null;
            requestBodyString = baos.toString("UTF-8");
            return requestBodyString;
        } catch (UnsupportedEncodingException e) {
            throw new UnRAVLException(e.getMessage());// should not happen; UTF-8 should exist
        } catch (IOException e) {
            throw new UnRAVLException(e.getMessage());
        }
    }

    private ApiCall createApiCall(String input) throws UnRAVLException,
            IOException, JsonProcessingException {
        UnRAVLRuntime r = new UnRAVLRuntime();
        ObjectNode root = Json.object(mockJson(input));
        UnRAVL script = new UnRAVL(r, root);
        ApiCall apiCall = new ApiCall(script);
        return apiCall;
    }
}
