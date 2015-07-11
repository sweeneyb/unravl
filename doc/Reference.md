UnRAVL is a ***Uniform REST API Validation Language*** - a JSON domain-specific language|domain-specific language (DSL) for validating REST APIs.

UnRAVL is a domain-specific language, coded in JSON, for validating REST APIs.
UnRAVL scripts consist of a JSON description of a REST API call:

1. HTTP method (<strong>`GET, POST, PUT, DELETE, HEAD, PATCH`</strong>)
1. URI
1. HTTP headers (optional)
1. Request body (optional)
1. Authentication (optional)

For each API call, an UnRAVL script may contain one or more
assertions which validate the results. Some assertions may be expressed as preconditions, which must be true before
making the API call. You can assert:

1. The result body matches expected JSON, text or other data
1. Specific headers exist with specific values
1. HTTP status code is a specific value or is in a specific set
1. Response body matches benchmarks
1. A groovy expression, testing elements of the response or environment, is true
1. A value in the environment is assigned

UnRAVL also supports extracting data from a REST API call's results,
binding those values to variables in the environment,
and using those values for future API call validations.
For example, you can save the <strong>`Location`</strong> header
from a <strong>`POST`</strong> call that creates a resource
and use that URL in a future <strong>`GET`</strong>, <strong>`PUT`</strong>, or <strong>`DELETE`</strong>.

A template facility provides reusable API validation constructs.

UnRAVL was designed and implemented by [https://github.com/DavidBiesack David Biesack]

## UnRAVL script syntax ##

An UnRAVL script contains the following elements.
Most are optional.

1. test name
1. test doc string; see Comments
1. template name
1. `env` block for setting variables in the environment
1. `preconditions`
1. `if` conditional execution
1. request `headers`
1. methodnd URI
1. request `body`
1. `bind` for extracting results into the environment and binding values
1. `assertions`

```
{ "name" : "test name",
  "template" : "template-name",
  "env" : { env-bindings },
  "preconditions" : [ preconditions ],
  "if" : condition,
  "headers" : { request-headers }
  method : URI
  "body" : { body-specification }
  "bind" : [ api-bindings ]
  "assert: [ assertions ]
}
```

In addition, a script file maybe a JSON array
of script objects:
```
[ { "name" : "test1", ... },
  { "name" : "test1", ... },
  ...
]
```

If a <code>"@file-or-URL"</code> names a file (not a URL), it is relative to the current directory
(not the directory where the script was found.)

### preconditions ###

Preconditions are Boolean expressions which are evaluated before
the API call. Preconditions must be true in order for the API call to
occur; if false, they result in assertion errors.
See assertions below

### if ###

The <code>"if"</code> condition is an element which allows you to control
conditional execution of the script. If the condition is true,
the script executes; if false, the script is skipped.

The condition is evaluated after the "env" element and after the "preconditions" element are evaluated, but
before the <code>"body"</code>,  <code>"headers"</code>,
<code>"GET"</code>...<code>"DELETE"</code>, <code>"bind"</code> or <code>"assert"</code> elements.
Thus, the condition expression can use values bound in the <code>"env"</code> element.

If there is no "if" element , the implicit condition is "failedAssertionCount == 0" - that is, the script will not run if any assertions have failed.

Unlike preconditions, a false condition does not
result in an assertion error and script failure;
the script is simply skipped instead.

Format:
```JSON
  "if" : condition
```

Condition may be:
; true : (the JSON true literal). The script will continue
; false : (the JSON false literal). The script will stop executing
; string: the value can be the name of UnRAVL environment variable which can be a Boolean object or a JSON BooleanNode value; if true, the script executes.
Else, the value is evaluated as a Groovy expression and if true, the script executes.

#### Example ####

```JSON
{
  "if" : true,
  "DELETE" : "{resourceLocation}"
}
```

Execute the script unconditionally.
You can use this to force execution of a script.
For example, if a compound script creates a resource that should be deleted,
you can use this to ensure the resource is deleted even if other assertion
or preconditions fail in early scripts.

```
{
  "if" : "exists",
  "PUT" : "{resourceLocation}",
   "headers" : { "Accept" : "application/json" },
  "body" : { "json" : { ... } },
}
```
Call the PUT method API call if the value of the variable <code>exists</code>
is true. This assumes the variable was previously bound (typically
by a <code>{ "bind" : { "groovy" : { ... } } }</code> element.

### method and URI ###

The UnRAVL script specifies the API call with the method name and URL

```JSON
  method : URI
```

the method and URI are strings. The method must be one of
 "GET", "HEAD", "POST", "PUT", "DELETE", "PATCH"

Examples:

```JSON
  "GET" : "http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false"
  "DELETE" : "http://www.example.com/rest/myService/myCollection/xk4783"
```

The URI is subject to environment substitution.

```JSON
  "DELETE" : "{BASE_URL}/rest/myService/myCollection/{itemId}"
```

will replace {BASE_URL} and {itemId} with the values of those
variables in the current environment.

### Request body ###

For "PUT", "POST" and "PATCH" methods, the request body can be passed in  multiple ways.

 "body" : { body-specification }

There are several ways to specify the request body.

#### json ####

To pass JSON, simply supply the JSON object or array:

```
  { "json" : json-object-or-array }
```

The *json-object-or-array* can contain variable references as per
Environments below.

You can also name a variable that is bound to a JSON object or array:

```JSON
  { "json" : "varName" }
```

Finally, you can read the JSON from a file or URL and
pass the resulting JSON as the request body. (The test
will fail if the referenced resource does not exist
or cannot be parsed as JSON.)

You can also name a variable that is bound to a JSON object or array:

```JSON
  { "json" : "@file-or-url" }
```
The referenced JSON resource can contain variable references
which will be expanded as per
Environments below.

In addition, if the body does not match any other body generator, such as
: <code>{ "json" : *json-body-specification* }</code>
: <code>{ "text" : *text-body-specification* }</code>
: <code>{ "binary" : *binary-body-specification* }</code>
then the entire value of the "body" item is used as a JSON body.
For example,
```JSON
  "body" : { "x" : 0, "y" : 1, "z" : -1 }
```is processed as if it were
```JSON
  "body" : { "json": { "x" : 0, "y" : 1, "z" : -1 } }
```
Using <code>{ "json" : *json-body-specification* }</code> is safer since it avoid conflict with body generator
names that may be added in the future, but the direct JSON body
is more concise and easier to use.

#### text ####

To pass plain text, supply a string or an array of strings:

```JSON
  { "text" : "text-string" }
```

or
```JSON
 { "text" : "some text\nanother line\na third line\n" }
```

or

```
  { "text" : [ "line 1",
               "line 2",
               ...
               "line n" ]
  }
```

When using an array of text, newlines are inserted only **between** elements.
If you want/require a closing newline, add an empty string "" to the end of the array.

Text (including JSON and XML) will be encoded as UTF-8.

If the string starts with @ it is assumed to be the name of a relative file or a URL
from which the text is read.

Multiple streams can be concatenated and/or mixed with
literals by mixing plain strings and @ strings in an array.

```
  { "text" : [ "@{templates}/preamble.txt",
               "some text",
               "using {variable} {substitution}"
               ...
               "@{templates}/closing.txt" ]
   }
```

The text can contain variable references as per
Environments below, including in @paths.

At present, the text source is the only way to PUT or POST XML content;
the JSON notation for UnRAVL scripts does not allow directly embedding raw XML text.
The text value may contain XML, or an array of strings, or @strings that reference
external files or URLs that contain XML content.

There is no way to specify that the body is generated by a Groovy
script, but that would be a natural next step.

#### binary ####

This form is for passing binary data as the request body.

```JSON
  "binary" : array-of-bytes
```

```JSON
  "binary" : "@binary-file-or-url"
```

### Request headers ###

Use the <code>headers</code> element to specify
one or more headers.

```JSON
  "headers" : headers
```

The elements is a JSON object consisting of one more more header name and body values.
The values may use environment substitution.

```JSON
  "headers" : { "Content-Type" : "application/json",
                "If-Unmodified-Since" : "{lastMod}" }
              }
```

This shows passing two headers, <strong>`Content-Type`</strong> and <strong>`If-Unmodified-Since`</strong>.
The value of the latter is taken from the environment.

Header names are case-insensitive but using Hyphenated-Upper-Camel-Case
is the convention.

## assertions ###

Assertions are what test the API.
There are two sets of supported assertions:
"preconditions" and "assert". Preconditions
are evaluated **before** invoking the API and must
all pass before running the API. This can be used to
validate environment variables that may have been set
from other APIs calls in earlier tests.
"assert" assertions run after the API call and validate the result.

```JSON
 "preconditions" : array-of-assertions ,
 "assert" : array-of-assertions
```

Below is the set of assertions supported by UnRAVL.

#### status ####

Checks that the HTTP status code matches the expected response.

```
 { "status" : integer }
 { "status" : integer-array }
 { "status" : pattern }
```

Examples:
```JSON
 { "status" : 200 }
 { "status" : [200, 204] }
 { "status" : "2.." }
```

Warning: Unless there is an explicit { "status" : status-code } assertion,
UnRAVL will execute and implicit <code>{ "status" : "2.." }</code> assertion.
Thus, if a test expects a non-2xx status code, use an explicit <code>"status"</code>
assertion and not a <code>"groovy"</code> assertion such as <code>"status == 404"</code>.

#### json ####

Asserts that the response body matches the JSON value

```
 { "json" : json-object-or-array }
 { "json" : "@file-or-url" }
```

String values in the JSON are subject to environment substitution,
and anywhere when using a @file-or-url.

**TODO**: augment to allow environment substitution for numbers, booleans, etc.
in literal JSON.  We can't put naked env references in there, such as

 { "longitude" : {longitude} }

to expand into

 { "longitude" : 89.392 }

because the JSON is parsed *before* the environment is defined,
and the JSON parser will balk at <code>{longitude}</code> as invalid JSON.

**TODO**: add an option to allow the result to be a subset of the expected
value, or for the expected value to be a subset of the actual object.
I'm not sure the best way to express this so that it is clear
which is which. The current Jackson library does not implement
a subobject equality test.

**TODO**: add an option to ignore certain fields or JSON Path expressions.

#### text ####

Asserts that the response body matches the (usually) plain/text body.

```
 { "text" : "expected text" }
 { "text" : "@file-or-url" }
 { "text" : array-of-strings }
```

The text literals are encoded with Unicode and escape characters,
such as \n for newlines. Or, an array of strings will
be concatenated. Any string that begins with the '@' character
is assumed to be a file or URL reference and that content
streamed in.

Environment substition is applied to strings and external text.

Initially, only UTF-8 text would be allowed.

```JSON
 { "text" : "This is\na multiline\nresponse\n" }
 { "text" : "@file-or-url" }
 { "text" : [ "This is",
              "a multiline",
              "response",
              "" ]
```

**TODO**: add diagnostic to indicate where the text differs.

**TODO**: add a <code>"literal" : true</code> or other option to suppress environment substitution

**TODO**: add a <code>"charset" : "charset-name"</code> option to use another character set.
in external resources.

#### xml ####

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

#### bound ####

Asserts that one or more variables are bound in the environment.
This is a "safety valve" for a script, especially those which
expect variables to be defined via system properties.

```
    { "bound" : "var-name" }
    { "bound" : [ "var-name1", ..., "var-namen" ] }
```

#### jsonPath ####

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

#### jsonPathMatch ####

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

#### headers ####

Assert that one or more headers exist and have the
specified value matching a regular expression

```
  { "headers" :
     {
       "Header-Name" : "pattern",
       ...
       "Header-Name" : "pattern"
      }
 }
```
Header names are case-insensitive but Hyphenated-Upper-Camel-Case
is the convention.

Examples:

```JSON
    { "headers" :
      {
        "Content-Type" : "application/json;\\s*charset=UTF-8",
        "Transfer-Encoding": "chunked",
        "Cache-Control" : "public.*"
      }
    }
```

#### schema ####

Asserts that one or more JSON structures conform to a JSON schema. There are
several possible forms for this assertion:

: <code>{ "schema" : <var>schema</var> }</code>
: <code>{ "schema" : <var>schema</var>, "values" : <var>values</var> }</code>

<var>schema</var> may be:
1. a JSON object which represents an embedded JSON schema
1. the name of a variable that contains a JSON object
1. a string in the form of <code>"@location"</code> where <var>location</var> is the URI of the JSON schema. (Environment variables are expanded within the <var>location</var> location string.)

<var>values</var> may be
1. a string containing a single variable (the key <code>"value"</code> may be used instead of <code>"value"</code>)
1. an array of variable names
1. For forms 1 and 2, each such variable must be bound to a JSON object or array. The JSON value of the variable is validated against the above referenced JSON schema.

If <code>"values"</code> is omitted, the the current response
body will be validated against the schema.

The assertion fails if any value does not
conform to the JSON schema, or if
the elements do not have the forms described above
or if the referenced JSON schema is not a valid schema.

##### Examples #####

This example will invoke GET to fetch a Swagger document and validate it against
the Swagger 2.0 schema.

```JSON
{
  "name" : "Fetch a Swagger document and validate the JSON body against the Swagger schema.",
  "GET" : "http://swagger.na.sas.com/swagger/public/factoryMiner/v1/swagger.json",
  "assert" : { "schema" : "@https://raw.githubusercontent.com/swagger-api/swagger-spec/master/schemas/v2.0/schema.json" }
}
```

----

This example creates an UnRAVL template for fetching a resource (via the
variable resId and validating the response body against a JSON schema.
The next three scripts use the template, setting the resId variable
to different values each time.

Note: At present, the UnRAVL "schema" assertion does **not**
cache JSON schema objects, so this will reread, parse, and process
the JSON schema object for each API call.

```JSON
[
  {
     "name" : "resourceX schema validation.template",
     "env"  : {
	        "schema" : "http://www.example.com/api/schemas/resourceX.json",
		"resId" : "<< Rebind this to a specific resurce id >>"
	       },
     "GET" : "http://www.example.com/api/resources/{resId}",
     "assert" : { "schema" : "@{schema}" }
   },
   {
     "template" : "resourceX schema validation.template",
     "env" : { "restId" : "r123" }
   },
   {
     "template" : "resourceX schema validation.template",
     "env" : { "restId" : "r456" }
   },
   {
     "template" : "resourceX schema validation.template",
     "env" : { "restId" : "r789" }
   }
]
```

----

This example is another way to do the previous test, to validate
that the result of three GET calls all conform to the JSON schema.
This one binds the response bodies (as JSON object) to three
different variables, then validates that the bound JSON objects
conform to the schema. It has the benefit of parsing and
processing the JSON schema only once.

```JSON
[
  {
     "GET" : "http://www.example.com/api/resources/r123",
     "bind" : { "json" : "r123" }
   },
  {
     "GET" : "http://www.example.com/api/resources/r456",
     "bind" : { "json" : "r456" }
   },
  {
     "GET" : "http://www.example.com/api/resources/r789",
     "bind" : { "json" : "r789" }
   },
   {
     "assert" : { "schema" : "http://www.example.com/api/schemas/resourceX.json",
                  "values" : [ "r123", "r456", "r789" ] }
   }
]
```

----

**TODO**: add a member to the "schema" element to save the parsed/loaded
JSON schema object in the environment, for later use.

#### XML schema ####

**TODO**

Assert that the body or variable is XML and that it conforms to the specified
XML schema

This will reuse the "schema" assertion defined
above but will auto detect if the referenced schema is an
XML schema (i.e. the location is .xsd or the value is
a compiled XML schema object, not an JSON schema object).

#### groovy ####

This assertion allows you to execute Groovy script code
to perform more complex validation and assertions.

```
    { "groovy" : groovy-script }
    { "groovy" : [ script-line,
                   ...
                   script-line,
                   ] }
```

groovy-script and each script-line script must be a string.
In the first form, the groovy-script is evaluated as
a Boolean expression. In the second form, all the lines
are combined with a newline separator and the resulting
string is evaluated as a Groovy script; the result
must also be a Boolean value.

Note that double quote characters in the script must be escaped
as \", but you can use single quotes to quote strings:
  "lastName == 'Biesack'"
Also, non-ASCII Unicode characters can be
expressed as \uxxxx (four hex digits), as per the JSON syntax rules.
(The entire unRAVL script must be UTF-8.)

If the string or one of the lines in the array form
starts with @ then the value is assumed to be the name
of a (relative) file resource or a URL and the Groovy script or fragment
is downloaded from there.

Tip: Avoid absolute path names. Use relative path names, and use
portable path notation, namely forward slashes which work on
both Linux **and** windows. Make sure the filename case is correct;
Windows will ignore case differences, but Linux will not.

The values in the current environment are passed to Groovy scripts.
Groovy assertions are often used in conjunction with binding
the JSON result of the API call to a variable using the
"json" extractor, which will parse the JSON response
and put it in a
[<code>org.fasterxml.jackson.databins.JsonNode</code>](http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/JsonNode.html) (using the Jackson JSON parser).

The Groovy is evaluated and if the result is a Boolean,
the assertion is true iff the Boolean value is true.
Warning: Other types are ignored.

The Groovy script may also throw an java.lang.AssertionError
to indicate a failed assertion.
A java.lang.RuntimeException results in a failed
assertion but also result in test errors.

Note that if you wish to compare *values* in a JSON object,
you must extract values with <code>.textValue() , .doubleValue(),
.longValue(), .intValue(), .booleanValue(),</code> etc.

For example, assuming the response body has been saved in a variable
named <code>result</code> in the current environment, an assertion such as
 "result[0].type == 'Folder'"
will always be false, even if the *type* field of the 0<sup>th</sup>
element of the JsonNode *result* has the value <code>"Folder"</code>, because
the type of <code>JsonNode</code> is a Jackson TextNode, not a <code>String</code>. Thus, the comparison is always false.

Instead, use

 "result[0].type.textValue() == 'Folder'"

In addition, if any element of the "assert" or "preconditions" arrays
are simple text strings, they are interpreted as groovy assertions.
Thus,
```JSON
 "assert" : [
      "projectId > 0",
      "projectId != lastProjectId"
 ]
```
is shorthand for embedding each of the expression in <code>{ "groovy" : expression}</code> element:

```JSON
 "assert" : [
      { "groovy" : "projectId > 0" },
      { "groovy" : "projectId != lastProjectId" }
 ]
```
Warning: Unless there is an explicit <code>{ "status" : status-code }</code> assertion,
UnRAVL will execute and implicit <code>{ "status" : "2.." }</code> assertion.
Thus, if a test expects a non-2xx status code, use an explicit <code>"status"</code>
assertion and not a <code>"groovy"</code> assertion such as <code>"status == 404"</code>.

#### javascript ####

The javascript assertion works just like the groovy assertion
described above, except that the expression is interpeted by
the JVM's JavaScript (Rhino) interpreter.

```
    { "groovy" : groovy-script }
    { "groovy" : [ script-line,
                   ...
                   script-line
                   ] }
```

Note that there are several differences. Groovy is a scripting
language designed to interoperate with Java, whereas JavaScript
is actually a different language. UnRAVL runs in the JVM
and interprets Java through the Rhino JavaScript engine.

For example, JavaScript strings are not the same as Java strings.
Thus, while a Groovy assertion

```JSON
  { "groovy" : "text.endsWith('.html'" }
```
may work (because Groovy uses Java's String object API),
the assertion

```JSON
  { "javascript" : "text.endsWith('.html'" }
```
will not work because the JavaScript String class
does not have the endsWith method that Java's String class has.

#### true and false ####

The special assertions "true" and "false"
are shorthand for Groovy expressions which
*must* evaluate to Boolean values;
the "true" assertion passes iff the Boolean value is true, and
the "false" assertion passes iff the Boolean value is false.

```
    { "true" : groovy-script }
    { "false" : groovy-script }
```

#### equal ####

<strong>Warning</strong> The equals assertion is deprecated due to some ugly
issues with it.

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
Does this mean I want to assert that the two arrays are equal,
or that I want to run two sets of equal assertions, each comparing two numbers?

#### ignore and doc ####

Useful to "comment out" an existing assertion in an UnRAVL, since JSON does not support comment syntax. For example, if you have the assertion
```JSON
 "assert" : [
     { "json" : "@benchmark.json" },
     { "status" : 201 }
     ]
```
and you wish to ignore the "json" assertion, but retain it for future use, change it into an ignore:
```JSON
 "assert" : [
     { "ignore" : { "json" : "@benchmark.json" } },
     { "status" : 201 }
     ]
```
This may also be used as a "doc" element, to allow arbitrary JSON documentation inside an "assert" or "preconditions":
```JSON
"assert" : [
     { "doc" : "Verify that the POST returns 201 Created status to indicate it successfully created a new resource." },
     { "status" : 201 }
     ]
```

### Authentication ###

Authentication is optionally specified with an <code>"auth"</code> element within a script.

Tip: You can put the <code>"auth"</code> member in a template and all scripts which inherit from that template will use that authentication method.

For best security, the credentials for authentication are stored in
the file <code>.netrc</code> in the current directory (if it exists),
or in the user's home directory (<code>~/.netrc</code> on Linux,
or <code>%USERPROFILE%\_netrc</code> on Windows, for example).
Users can protect these files using file system security.
For example, on Linux:
```
 chmod go-rwx .netrc
```
will not allow others users to read or write the <code>.netrc</code> file.

The format of the file is a simplified version of the standard
[Unix netrc file format](http://www.lehman.cuny.edu/cgi-bin/man-cgi?netrc+4).

Warning: The [<code>*default*</code> entry](http://www.lehman.cuny.edu/cgi-bin/man-cgi?netrc+4) and
[<code>*macdef*</code>](http://www.lehman.cuny.edu/cgi-bin/man-cgi?netrc+4) are not supported.}}
Credentials must be specified entirely on one line:

 machine *hostname* login *userid* password *password*

such as

 machine rdcesx51019.race.sas.com login sasdemo password sasDemoSecret123

The *hostname* field must exactly match the hostname in UnRAVL API calls.

You may also embed the credentials directly inside the authentication element in the script.
These may be the login id and password (if there are no security issues with directly embedding
the credentials in the script).

```JSON
  "auth" : { "basic" : true,
             "login" : "sasdemo",
             "password" : "sasDemoSecret123" }
```
Your script <code>"auth"</code> elements may also use UnRAVL environment variable
substitution. For example, you can pass the credentials at startup:

```
 java -Dhostname.login=sasdemo -Dhostnme.password=sasDemoSecret123 com.sas.unravl.Main ....
```

Warning: Doing this may leave the credentials readable in the process tables or to other tools that can read the currently running commands and all their parameters.

You may set them in Java system properties before creating the UnRAVL runtime
(for example, when running UnRAVL scripts in JUnit tests).
The scripts can then access the credentials from the environment, such as

```JSON
  "auth" : { "basic" : true,
             "login" : "{hostname.login}",
             "password" : "{hostname.password}" }
```

or

```JSON
  "auth" : { "cas" : "{casUrl}",
             "login" : "{hostname.login}",
             "password" : "{hostname.password}" }
```

If the <code>"login"</code> is embedded but no <code>"password"</code>,
UnRAVL will look up the password for that host/login pair in the <code>.netrc</code> file.

Tip: In older releases of UnRAVL, authentication was done with "basicAuth" and "casAuth" members in preconditions. This format is still supported but deprecated and will be removed in the future.

#### basic authentication ####

Basic Authentication locates credentials for the REST API call host
via the .netrc file (see above) and adds an
: <code>Authentication: Basic *encoded-credentials*</code>
header to the request.

The scriptlet form is
```JSON
  "auth" : { "basic" : true }
  "auth" : { "basic" : true, "login" : "testuserid" }
  "auth" : { "basic" : true, "login" : "testuserid", "password" : "testSecret" }
```

#### cas authentication ####

Central Authentication Service authentication will
login and acquire a Ticket Granting Ticket. For each API call
in an UnRAVL script, UnRAVL will request a Service Ticket
for that URL and append the Service Ticket to
the request URI.

The form is
```JSON
  "auth" : { "cas" : "logon-URL" }
  "auth" : { "cas" : "logon-URL", "login" : "testuserid" }
  "auth" : { "cas" : "logon-URL", "login" : "testuserid", "password" : "testSecret" }
```

Example:

```JSON
  "auth" : "cas" : "http://{sas.logon.host}:7980/SASLogon/rest/v1/tickets",
  "GET" : "http://{my.app.host}/SASMyApi/rest/myEndpoint/myResource"
```

UnRAVL will lookup the
The credentials for the {sas.logon.host} URL
in the file .netrc as described above, then use the full URL
"http://{sas.logon.host}:7980/SASLogon/rest/v1/tickets" to obtain a TGT.
Then using that TGT, UnRAVL will request a service ticket for the URL
"http://{my.app.host}/SASMyApi/rest/myEndpoint/myResource" (after expanding the
environment variables), then append the ticket. The net result will look something
like
 GET http://www.example.com/SASMyApi/rest/myEndpoint/myResource?ticket=ST-188763-kEcYVdVfAVYdmEyyfZWg-cas

The TGT is stored in the environment using <code>&lt;<em>hostname</em>&gt;.TGT</code>,
where <code>&lt;<em>hostname</em>&gt;</code> is taken from the <code>logon-URL</code>. The TGT
will be resused in other scripts that call the same host.

### Environments ###

Tests run within an *environment*, which is a mapping of name/value pairs,
or *bindings*. When executing an UnRAVL script, values can be captured
from the responses and stored in that environment. These environment values can be used
for specific assertion values. This is done by referring to bindings
to the script.

Variable names should be simple identifiers, so that they can be referenced
in Groovy code. However, UnRAVL also imports all system variables and
operating system environmentvariables, so some environment variables
may exist with names such as <code>os.name</code>, <code>user.name</code> and <code>user.dir</code>. Hwoever,
such variables are not available in Groovy scripts (but Groovy can directly access Java system properties via <code>System.getProperty(name)</code>.

An environment binding is *referenced* by using the <code><nowiki>{varName}</nowiki></code>
notation. (Do not use leading or trailing whitespace.)
The value of that binding is substituted.
(However, Groovy scripts do not need the braces as the environment bindings
are made directly available as Groovy variables.)

If a variable is not bound, this notation passes through; that is
the value of <code><nowiki>{undefinedVariable}</nowiki></code>
is <code><nowiki>{undefinedVariable}</nowiki></code>.

#### Automatically bound variables ####

; system properties
: at startup, all Java system properties (including values passed via <code>-Dprop=value</code>) are bound
; operating system environment variables
: at startup, all operating system environment variables are bound
; <code>name</code>
: the name of the currently executing script (from the <code>"name"</code> element of the script)
; <code>unravlScript</code>
: the UnRAVL script object currently executing
; <code>status</code>
: is always bound to the HTTP status of the latest API call.
; <code>responseBody</code>
: is bound to the response body for the <code>"json"</code>, <code>"text"</code>, and <code>"binary"</code> extractors (the JSON value, text response as a single <code>String</code>, or the bytes of the response as a <code>byte[]</code>, respectively)
; Unicode characters
: The special notation {U+nnnn} may be used to insert Unicode characters into text anywhere variable expansion is allowed.  You must supply four hex digits. For example,
:* <code>{U+002D}</code> will be replaced with the right curly (close) brace, <code>}</code>,
:* <code>{U+03C0}</code> will be replaced with the Unicode GREEK SMALL LETTER PI, &#x3c0;
: UnRAVL does not allow rebinding these values.

#### Alternate text for unbound variables ####

You may also provide alternate text to use if a variable is
not defined. Add a '|' vertical bar (pipe) character and the alternative text
before the closing brace:

<code>{varName|*alt text*}</code>

If <code>varName</code> is bound, the result will be the value of that variable and the *<code>alt text</code>* is discarded,
else the result will be *<code>alt text</code>*. The *<code>alt text</code>* can also
contain nested variable references. (Left and right curly braces must match inside the *<code>alt text</code>*.)

Processing of *<code>alt text</code>* is only supported for variable names
that consist of the following characters
: alphanumeric characters <code>a-Z A-Z 0-9</code>,
: '<code>.</code>' (period)
: '<code>_</code>' (underscore)
: '<code>$</code>' (dollar sign)
: '<code>-</code>' (dash)

This syntax restriction on names is so that other syntax using braces and vertical bars can pass
through directly. For example, an UnRAVL script may contain text such as
the following

   if (pending)
      { next = state|ACTIVE ? active : inactive; }

UnRAVL should not process this like a <code>{varName|*alt text*}</code>
expression. If it did, it would parse this as
: <code>varName == " next = state"</code>
: <code>*alt text* == "ACTIVE ? active : inactive; "</code>
Since there is no UnRAVL variable binding for a variable with the name <code> next = state</code>,
the result would be the <code>*alt text*, "ACTIVE ? active : inactive; "</code>
Thus, the net result would be the unexpected

   if (pending)
      ACTIVE ? active : inactive;

Warning: because of this *alt text* processing, some text may
be replaced even if you do not intend it to be interpreted as
variable references. For example, if A and D are not bound, the input text

   {A|B|C} | {D|E} is the same as {A|B|C|D|E}

will result in the text

   B|C | E is the same as B|C|D|E

Note: If you wish to include unbalanced left and right braces in <code>*alt text*</code>,
you may use Unicode replacement.  For example, if you want the value
of the variable <code>end</code>, but use <code>%}</code> if end is not defined, you cannot use
    {end|}}
because the first <code>}</code> will be used as the end of the variable reference and the <code>*alt text*</code> will be empty.
(If <code>end</code> is bound to the string <code>$</code> then <code>{end|}}</code> will result in <code>$}</code>,
and if <code>end</code> is not bound, the result will be <code>}</code>,
neither of which is not desired.)

Instead, use
    {end|%{U+002D}}
Here, the <code>*alt text*</code> is <code>%{U+002D}</code> which will expand to the desired <code>%}</code>.

#### Examples ####

Here is an example that shows binding values in an environment,
using them to invoke an API call, and binding values from the API results.
This is all very fluid and subject to change.

```JSON
{
  "name" : "GoogleEverestElevation",
  "env" : { "latitude" : 27.988056,
            "longitude" : 86.925278,
             "expectedElevation" :  8815.7158203125,
            "API_ROOT" : "http://maps.googleapis.com/maps/api/elevation"
            },
  "GET" : "{API_ROOT}/json?locations={latitude},{longitude}&sensor=false" },
  "bind" : [
     { "json" : "@{outputDir}/{name}.json" },
     { "headers" : [ "Content-Type", "contentType" ] },
     { "json" : "response" },
     { "jsonPath" : {
           "actualElevation" : "results[0].elevation",
           "actualLat" : "results[0].location.lat",
           "actualLong" :"results[0].location.lng",
           "actualStatus" "status" }},
     ],
  "assert": [
    { "status" : 200 },

    { "groovy" : [ "response.results[0].elevation.doubleValue() == {expectedElevation}",
                   "response.results[0].location.lat.doubleValue() == {latitude}",
                   "response.results[0].location.lng.doubleValue() == {longitude}",
                   "'OK' == actualStatus"
                 ]
    }
    ]
}
```

Here, the response body is saved in a file in the output directory with a file name based on the test name,
for later analysis/use, or for creating a benchmark for later validation.
The specific values from the body are bound to environment variables, actualElevation, actualLat, actualLong,
and actualStatus which may be used in assertions later.

The values in the current environment are passed
to the Groovy scripts as Groovy bindings, so the <code>{varName}</code> notation is not needed
in the expressions. If you use <code>{varName}</code> in a groovy expression, it will be substituted
before Groovy is interpreted, so it is useful to *generate* the Groovy source,
or to inject content into  Groovy string literals.

Note how the literal value <code>"OK"</code> may be quoted <code>'OK'</code>
in the Groovy assertion <code>"'OK' == actualStatus"</code>.

Note that values captured in the environment may be used in subsequent tests.

### Extractors ###

Extractors are what create new bindings in the environment
based on extracting data from an API response body and headers.

With the exception of <code>env|"env"</code>,
extractors are defined in the <code>"bind"</code> element of the Syntax|UnRAVL script:

  "bind" : [
      extractor_0,
      extractor_1,
      ...
      extractor_n
      ]

If you only have one extractor, you do not need to embed it in an array:

  "bind" : extractor_0

#### env ####

<code>"env"</code> is the default binding which runs *before* a test;
it is a top level element and should not be used in the "bind"
element (which runs after the API call.)

```JSON
"env" : collection-of-bindings
```

This is a collection of <code>"name" : *value*</code> pairs (a Map, if you will).

Example:

```JSON
  "env" : { "lat" : 27.988056,
            "longitude" : 86.925278,
            "API_ROOT" : "http://maps.googleapis.com/maps/api/elevation",
            "obj" : { "x" : 0, "y" : 10, "z" : -1 },
            "a" : [ 0, 1, 4, 9 ]
          }
```

Values may be any valid JSON type. The values are Jackson `com.fasterxml.jackson.databind.node.JsonNode`
instances: `BooleanNode`, `TextNode`, `DoubleNode`, `IntNode`, `LongNode`, `ObjectNode`, `ArrayNode`,
`NullNode`, etc.

#### headers ####

The <code>headers</code> element is used to extract text from response headers
into environment variables. The simplest form consists of a
JSON object of one or more name/header strings:

```
 { "headers" :  { "var" : "Header-Name", ..., "var" : "Header-Name" }  }
```
For example:

```JSON
 { "headers" :  { "cType" : "Content-Type", "loc" :"Location" }  }
```
which will bind the string value of the Content-Type and Location headers
to the variables named cType and loc.

The case of the header name is not significant.

In addition to this simplest binding, an array containing
the header name and a regular expression
may be specified instead of just the header name as a string.

```
   "var" : [ headerName, pattern, name1, ..., namen ]
```
For example, the header binding

```JSON
  { "header" : { "loc" : [ "Location", "{API_ROOT}/folder/(\\w+)/resources/(\\w+)", "folderId", "resourceId" ] } }
```

will save the <code>Location</code> header in the variable <code>loc</code>, then
matches a regular expression (which is first expanded with environment substitution for {API_ROOT})
and stores the first matching group to <code>folderId</code> and the second matching group to <code>resourceId</code>.

Note: The backslash character \ must be escaped in JSON: use <code>\\w+</code> if you want the regex pattern <code>\w+</code>, etc.

Thus, the format of the headers extractor is an array of arrays.

```
{ "headers" : [ array-of-strings, ..., array-of-strings ] }
```

In addition to the simplest binding, a regular expression format
is allowed:

```
[ headerName, name0, pattern, name1, ..., namen ]
```

**TODO**: Populate the default environment with some reusable
regular expression patterns, such as <code>{iso8601}</code> which is the pattern for an
ISO 8601 date/time value such as <code>2014-07-16T19:20:30.45+01:00</code>
and <code>{httpDate}</code> which is the pattern
for HTTP header date/time values such as <code>Fri, 01 Aug 2014 15:16:47 GMT</code>.
Each of these have one group for each element of the timestamp.

```JSON
{ "headers" : { "lastMod" : [ "Last-Modified", "{httpDate}", "dow", "dom", "mon", "year", "hh", "mm", "ss", "tz" ] } }
```

Tip: Do not use other matcher groups in the regular expression. Where necessary escape special regular expression characters like *, ?, and .

#### pattern ####

Matches text against grouping regular expressions and binds the substrings
into constituent variable bindings in the current UnRAVL script environment. The extractor form is

```
 { "pattern" : [ string, pattern, var0, ... varn ] }
```
such as
```
 { "pattern" : [ "{responseType}", "^(.*)\\s*;\\s*charset=(.*)$", "mediaType", "charset" ] }
```
This will match the value of the environment expansion of "{responseType}" to the given regular expression pattern <code>^(.*)\s*;\s*charset=(.*)$</code>, and bind the media type and the encoding character set  substrings to the variables <code>mediaType</code> and <code>charset</code>. (Note that a per the JSON grammar,
backslash (<code>\\</code>) characters in a JSON string must be escaped, so the regular expression notation <code>\s</code> is coded in the JSON string as <code>\\\\s</code>.)
For example, if the <code>responseType</code> binding in the environment was

 application/json; charset=UTF-8

this pattern specification will bind the variables:
<code>mediaType</code> to "<code>application/json</code>", and
charset to "UTF-8".
If the regular expression does not match, this extractor will throw an <code>UnRAVLAssertionException</code>

This extractor will unbind all the variables before testing the regular expression, so that bindings left from other tests won't persist and leave a false positive. See also the bound assertion to test if values are bound.

#### groovy ####

Run Groovy scripts and bind the results to variables in the environment.
This is like <code>"env"</code> extractor, but the values are not just JSON elements,
but Groovy scripts (encoded as strings).

```
 { "groovy" : { map-of-name-script-pairs } }
```

This is one way to convert strings to numbers, or to
extract elements of a JSON or XML response object.

```JSON
{ "groovy" : {
     "actualLat" : "jsonResponse.results[0].location.lat.doubleValue()",
     "actualLng" : "jsonResponse.results[0].location.lng.doubleValue()",
     "actualElevation" : "jsonResponse.results[0].elevation.doubleValue()"
     }
}
```

The right hand side scripts may be a simple string containing Groovy expression,
the notation "@file-or-URL" to indicate the Groovy should be read from the named file or URL,
or an array that combines the first two forms (after which the results are concatenated.)

The resulting text string is subject to environment substitution before
being interpreted as Groovy. All variables in the current environment are
available for use as local variables in the Groovy script.

#### javascript ####

Run javascript scripts and bind the results to variables in the environment.
This works like the "groovy" bind element above but uses JavaScript expressions
```
 { "javascript" : { map-of-name-script-pairs } }
```

See the note under the "javascript" assertion about difference
between Groovy and JavaScript.

#### jsonPath ####

**TODO**

Binds values from the JSON response by extracting data via their JSONPath.

```
 { "jsonPath" : { map-of-var-path-pairs } }
 { "jsonPath" : { map-of-var-path-pairs }, "from" : json }
 { "jsonPath" : { map-of-var-path-pairs }, "from" : "varname" }
```

The first form binds from the JSON response.
The second form may be used to name a variable in the environment.

```JSON
{ "jsonPath" : {
     "actualLat" : "results[0].location.lat",
     "actualLng" : "results[0].location.lng",
     "actualElevation" : "results[0].elevation"
     }
}
```

```JSON
{ "jsonPath" : {
     "actualLat" : "results[0].location.lat",
     "actualLng" : "results..lng",
     "actualElevation" : "results..elevation"
     },
   "from" : "jsonVarName"
}
```

The JSONPath strings are subject to environment substitution.

Note that many JSONPath expressions result in arrays of values
that match the path.
TODO: Decide if we need this or if using "groovy" will be sufficient.

#### xPath ####

TODO

Binds values from the XL response by extracting data via their XPath.

#### text ####

This binds the response body to a variable or writes it to a file.

```
 { "text" : "varName" }
 { "text" : "@file", "pretty" : boolean }
```

The file name "-", as in
<code>{ "body" : "@-" }</code>,
denotes standard output.

##### To do #####

If pretty is true, then the output will be pretty printed.
The Content-Type will be used to determine how to pretty print
If the content matches ".*[/+]json", it is pretty printed as JSON.
If the content matches ".*[/+]xml", it is pretty printed as XML.
<!-- If the content matches ".*[/+]x?html", it is pretty printed as HTML/XML. -->

#### json ####

```
 { "json" : "@file-name" }
 { "json" : "var" }
 { "json" : "var", "class" : class-name }
```

Parses the response body as a JSON object or JSON array.

It is an error if the response body cannot be parsed as JSON.

using the Jackson 2.2 JSON parser and the result is bound to a
[<code>org.fasterxml.jackson.databins.JsonNode</code>](http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/JsonNode.html) as a variable in the current
Environment.

TODO: If the target <code>class</code> or class array is present,
Jackson will be used to bind the result to an instance of that class,
and the resulting Java object will be stored in the variable.
The class must be accessible in the current classpath.
This may not be used with the "@file-name" target.

#### xml ####

TODO

```
 { "xml" : XPath, "class" : className }
 { "xml" : XPath, "class" : array-of-classNames }
```

The xml binder will bind (a fragment of) the XML response body to a Java object,
identified via an XPath expression, using JAXB and place it in the environment.
Use the JSONPath "/" for the entire body.

If the target class or class array is omitted, the XML org.w3c.dom.Node will be stored.

#### binary ####

This binds the response body to a variable as a byte[] array,
or writes it to a file.

```JSON
 { "binary" : "varName" }
 { "binary" : "@file" }
```

The content is copied exactly as 8-bit binary bytes, with no default encoding.
As binary content, the output cannot be streamed to stdout with "@-"
as with the "text" extractor.

#### links and hrefs ####

Extract links via link relation names or link matchers.

```
 { "links" : matchers }
 { "links" : matchers, "from" : "path" }
 { "hrefs" : matchers }
 { "hrefs" : matchers, "from" : "path" }
```

Each *matcher* can be either a string (find the corresponding link with that link relation name),
an array of strings (bind multiple variables via multiple link relation names),
or a JSON object with pairs of link relation names/matchers.

Here's an example. GET a resource at URL stored in the var <code>{location}</code>, extract the hrefs for the links with the link relations `"rel' : "self"`, `"rel":"update"` and `"rel": "delete"` from that JSON response body's <code>"links"</code> array, and assert the location matches the <code>"href"</code> values of those three links:

```JSON
   { "name" : "Extract just the self URL",
     "GET" : "{location}",
     "bind" : [
                { "json" : "responseBody" },
                { "hrefs" : [ "self", "update", "delete"] },
              ],
     "assert" : [ "self == location",
                  "update == location",
                  "delete == location"
                ]
   }
```

The following will extract the <code>"self"</code>, <code>"update"</code>, and <code>"delete"</code>
links as link objects. Instead of extracting from the default variable "responseBody",
this extracts from the JSON object stored in "resource".

```JSON
   { "name" : "Extract just the self URL",
     "GET" : "{location}",
     "bind" : [
                { "json" : "resource" },
                { "links" : [ "self", "update", "delete"], "from" : "resource" }
              ],
     "assert" : [ "self.href.textValue() == location",
                  "self.method.textValue() == 'GET'",
                  "delete.href.textValue() == location",
                  "update.method.textValue() == 'PUT'",
                  "delete.href.textValue() == location",
                  "delete.method.textValue() == 'DELETE'"
                ]
```

Note that when using link objects (the "links" form),
you can access fields of the link objects with
Groovy expressions such as <code>self.href.textValue()</code>
and <code>self.method.textValue()</code>.

By default, <br />
"links" extracts links from the current JSON object stored in <code>responseBody</code>,
which is normally defined when using the <code>"json"</code> extractor.
You may use <code>"from"</code> to specify an alternate object that contains links,
such as <code>"from" : "responseBody.items[0]"</code>

These extractors work with
JSON responses that contain [atom:link](http://tools.ietf.org/html/rfc4287#section-4.2.7) representations. This is also compatible with [Collection+JSON](http://amundsen.com/media-types/collection/) format.

```
  {  ...,
     "links" : [
         { "rel" : "self",
           "method" : "GET",
           "href" : "http://www.example.com/orders",
           "type" : "application/json"
         },
         { "rel" : "search",
           "method" : "GET",
           "href" : "/orders?id={order_id}",
           "type" : "application/json"
         }
      ],
     ...
  }
```
We will refer to the above as the atom:link response.
To support Collection+JSON, the "links" may be embedded in the top level "collection" object.
Common atom:link members are rel, type, href.

The second format is the the
[Application Language](http://stateless.co/hal_specification.html Hypertext) (HAL)
representation by Mike Kelly which uses a "_links" member:

```
  { ...,
    "_links": {
         "self": { "href": "/orders" },
         "search": { "href": "/orders?id={order_id}" }
    }
  }

```

We will refer to this as the HAL response. Each HAl link contains only the href member.

The general form of the links (or hrefs) extractor is

```
  { "links" : { "var1" : selector1,
                "var2" : selector2,
                ...
                "varn" : selectorn } }

```
"var1" through "varn" are environment variable names which will be bound to the links to their corresponding selectors. Selectors may be:
; a string
: the string is the link relation
; a JSON object
: the link which matches all the members of this object (as regular expressions) will be selected. The values in the selector must be strings. This allows you to match on the link relation, method, type, and/or uri instead of just the link relation.

Instead of a JSON object, the value can be an array of strings, in which case each string is used as both the variable name and the link relation (selector) name. Thus,

```JSON
  { "links" : [ "self", "update", "delete" ] }

```
is equivalent to

```JSON
  { "links" : { "self"   : "self",
                "update" : "update",
                "delete" : "delete" } }
```

Finally, a single string value value may be used:
```JSON
  { "link" : "self" }
```

is equivalent to

```JSON
  { "link" : { "self" : "self" } }
```

(Note that "link" may be used instead of "links"; this is clearer for extracting a single link.)

##### Example: Extracting multiple links #####

Consider two different JSON responses, the atom:link response and the HAL response, as described above. The UnRAVL "bind" element

```JSON
  { "links" : { "self" : "self",
                "search" : "search" }
  }
```

when used with the atom:link response above will select links based on their "rel" member.
This will bind the variable "self" to the object
```JSON
  { "rel" : "self",
    "method" : "GET",
    "href" : "http://www.example.com/orders",
    "type" : "application/json"
  }
```

When used with the HAL response, this will bind "self" to the link object

```JSON
  { "href": "/orders" }
```

##### Example: Extracting from other sources #####

By default, this extractor works on the variable named "responseBody" which is bound when using the "json" extractor. However, you can use the optional "from" member to name another variable that is bound, or you can use a Groovy expression that returns a JsonNode. This is useful if you want to extract the links of nested objects. It is required for Collection+JSON nodes to select from the "collection" element inside the response, for example.
```JSON
  "bind" : [
             { "href" : { "coll" : "self" },
               "from" : "responseBody.collection" } ,

             { "href" : { "self0" : "self",
                          "delete0" : "delete" },
               "from" : "responseBody.collection.items[0]" } ,

             { "href" : { "selfLast" : "self",
                          "deleteLast" : "delete" },
               "from" : "responseBody.collection.items[responseBody.collection.items.size()-1]" }
           ]
```

this will extract the href from the link to the collection as well as the the href values from the "self" and "delete" links in the first and last element of the nested items array, respectively. Environment variable substitution is performed on the string before evaluating it as a Groovy expression.

##### Example: Complex matching #####

By default, if the selector is a string, this extractor only matches the link relation ("rel" value for atom:link content or the key for HAL content). This is also the only option for HAL. For atom:link, you may specify multiple matching criteria, using regular expression matches for one or more members of the link. For example, to match a link that has a "rel" value of "update" and a "method" value of "PUT" and a "href" label that contains "models", use

```JSON
  "bind" : { "link" : { "updateLink" : { "rel" : "update",
                                         "method" : "PUT",
                                         "href" : ".*models.*"
                                        }
                       }
            }
```

#### ignore and doc ####

Use this to comment out an extractor in a "bind" element, or to add documentation to the "bind" element. For example, to cause the <code>"json"</code> extractor to be ignore (not create out.json), change

```JSON
 "bind" : [
            { "json" : "@out.json" }
          ]
```

to

```JSON
 "bind" : [
            { "ignore" : { "json" : "@out.json" } }
          ]
```

or, to add a comment:
```JSON
 "bind" : [
            { "doc" : "write the response body as JSON to the file out.json" },
            { "json" : "@out.json" }
          ]
```

## Templates ##

If a test name ends with <code>.template</code> then it is assumed to be a *template*.
Templates are not executed but saved. Other tests can reuse saved templates
by specifying a `"template" : "template-name"` attribute.
The template name is subject to environment substitution},
so you can choose preconditions and assertions by dynamically
loading templates based on a variable binding.

The ".template" suffix is assumed if omitted.

All the environment settings, if conditions, request body and headers, API calls, bindings, and assertions
of the template are added to a test.  A test or template will evaluated these template
elements before its own. Note that "env" elements in templates may
overwrite variable bindings.

Templates can also refer to other templates, so you can chain capabilities.

The special template <code>implicit.template</code>, if defined, is applied to all
tests or templates which do not have a <code>"template"</code> element.
It can be used to define global assertions or defaults for the
entire test suite without having to repeatedly name the template
in each test.

TODO: Allow the "templates" value to be an array of template names

For example, to test that the Google elevation API is idempotent,
one could create a template for it, then run that multiple times
to assert the same response each time:

```JSON
[

  {
    "name" : "GoogleEverestElevationEnv",
    "doc" : "Define the target URL for GET calls",
    "env" : { "url" : "http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false" }
  },

  {
    "name" : "GoogleEverestElevation.template",
    "doc" : "Template which invokes the GET and asserts the response equals the contents of GoogleEverestElevation.json",
    "GET" : "{url}",
    "assert" : [
       { "json" :  "@GoogleEverestElevation.json" }
    ]
  },

  {
    "name" : "GoogleEverestElevationBenchmark",
    "doc" : "Template which invokes the GET and saves the response body to GoogleEverestElevation.json",
    "template" : "GoogleEverestElevation.template"
  },

  "GoogleEverestElevationBenchmark",
  "GoogleEverestElevationBenchmark",
  "GoogleEverestElevationBenchmark",

]
```

The first test binds <code>url</code> in the environment; the remaining tests use that
binding.
The second test is a template which performs a GET and asserts the
response body matches the JSON in the file <code>"@GoogleEverestElevation.json"</code>
(which will be created by the next text)

The third test calls the GET the first time (the method call is defined
in the template), also saves the response
body into the file <code>"@GoogleEverestElevation.json"</code>.
This test will also run the assertion (which will be true because
it is comparing the response body to the file it just created
from that response body.)

The next three tests simply repeat the previous test (by name) , each of which will
also use the template. Each will call the API,
check for a 2xx HTTP status (the default behavior), and then
run the assertions in the template to validate that the response
body, as JSON, matches the JSON in the file.

## Miscellaneous ##

### Script language ###

UnRAVL interprets some quoted expressions as Groovy expressions by default.
For example, the "if" guard for conditional execution of a script is defined as

```
   "if" : "expression"
```
If an element of the "assert" or "preconditions" elements
is a naked string, it is evaluated using Groovy:
```
  "assert" : [ "response != null",
               "response.size() > 10"
             ]
```
Also, the "links" and "hrefs" extractors in a "bind" element can use
a Groovy path expression to extract link objects.

You can override the default language (Groovy) by setting the system
property <code>unravl.script.language</code> to "javascript".
Java 7 and higher comes with a JavaScript engine. UnRAVL also includes
groovy-all which includes a Groovy script engine (the default).

The script language must have a corresponding assertion
class so that "assert" elements can be converted into
explicit assert objects. The above is converted to the equivalent
```
  "assert" : [ { "groovy" : "response != null" },
               { "groovy" : "response.size() > 10" }
             ]
```

If you want all such expressions to use JavaScript instead of
Groovy, use the setting

```
  export UNRAVL_OPT=-Dunravl.script.language=javascript
```
when you launch UnRAVL with the <code>bin/unravl.sh</code> script.

If you are instantiating an <code>UnRAVLRuntime</code>,
you can set the script language with
<code>runtime.setScriptLanguage("javascript");</code>

### Comments ###

Unfortunately, JSON does not provide any syntax for enclosing comments.

Although the Jackson parser has an option to enable Java-style /* ... */ and //
comments, not all JSON tools support this, so UnRAVL does not allow them.
Instead, each test (or template) may have "doc" elements which may be a string
or an array of strings, or actually any valid JSON. (This also allows
you to comment out a block of JSON UnRAVL script by wrapping it in a
<code>{ "doc" : argitrary-JSON-to-be-commented-out }</code> block

The UnRAVL script may have a "doc" comment string.
Many of the assertions and extractors also allow a "doc" string,
such as

```JSON
    { "jsonPath" : "status",
      "value" : "OK",
      "doc" : "assert that the response contains a 'status' with the value 'OK'"
    }
```

### Redirection ###

TODO

The default operation is to apply redirection if a 3xx responses is received.
For 300, 301, and 302, UnRAVL will retry with GET. For 303 and higher,
UnRAVL will retry with the same method.  (Via Apache HTTP Components.)

Redirect retries are controlled by environment variables
_maxRedirect (integer, valid values 0-5). If 0, no redirects are followed.

## JUnit integration ##

It is quite easy to run UnRAVL scripts from JUnit.
Place your scripts in a directory and use the
<code>com.sas.unravl.assertions.JUnitWrapper</code> class
to run all the tests in that directory.

For builds, you can add a dependency to UnRAVL as described in the Logistics section.
Then in a JUnit test method, run the tests in your test scripts directory.
See <code>src/test/java/com/sas/unravl/test/TestScripts.java</code> which runs the testsin
the UnRAVL source folder, <code>src/test/scripts</code> (but not in subdirectories)

## Plugins ##

The framework can support custom assertions, body generators,
and extractors by defining Java classes which implement the appropriate
interfaces or inherit from the base classes.
These are loaded via Spring component-scan and
dependency injection of an UnRAVLRuntime instance
through which the plug-in can register the plug-in
class.

Each such class use an annotation to define it's
shortcut code (such as "json" or "headers"
as used in the other assert objects above.)

For example, the "bound" assertion is defined as:

```Java
import com.sas.unravl.annotations.Assertion;
@Assertion("bound")
public class BoundAssertion extends BaseAssertion {
```

By convention, tag names should use lowerCamelCase.

Be careful to not use names that are already in use
or which could be confused with attributes of other assertions.

Plugins register themselves via the following called from the inherited
```Java
 @Autowired
 public void setRuntime(UnRAVLRuntime runtime)
```

metod in the base class. For example, BaseAssertion does:

```Java
    /**
     * Used to register the assertion class with the UnRAVLRuntime.
     * This is called from Spring when the UnRAVLRuntime class is loaded.
     * @param runtime a runtime instance
     */
	@Autowired
	public void setRuntime(UnRAVLRuntime runtime) {
		runtime.addAssertion(this.getClass());
	}
```

If you implement a plugin, inherit from the base class,
or else use the <code>@Autowired</code> annotation, implement this setter,
and call the appropriate <code>runtime.add*Plugin*</code> method.

## Logging ##

By default, UnRAVL will log the REST API calls' HTTP method and URI, request and response bodies,
request and response headers, and HTTP status codes.

You can control the level of logging by with command line argument used before
the scripts:
 unravl -q &lt;<em>scripts</em>&gt;
 unravl --quiet &lt;<em>scripts</em>&gt;
 unravl -v &lt;<em>scripts</em>&gt;
 unravl --verbose &lt;<em>scripts</em>&gt;

These options select an alternate Log4j configuration file, respectively:
--quiet
 The quiet configuration logs just the script name and the passed/failed assertion.
 (source in `src/main/resources/log4j-quiet.properties`)
--verbose
 the verbose configuration (TRACE) lists all the API call inputs, headers, and output, as well as the results : `log4j-trace.properties` (source in `src/main/resources/log4j-trace.properties`)
of the assertions
The default lists each scripts and the results of the assertions
([http://gitlab.sas.com/sasdjb/unravl/blob/master/src/main/resources/log4j.properties log4j.properties]).

You can configure more fine-grained logging other Log4J configuration files:

  <code>java -Dlog4j.configuration=your-log4j.properties ...</code>

## Logistics ##

UnRAVL is built with either [[Gradle]] or [[Maven]]. The Gradle wrapper is included in the project,
so no extra setup is required (unlike Maven)
to create a jar file <code>sas.unravl-<em>version</em>.jar</code>

* Gradle build puts the jar in ./build/libs
* Windows:
  * `gradlew.bat build`
* Linux
  * `./gradlew build`
* Maven build puts the jar in ./target
  * `mvn clean compile package`

## Running UnRAVL from the command line ##

First, copy all dependencies

`gradlew copyDeps`

to download the jars necessary to run UnRAVL with a classpath pointing to the dependcy directory

Run with

* Linux:
  *  `java -classpath 'build/libs/*:build/output/lib/*' com.sas.unravl.Main file.json ... file.json`
* Windows:
  *  `java -classpath 'build/libs/*;build/output/lib/*' com.sas.unravl.Main file.json ... file.json`

You may deploy all the jar files to a single filter named by UNRAVL_LIB and then run

 `java -classpath "$UNRAVL_LIB/*" com.sas.unravl.Main file.json ... file.json`

If your project uses Spring, you should augment your Spring configuration to include

 classpath:/META-INF/spring/unravlApplicationContext.xml


