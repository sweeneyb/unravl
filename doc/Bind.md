This page describes the *assertions* supported in [UnRAVL](Reference.md) scripts.

The `"bind"` elements extract data from an API response body and headers
and store the values in variables that may be used to test the response.
Some bind elements also validate data, acting as implicit assertions.

```
"bind" : [
           extractor_0,
           extractor_1,
           ...
           extractor_n
         ]
```
If you only have one extractor, you do not need to embed it in an array:

```
"bind" : extractor
```
Each extractor has a name and a value. For example,
the following bind element invokes three extractors.
The first is a headers extractor which binds variables
to the values of the API's response headers.
The second writes the response body as text to the
file `response.txt`. The third extractor will
parse the response as JSON and store it in
the environment in the variable named `jsonResponse`.

```JSON
"bind" : [
           { "headers" : { "ct" : "Content-Type", "cl" : "Content-Length" } }
           { "text" : "@response.txt" }
           { "json" : "jsonResponse" },
         ]
```

Below are the UnRAVL extractors listed by name.

## headers

The `headers` element is used to extract text from response headers
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

will save the `Location` header in the variable `loc`, then
matches a regular expression (which is first expanded with environment substitution for {API_ROOT})
and stores the first matching group to `folderId` and the second matching group to `resourceId`.

Note: The backslash character \ must be escaped in JSON: use `\\w+` if you want the regex pattern `\w+`, etc.

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
regular expression patterns, such as `{iso8601}` which is the pattern for an
ISO 8601 date/time value such as `2014-07-16T19:20:30.45+01:00`
and `{httpDate}` which is the pattern
for HTTP header date/time values such as `Fri, 01 Aug 2014 15:16:47 GMT`.
Each of these have one group for each element of the timestamp.

```JSON
{ "headers" : { "lastMod" : [ "Last-Modified", "{httpDate}", "dow", "dom", "mon", "year", "hh", "mm", "ss", "tz" ] } }
```

Tip: Do not use other matcher groups in the regular expression. Where necessary escape special regular expression characters like *, ?, and .

### jsonPath

**TODO**

Binds values from the JSON response by extracting data via their
[JsonPath](https://github.com/jayway/JsonPath).

```
 { "jsonPath" : { map-of-var-path-pairs } }
 { "jsonPath" : { map-of-var-path-pairs }, "from" : "varname" }
```

The first form binds from the JSON response.
The second form may be used to name a variable in the environment;
th value of that variable should be a JSON object
(such as from an `"env"` element or a previous
`"json"` extractor.)

```JSON
{ "jsonPath" : {
     "actualLat" : "$.results[0].location.lat",
     "actualLng" : "$.results[0].location.lng",
     "actualElevation" : "results[0].elevation"
     }
}
```

```JSON
{ "jsonPath" : {
     "actualLat" : "{location}.lat",
     "actualLng" : "{location}.lng",
     "actualElevation" : "results.elevation"
     },
   "from" : "jsonResponse"
}
```

The JsonPath strings are subject to environment substitution.
For example, if the variable `"location"` is bound
to `$.results[0].location`, then the second example
above will extract `actualLat` and `actualLng`
from `$.results[0].location.lat` and `$.results[0].location.lng`
resoectively.

Note that many Jsonath expressions result in arrays of values
that match the path.

## pattern

Matches text against grouping regular expressions and binds the substrings
into constituent variable bindings in the current UnRAVL script environment. The extractor form is

```
 { "pattern" : [ string, pattern, var0, ... varn ] }
```
such as
```
 { "pattern" : [ "{responseType}", "^(.*)\\s*;\\s*charset=(.*)$", "mediaType", "charset" ] }
```
This will match the value of the environment expansion of `"{responseType}"` to the given regular expression pattern `^(.*)\s*;\s*charset=(.*)$`, and bind the media type and the encoding character set substrings to the variables `mediaType` and `charset`. (Note that a per the JSON grammar,
backslash (`\\`) characters in a JSON string must be escaped, so the regular expression notation `\s` is coded in the JSON string as `\\\\s`.)
For example, if the `responseType` binding in the environment was `application/json; charset=UTF-8`,
this pattern specification will bind the variables:
`mediaType` to `application/json`, and
charset to `UTF-8`.
If the regular expression does not match, this extractor will throw an `UnRAVLAssertionException`

This extractor will unbind all the variables before testing the regular expression, so that bindings left from other tests won't persist and leave a false positive. See also the bound assertion to test if values are bound.

## groovy

Run Groovy scripts and bind the results to variables in the environment.
This is like `"env"` extractor, but the values are not just JSON elements,
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

## javascript

Run javascript scripts and bind the results to variables in the environment.
This works like the "groovy" bind element above but uses JavaScript expressions
```
 { "javascript" : { map-of-name-script-pairs } }
```

See the note under the "javascript" assertion about difference
between Groovy and JavaScript.

## text

This binds the response body to a variable or writes it to a file.

```
 { "text" : "varName" }
 { "text" : "@file", "pretty" : boolean }
```

The file name "-", as in
`{ "body" : "@-" }`,
denotes standard output.

##### To do

If pretty is true, then the output will be pretty printed.
The value of the Content-Type header will be used to determine how to pretty print.
If the content type matches ".*[/+]json", it is pretty printed as JSON.
If the content type matches ".*[/+]xml", it is pretty printed as XML.

#### json

```
 { "json" : "@file-name" }
 { "json" : "var" }
 { "json" : "var", "class" : class-name }
```

Parses the response body as a JSON object or JSON array.

It is an error if the response body cannot be parsed as JSON.

Using the Jackson 2.x JSON parser and the result is bound to a
[`org.fasterxml.jackson.databins.JsonNode`](http://fasterxml.github.io/jackson-databind/javadoc/2.2.0/com/fasterxml/jackson/databind/JsonNode.html) as a variable in the current
Environment.

TODO: If the target `class` or class array is present,
Jackson will be used to bind the result to an instance of that class,
and the resulting Java object will be stored in the variable.
The class must be accessible in the current classpath.
This may not be used with the "@file-name" target.

## binary

This binds the response body to a variable as a byte[] array,
or writes it to a file.

```JSON
 { "binary" : "varName" }
 { "binary" : "@file" }
```

The content is copied exactly as 8-bit binary bytes, with no default encoding.
As binary content, the output cannot be streamed to stdout with "@-"
as with the "text" extractor.

## links and hrefs

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

Here's an example. GET a resource at URL stored in the var `{location}`, extract the hrefs for the links with the link relations `"rel' : "self"`, `"rel":"update"` and `"rel": "delete"` from that JSON response body's `"links"` array, and assert the location matches the `"href"` values of those three links:

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

The following will extract the `"self"`, `"update"`, and `"delete"`
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
Groovy expressions such as `self.href.textValue()`
and `self.method.textValue()`.

By default, <br />
"links" extracts links from the current JSON object stored in `responseBody`,
which is normally defined when using the `"json"` extractor.
You may use `"from"` to specify an alternate object that contains links,
such as `"from" : "responseBody.items[0]"`

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
* a string
  * the string is the link relation
* a JSON object
  * the link which matches all the members of this object (as regular expressions) will be selected. The values in the selector must be strings. This allows you to match on the link relation, method, type, and/or uri instead of just the link relation.

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

### Example: Extracting multiple links

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

### Example: Extracting from other sources

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

### Example: Complex matching

By default, if the selector is a string, this extractor only matches the link relation ("rel" value for atom:link content or the key for HAL content). This is also the only option for HAL. For atom:link, you may specify multiple matching criteria, using regular expression matches for one or more members of the link. For example, to match a link that has a "rel" value of "update" and a "method" value of "PUT" and a "href" label that contains "models", use

```JSON
  "bind" : { "link" : { "updateLink" : { "rel" : "update",
                                         "method" : "PUT",
                                         "href" : ".*models.*"
                                        }
                       }
            }
```

## ignore and doc

Use this to comment out an extractor in a "bind" element, or to add documentation to the "bind" element. For example, to cause the `"json"` extractor to be ignore (not create out.json), change

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

## To do

See [Bind to do](Bind-to-do.md) for some possible new `"bind"` elements.
