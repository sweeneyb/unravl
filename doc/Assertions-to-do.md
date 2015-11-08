The following are possible fture [assertion](Assertions.md) elements:

### xml

**TODO**

Asserts that the response body matches an XML.

Since UnRAVL is encoded as JSON, there is no way to embed
native XML text in an UnRAVL script as there is with a JSON body.
Validating an XML response
is done by encoding the XML body in a String (awkward for XML).
or with a @ reference to a file or URL. The `"xml"` body
spec operates just like `"text"` but the net result must be
valid XML.

```
 { "xml" : "xml-body-encoded-as-a-string"
 { "xml" : "@file-or-url" }
 { "xml" : array-of-strings
```

Examples:

```JSON
 { "xml" : "<myResource x='an attribute' y=100><data>foo</data></myResource>" }
```

or using environment substitution:

```JSON
 { "xml" : "<myResource x='an attribute' y=100><data>{variableData}</data></myResource>" }
```

```JSON
 { "xml" : [ "<myResource an attribute' y=100>",
                "<data>",
                "@foo.data.txt",
                "</data>",
              "</myResource>"
              ]
```

as with "text",  environment substitution is also performed on string
literals and content read from files.

**TODO**: add a `"literal" : true` or other option to suppress environment substitution
in external resources.

### XML schema

**TODO**

Assert that the body or variable is XML and that it conforms to the specified
XML schema

This will reuse the "schema" assertion defined
above but will auto detect if the referenced schema is an
XML schema (i.e. the location is .xsd or the value is
a compiled XML schema object, not an JSON schema object).

### json

Extend the `"json"` assertion to allow simply containment checks,
and to compare to other JSON values, not just the JSON body.
See [Issue #21, *Extend "json" assertion to allow subset match; ignore order*](https://github.com/sassoftware/unravl/issues/21).

### jsonPath

Asserts that a value matches the JSON value identified by a JSONPath expression,
which refers to a value in the JSON response body.

```
    { "jsonPath" :
       { jsonPathExpression : value,
         jsonPathExpression : value,
         ...
         jsonPathExpression : value,
       }
     }
```

Assert that the values at one or more *`jsonPathExpressions`*
matches a *`value`*. The *`value`* may be any JSON value. Strings in the value expression
are subject to  environment substitution.

The value could be a JSON number, string, boolean, array, or object.

**TODO**: augment to allow environment substitution for numbers,
booleans, etc.

**TODO**: add a `"from"` : value
attribute to allow testing another JSON object instead of the response body.

### jsonPathMatch

**TODO**

```
    { "jsonPathMatch" :
       { jsonPathExpression : pattern,
         jsonPathExpression : pattern,
         ...
         jsonPathExpression : pattern,
       }
     }
```
Asserts that one or more values named by JSONPath expressions
(which must resolve to string values or an array of strings)
matches the given java.util.regex.Pattern patterns.

#
