UnRAVL is a domain-specific language, coded in JSON, for validating REST APIs.
UnRAVL scripts consist of a JSON description of a REST API call:

1. HTTP headers (optional)
1. Request body (optional)
1. HTTP method (<strong>`GET, POST, PUT, DELETE, HEAD, PATCH`</strong>)
1. URI
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

UnRAVL was designed and implemented by [David Biesack](https://github.com/DavidBiesack)

## UnRAVL script syntax

An UnRAVL script contains the following elements.

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

The order does not matter, as UnRAVL processes each element by keys.
All elements are optional.

The JSON syntax is as follows:

```
{ "name" : "test name",
  "doc" : "a comment describing this test",
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

In addition, a script file may be a JSON array
of script objects, script names, or script resource names (file or URLs):
```
[
  { "name" : "test1", ... },
  { "name" : "test2", ... },
  "test1",
  "@file-or-url",
  ...
]
```

If the element of the array is a simple string, it should be the name of a
script that has already been executed. If multiple scripts have the same
name, sequential execution order will replace the previous mapping of name-to-script
("last one wins".) If the "name" element is omitted, the script
is given a name based on the current local date and time.

If a `"@file-or-URL"` element names a file (not a URL) without an absolute file location,
it should reside relative to the current directory
(not the directory where the script was found.)

UnRAVL scripts also execute with an environment: a set of variable bindings
which can contain data to pass to API calls, data parsed from API responses,
and which may be used to perform validations/assertions.

Below are the structural elements of an UnRAVL script,
with syntax and descriptions.

### name

Use the optional `"name"` element to provide a meaningful, descriptive name for the test.
The name does not have to be an identifier; it may be a phrase or sentence.
You may use the name to invoke the test again.

```JSON
{
  "name" : "Simple test",
  "GET" : "http://www.example.com/api"
}
```

If a test name ends with `.template` then it is assumed to be a
**[template](Templates.md)**. It is not executed at the point it occurs,
but define reusable test structures.

### template

You can reuse an existing template by naming it:

```JSON
{
   "name" : "A more complex test",
   "template" : "myTemplate",
   "GET" : "http://www.example.com/api"
}
```

The operations (preconditions, assertions, environment, etc.)
defined by the template are inherited by the current test.

See [template](Templates.md) for more details.

### env

`"env"` is a set of variables which runs before a test.

```JSON
"env" : collection-of-bindings
```

This is a collection of `"name" : *value*` pairs (a Map, if you will).
Each `*value*` is bound to an variable named "name" in the current
environment. These values may later be used by referencing
them as `{name}` inside String values, or as values bound
when Groovy or JavaScript expressions are run.

Example:

```JSON
  "env" : { "lat" : 27.988056,
            "longitude" : 86.925278,
            "API_ROOT" : "http://maps.googleapis.com/maps/api/elevation",
            "obj" : { "x" : 0, "y" : 10, "z" : -1 },
            "a" : [ 0, 1, 4, 9 ]
          }
```

Values may be any valid JSON type. For scalar types (integers, long, doubles, strings, booleans, null),
the corresponding `java.lanag.{Integer, Long, Double, String, Boolean}` or `null` value
is assigned to the variable. For JSON Objects and Arrays, the values are Jackson `com.fasterxml.jackson.databind.node.{ObjectNode, ArrayNode}`.

UnRAVL will also bind the values in the test's template `"env"` block, if one is named.

Variables bound in the environment may be used elsewhere in UnRAVL
scripts: for building inputs or request headers or request bodies, or for validating the API
with assertions. See [Environment](#Environment) below for details on how variables
may be used.

### preconditions

Preconditions are Boolean expressions which are evaluated *before*
the API call. Preconditions must be true in order for the API call to
occur; if false, they result in assertion errors.

Syntax:

```JSON
  "preconditions" : precondition
  "preconditions" : array-of-precondition
```

See [Assertions](Assertions.md) for details on the syntax of preconditions
and assertions.

### if

The `"if"` condition is an element which allows you to control
conditional execution of the script. If the condition is true,
the script executes; if false, the script is skipped.

The condition is evaluated after the "env" and "preconditions" elements are evaluated (if present), but
before the `"body"`,  `"headers"`,
`"GET"`...`"DELETE"`, `"bind"` or `"assert"` elements.
Thus, the condition expression can use values bound in the `"env"` element.

If there is no "if" element , the implicit condition is "failedAssertionCount == 0" - that is, the script will not run if any previous assertions/preconditions have failed.

Unlike preconditions, a false condition does not
result in an assertion error or script failure;
the script is simply skipped instead.

Format:
```JSON
  "if" : condition
```

Condition may be:
* true : (the JSON true literal). The script will continue.
* false : (the JSON false literal). The script will stop executing.
* string: the value can be the name of UnRAVL environment variable which can be a Boolean object or a JSON BooleanNode value; if true, the script executes.
* If none of the above match, the value is evaluated as a Groovy expression and if true, the script executes.

For example:

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
  "body" : { ... },
}
```
Call the PUT method if the value of the variable `exists`
is true. This assumes the variable was previously bound (typically
by a `{ "bind" : { "groovy" : { ... } } }` element.

### headers

Use the `headers` element to specify
one or more request headers.

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

Header names are case-insensitive but using *Hyphenated-Upper-Camel-Case*
is the convention.

### auth

Authentication is optionally specified with an `"auth"` element within a script.
UnRAVL supports Basic authentication:
```
  "auth" : { "basic" : true }
```
and Central Authentication Service authentication:
```JSON
  "auth" : { "cas" : "logon-URL" }
```
Credentials may be supplied in a credentials file (recommended), passed
in the environment, in stored directly the script (discouraged).

See [Authentication](Authentication.md) for details.

### body

For "PUT", "POST" and "PATCH" methods, the request body can be expressed in multiple ways.

```
 "body" : body-specification
```
The *body-specification* may take one of several forms:

```
 "body" : json-request-body
 "body" : { "json" : json-request-body }
 "body" : { "text" : text-request-body }
 "body" : { "binary" : binary-request-body }
```

The different forms are described in [Body](Body.md).

### Method and URI

The UnRAVL script specifies the API call with the method name and URL.

```JSON
  method : URI
```

The *method* and *URI* are both strings. The *method* must be one of
`"GET", "HEAD", "POST", "PUT", "DELETE", "PATCH"`.
The *URI* is the REST API being tested.

Examples:

```JSON
  "GET" : "http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false"
  "DELETE" : "http://www.example.com/rest/myService/myCollection/xk4783"
```

The *URI* is subject to [environment substitution](#Environment):

For example,
```JSON
  "DELETE" : "{BASE_URL}/rest/myService/myCollection/{itemId}"
```

will replace `{BASE_URL}` and `{itemId}` with the values of those
variables in the current environment. Variables are assigned by
the `"[env](#env)"`,
`"groovy"`, or
`"javascript"` elements
or other `"[bind](#bind)"`
described below,


### bind

The "bind" element extract data from an API response body and headers
and store the values in variables that may be used to test the response.
Some bind elements also validate data, acting as implicit assertions.

  "bind" : [
      extractor_0,
      extractor_1,
      ...
      extractor_n
      ]

If you only have one extractor, you do not need to embed it in an array:

  "bind" : extractor_0

### assert

The "assert" element names one or more assertions to run
after the API call. Assertions are used to test the result of the API call:
the status code, the response headers, and the response body.

Syntax:

```JSON
  "preconditions" : precondition
  "preconditions" : array-of-precondition
```

See [Assertions](Assertions.md) for full details.

## Environment

Tests run within an *environment*, which is a mapping of name/value pairs,
or *bindings*. When executing an UnRAVL script, values can be captured
from the responses and stored in that environment. These environment values can be used
for specific assertion values. This is done by referring to bindings
to the script.

Variable names should be simple identifiers, so that they can be referenced
in Groovy code. However, UnRAVL also imports all system variables and
operating system environmentvariables, so some environment variables
may exist with names such as `os.name`, `user.name` and `user.dir`. Hwoever,
such variables are not available in Groovy scripts (but Groovy can directly access Java system properties via `System.getProperty(name)`.

An environment binding is *referenced* by using the `<nowiki>{varName}</nowiki>`
notation. (Do not use leading or trailing whitespace.)
The value of that binding is substituted.
(However, Groovy scripts do not need the braces as the environment bindings
are made directly available as Groovy variables.)

If a variable is not bound, this notation passes through; that is
the value of `<nowiki>{undefinedVariable}</nowiki>`
is `<nowiki>{undefinedVariable}</nowiki>`.

#### Automatically bound variables

* system properties
  * at startup, all Java system properties (including values passed via `-Dprop=value`) are bound
* operating system environment variables
  * at startup, all operating system environment variables are bound
* `name`
  * the name of the currently executing script (from the `"name"` element of the script)
* `unravlScript`
  * the UnRAVL script object currently executing
* `status`
  * is always bound to the HTTP status of the latest API call.
* `responseBody`
  * is bound to the response body for the `"json"`, `"text"`, and `"binary"` extractors (the JSON value, text response as a single `String`, or the bytes of the response as a `byte[]`, respectively)
* Unicode characters
  * The special notation {U+nnnn} may be used to insert Unicode characters into text anywhere variable expansion is allowed.  You must supply four hex digits. For example,
  * `{U+002D}` will be replaced with the right curly (close) brace, `}`,
  * `{U+03C0}` will be replaced with the Unicode GREEK SMALL LETTER PI, &#x3c0;
  * UnRAVL does not allow rebinding these values.

#### Alternate text for unbound variables

You may also provide alternate text to use if a variable is
not defined. Add a '|' vertical bar (pipe) character and the alternative text
before the closing brace:

`{varName|*alt text*}`

If `varName` is bound, the result will be the value of that variable and the *`alt text`* is discarded,
else the result will be *`alt text`*. The *`alt text`* can also
contain nested variable references. (Left and right curly braces must match inside the *`alt text`*.)

Processing of *`alt text`* is only supported for variable names
that consist of the following characters
* alphanumeric characters `a-Z A-Z 0-9`,
* '`.`' (period)
* '`_`' (underscore)
* '`$`' (dollar sign)
* '`-`' (dash)

This syntax restriction on names is so that other syntax using braces and vertical bars can pass
through directly. For example, an UnRAVL script may contain text such as
the following

```
   if (pending)
      { next = state|ACTIVE ? active : inactive; }
```

UnRAVL should not process this like a `{varName|*alt text*}`
expression. If it did, it would parse this as

`varName == " next = state"`
`*alt text* == "ACTIVE ? active : inactive; "`

Since there is no UnRAVL variable binding for a variable with the name ` next = state`,
the result would be the `*alt text*, "ACTIVE ? active : inactive; "`
Thus, the net result would be the unexpected

```
    if (pending)
       ACTIVE ? active : inactive;
```
Warning: because of this *alt text* processing, some text may
be replaced even if you do not intend it to be interpreted as
variable references. For example, if A and D are not bound, the input text

```
    {A|B|C} | {D|E} is the same as {A|B|C|D|E}
````

will result in the text

```
    B|C | E is the same as B|C|D|E
```

Note: If you wish to include unbalanced left and right braces in `*alt text*`,
you may use Unicode replacement.  For example, if you want the value
of the variable `end`, but use `%}` if end is not defined, you cannot use
    {end|}}
because the first `}` will be used as the end of the variable reference and the `*alt text*` will be empty.
(If `end` is bound to the string `$` then `{end|}}` will result in `$}`,
and if `end` is not bound, the result will be `}`,
neither of which is not desired.)

Instead, use
```
    {end|%{U+002D}}
````
Here, the `*alt text*` is `%{U+002D}` which will expand to the desired `%}`.

#### Examples

Here is an example that shows setting values in an environment,
using them to invoke an API call, and binding values from the API results.

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
     { "headers" : { "contentType" : "Content-Type" } },
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
to the Groovy scripts as Groovy bindings, so the `{varName}` notation is not needed
in the expressions. If you use `{varName}` in a groovy expression, it will be substituted
before Groovy is interpreted, so it is useful to *generate* the Groovy source,
or to inject content into  Groovy string literals.

Note how the literal value `"OK"` may be quoted `'OK'`
in the Groovy assertion `"'OK' == actualStatus"`.

Note that values captured in the environment may be used in subsequent tests.

## Miscellaneous

### Script language

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
property `unravl.script.language` to "javascript".
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
when you launch UnRAVL with the `bin/unravl.sh` script.

If you are instantiating an `UnRAVLRuntime`,
you can set the script language with
`runtime.setScriptLanguage("javascript");`

### Comments

Unfortunately, JSON does not provide any syntax for enclosing comments.

Although the Jackson parser has an option to enable Java-style /* ... */ and //
comments, not all JSON tools support this, so UnRAVL does not allow them.
Instead, each test (or template) may have "doc" elements which may be a string
or an array of strings, or actually any valid JSON. (This also allows
you to comment out a block of JSON UnRAVL script by wrapping it in a
`{ "doc" : argitrary-JSON-to-be-commented-out }` block

The UnRAVL script may have a "doc" comment string.
Many of the assertions and extractors also allow a "doc" string,
such as

```JSON
    { "jsonPath" : "status",
      "value" : "OK",
      "doc" : "assert that the response contains a 'status' with the value 'OK'"
    }
```

### Redirection

TODO

The default operation is to apply redirection if a 3xx responses is received.
For 300, 301, and 302, UnRAVL will retry with GET. For 303 and higher,
UnRAVL will retry with the same method.  (Via Apache HTTP Components.)

Redirect retries are controlled by environment variables
_maxRedirect (integer, valid values 0-5). If 0, no redirects are followed.

### JUnit integration

It is quite easy to run UnRAVL scripts from JUnit.
Place your scripts in a directory and use the
`com.sas.unravl.assertions.JUnitWrapper` class
to run all the tests in that directory.

For builds, you can add a dependency to UnRAVL as described in the Logistics section.
Then in a JUnit test method, run the tests in your test scripts directory.
See `src/test/java/com/sas/unravl/test/TestScripts.java` which runs the testsin
the UnRAVL source folder, `src/test/scripts` (but not in subdirectories)

### Plugins

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
or else use the `@Autowired` annotation, implement this setter,
and call the appropriate `runtime.add*Plugin*` method.

### Logging

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
The default
([log4j.properties](http://gitlab.sas.com/sasdjb/unravl/blob/master/src/main/resources/log4j.properties))
lists each scripts and the results of the assertions

You can configure more fine-grained logging other Log4J configuration files:

  `java -Dlog4j.configuration=your-log4j.properties ...`

## Logistics

UnRAVL is built with either [[Gradle]] or [[Maven]]. The Gradle wrapper is included in the project,
so no extra setup is required (unlike Maven)
to create a jar file `sas.unravl-<em>version</em>.jar`

* Gradle build puts the jar in ./build/libs
* Windows:
  * `gradlew.bat build`
* Linux
  * `./gradlew build`
* Maven build puts the jar in ./target
  * `mvn clean compile package`

## Running UnRAVL from the command line

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


