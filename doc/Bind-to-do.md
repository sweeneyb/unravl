Below are some (possible) future enhancements to `"bind"`:


## xml

TODO

```
 { "xml" : XPath, "class" : className }
 { "xml" : XPath, "class" : array-of-classNames }
```

The `"xml"` binder will bind (a fragment of) the XML response body to a Java object,
identified via an XPath expression, using JAXB and place it in the environment.
Use the JSONPath "/" for the entire body.

If the target class or class array is omitted, the XML org.w3c.dom.Node will be stored.

## xPath

TODO

Binds values from the XML response by extracting data via their XPath.

