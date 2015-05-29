package com.sas.unravl.test;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.generators.Text;

import java.io.IOException;

import org.junit.Test;

public class TestText extends TestBase {

    @Test
    public void simpleText() throws IOException, UnRAVLException {
        JsonNode node = mockJson("{ 'text' : 'plain text' }");
        Text t = new Text(node, "text");
        assertEquals("plain text", t.text());
    }

    @Test
    public void arrayText() throws IOException, UnRAVLException {
        JsonNode node = mockJson("{ 'text' : [ 'A', 'B', 'C' ] }");
        Text t = new Text(node, "text");
        assertEquals("A\nB\nC", t.text());
    }

    @Test
    public void nestedText() throws IOException, UnRAVLException {
        // { "text" : [ "A", "@src/test/java/B.txt", [ "C" ] ] }
        JsonNode node = mockJson("{ \"text\" : [ 'A', '@src/test/java/B.txt', [ 'C' ] ] }");
        Text t = new Text(node, "text");
        // B.txt has a new line also, and newlines between elements
        assertEquals("A\nB\n\nC", t.text());
    }

    @Test(expected = IOException.class)
    public void noSuchFile() throws IOException, UnRAVLException {
        JsonNode node = mockJson("{'text' : '@noSuchFile' }");
        new Text(node, "text");
    }

    @Test(expected = IOException.class)
    public void noSuchURL() throws IOException, UnRAVLException {
        JsonNode node = mockJson("{'text' : '@@scheme://host:9090/no/such/resource.ext' }");
        new Text(node, "text");
    }

}
