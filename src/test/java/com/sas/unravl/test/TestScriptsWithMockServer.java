// Copyright (c) 2014, SAS Institute Inc., Cary, NC, USA, All Rights Reserved
package com.sas.unravl.test;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sas.unravl.UnRAVLException;
import com.sas.unravl.UnRAVLPlugins;
import com.sas.unravl.UnRAVLRuntime;
import com.sas.unravl.assertions.JUnitWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * These tests use a RestTemplate and Spring MockRestServiceServer. The scripts
 * are all located in the src/test/scripts/mock directory.
 * <p>
 * UnRAVL scripts in the src/test/scripts/mock/fail directory should fail with
 * assertion errors.
 * 
 * @author David.Biesack@sas.com
 */
public class TestScriptsWithMockServer extends TestBase {

    private static final String SRC_TEST_SCRIPTS_MOCK = "src/test/scripts/mock";
    private static final String SRC_TEST_SCRIPTS_MOCK_FAIL = SRC_TEST_SCRIPTS_MOCK
            + "/fail";
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @Before
    public void createMockServer() {
        restTemplate = new RestTemplate();
        setRestTemplate(restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    static UnRAVLRuntime runtime;

    private static void setRestTemplate(RestTemplate restTemplate) {
        runtime = new UnRAVLRuntime();
        UnRAVLPlugins plugins = runtime.getPlugins();
        plugins.setRestTemplate(restTemplate);
    }

    @After
    public void afterClass() {
        setRestTemplate(null);
    }

    @Test
    public void helloJson() throws UnRAVLException {

        createHelloJsonMock();
        JUnitWrapper.runScriptsInDirectory(runtime, SRC_TEST_SCRIPTS_MOCK,
                "helloJson.json");
        mockServer.verify();
    }

    @Test
    public void helloJsonFail() throws UnRAVLException {
        createHelloJsonMock();
        JUnitWrapper.tryScriptsInDirectory(runtime, null,
                SRC_TEST_SCRIPTS_MOCK_FAIL, "helloJson.json");

    }

    private void createHelloJsonMock() throws UnRAVLException {
        mockServer
                .expect(requestTo("/hello.json"))
                .andRespond(
                        withSuccess(
                                mockJson(
                                        "{ 'greeting' : 'Hello', 'addressee' : 'World' }")
                                        .toString(), MediaType.APPLICATION_JSON));
    }


    private void createErrorJsonMock() throws UnRAVLException {
        String responseBody = mockJson("{ 'error' : 'BAD REQUEST', 'httpStatusCode' : 400 }").toString();
        mockServer
                .expect(requestTo("/error.json"))
                .andRespond(
                        withBadRequest().body(responseBody).contentType(MediaType.APPLICATION_JSON));
    }



    @Test
    public void helloText() throws UnRAVLException {
        createHelloTextMock();
        JUnitWrapper.runScriptsInDirectory(runtime, SRC_TEST_SCRIPTS_MOCK,
                "helloText.json");

        mockServer.verify();
    }

    @Test
    public void helloTextFail() throws UnRAVLException {
        createHelloTextMock();
        JUnitWrapper.tryScriptsInDirectory(runtime, null,
                SRC_TEST_SCRIPTS_MOCK_FAIL, "helloText.json");
    }

    private void createHelloTextMock() {
        mockServer.expect(requestTo("/hello.txt")).andRespond(
                withSuccess("Hello, World!", MediaType.TEXT_PLAIN));
    }

    @Test
    public void binary() throws UnRAVLException {
        createBinaryMock();
        JUnitWrapper.runScriptsInDirectory(runtime, SRC_TEST_SCRIPTS_MOCK,
                "binary.json");
        mockServer.verify();
    }

    @Test
    public void binaryFail() throws UnRAVLException {
        createBinaryMock();
        JUnitWrapper.tryScriptsInDirectory(runtime, null,
                SRC_TEST_SCRIPTS_MOCK_FAIL, "binary.json");
    }

    private void createBinaryMock() {
        mockServer.expect(requestTo("/binary.dat")).andRespond(
                withSuccess(new String(new byte[] { 0, 1, 2, 3, 4, 5, 6 }),
                        MediaType.APPLICATION_OCTET_STREAM));
    }
    

    @Test
    public void errorResponse() throws UnRAVLException {

        createErrorJsonMock();
        JUnitWrapper.runScriptsInDirectory(runtime, SRC_TEST_SCRIPTS_MOCK_FAIL,
                "errorJson.json");
        mockServer.verify();
    }
    

}
