The following are possible fture [assertion](Assertions.md) elements:

### xml ###

**TODO**

Asserts that the response body matches an XML.

Since UnRAVL is encoded as JSON, there is no way to embed
native XML text in an UnRAVL script as there is with a JSON body.
Validating an XML response
is done by encoding the XML body in a String (awkward for XML).
or with a @ reference to a file or URL. The <code>"xml"</code> body
spec operates just like <code>"text"</code> but the net result must be
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

**TODO**: add a <code>"literal" : true</code> or other option to suppress environment substitution
in external resources.

### XML schema ###

**TODO**

Assert that the body or variable is XML and that it conforms to the specified
XML schema

This will reuse the "schema" assertion defined
above but will auto detect if the referenced schema is an
XML schema (i.e. the location is .xsd or the value is
a compiled XML schema object, not an JSON schema object).

### jsonPath ###

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

Assert that the values at one or more <code>*jsonPathExpressions*</code>
matches a <code>*value*</code>. The <code>*value*</code> may be any JSON value. Strings in the value expression
are subject to  environment substitution.

The value could be a JSON number, string, boolean, array, or object.

**TODO**: augment to allow environment substitution for numbers,
booleans, etc.

**TODO**: add a "source" : value
attribute to allow testing another JSON object instead of the response body.

### jsonPathMatch ###

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

## equal ##

<strong>Warning</strong> The <code>"equal"</code> assertion is deprecated due to some ugly
issues with it. It will probably be removed before the 1.0.0 release of UnRAVL.
It is of less value since it is faily easy to do similar (and more precise) comparisons with
the <code>"groovy"</code> or <code>"javascript"</code> assertions.

Asserts that two values are equal. There are two possible forms for this assertion:

```
 { "equal" : [ lhs, rhs ] }
 { "equal" : [ lhs, rhs, epsilon ] }
```

The lhs and rhs values are compared and if not equal, the assertion throws an UnRAVLAssertionException. The values may be JSON null, booleans, integers, strings, doubles, JSON arrays, or JSON objects. The values should be the same type. Environment expansion is performed on all string values (top-level or nested inside JSON arrays or objects), with the exception of JSON field names which are not expanded. Note that this means the string value of variables will be compared.

If the optional *epsilon* value exists, it should be a floating point value and the lhs and rhs values are compared as doubles and must be within epsilon of each other. If the lhs or rhs values (after environment expansion) are string values, the value is converted to a double via <code>Double.valueOf(String)</code>

**TODO**: Allow passing multiple equality tests.
This is ambiguous right now.

```
{ "equal" : [
              [ expectedA, actualA ],
              [ expectedB, actualB ]
   ]
 }
```
Does this mean you want to assert that the two arrays are equal,
or that that tou want to run two sets of equal assertions, each comparing two values?
