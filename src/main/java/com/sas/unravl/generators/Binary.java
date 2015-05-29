package com.sas.unravl.generators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sas.unravl.UnRAVLException;
import com.sas.unravl.util.Json;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class produces a binary byte stream from a JSON 'binary' specification.
 * 
 * <pre>
 * { "binary" : array-of-bytes } // the binary value is the result
 * { "binary" : "@fileOrUrl" } // read binary data from a File or URL
 * { "binary" : array-of-binary } // combine binary streams, each of which may be a array-of-bytes or a @file-or-url
 * </pre>
 * <p>
 * TODO: allow variable references <code>"varName"</code>; the value must be an
 * array of byte values.
 * </p>
 * 
 * @author David.Biesack@sas.com
 */
public class Binary {

    private static final int BUFSIZE = 256;
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    /**
     * Create an instance from a JSON object; the value associated with the
     * field name defines how to create the binary data. For example, for the
     * ObjectNode <code>{ "binary" : [ 0, 2, 1, 3 ]}</code> the constructor new
     * Binary(object, "binary")
     * 
     * @param node
     * @param fieldName
     * @throws IOException
     * @throws UnRAVLException
     */
    public Binary(ObjectNode node, String fieldName) throws IOException,
            UnRAVLException {
        this(node.get(fieldName));
    }

    /**
     * Construct a Binary instance from the binary spec value.
     * 
     * @param binarySpec
     *            either a TextNode with the value "@file-or-url", or a
     *            ArrayNode that contains integer byte values (0 to 255) or
     *            "@file-or-url" strings, or nested arrays.
     * @throws IOException
     * @throws UnRAVLException
     */
    public Binary(JsonNode binarySpec) throws IOException, UnRAVLException {
        build(binarySpec);
    }

    public void build(JsonNode node) throws IOException, UnRAVLException {
        if (node == null) {
        } else if (node.isTextual()) {
            build(node.textValue());
        } else if (node.isArray()) {
            for (JsonNode each : Json.array(node)) {
                build(each);
            }
        } else if (node.isInt()) {
            int b = node.asInt();
            if (b < 0 | b > 255)
                throw new UnRAVLException("Byte value " + b
                        + " not in [0..255] in binary element");
            bytes.write(b);
        } else if (node.isBinary()) {
            BinaryNode b = (BinaryNode) node;
            bytes.write(b.binaryValue());
        } else {
            throw new UnRAVLException("Invalid element " + node
                    + " in binary element.");
        }
    }

    private void build(String node) throws IOException, UnRAVLException {
        if (node.startsWith("@")) {
            buildFromStream(node.substring(1));
        } else {
            throw new UnRAVLException("Unrecognized element " + node
                    + "in 'binary' input.");
        }

    }

    private void buildFromStream(String fileOrURL) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(fileOrURL);
            is = url.openStream();
        } catch (MalformedURLException e) {
            File f = new File(fileOrURL);
            if (f.exists()) {
                is = new FileInputStream(f);
            } else {
                is = getClass().getResourceAsStream(fileOrURL);
            }
        }
        if (is == null) {
            throw new IOException("No such file or URL " + fileOrURL);
        }
        copy(is, bytes);
    }

    /**
     * Copy bytes from an input stream to an output stream.
     * 
     * @param in
     *            the input stream. This is closed when done
     * @param out
     *            the output stream. This is <bold>not</bold> closed.
     * @throws IOException
     */
    public static void copy(InputStream in, OutputStream out)
            throws IOException {

        byte buffer[] = new byte[BUFSIZE];
        BufferedInputStream bis = new BufferedInputStream(in);
        BufferedOutputStream bos = new BufferedOutputStream(out);
        for (int n = bis.read(buffer, 0, BUFSIZE); n > 0; n = bis.read(buffer,
                0, BUFSIZE))
            bos.write(buffer, 0, n);
        bos.flush();
        bis.close();
    }

    public InputStream stream() {
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    public int size() {
        return bytes.size();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        else if (other instanceof Binary) {
            return bytes.equals(((Binary) other).bytes);
        } else
            return false;
    }

    public byte[] bytes() {
        return bytes.toByteArray();
    }

}