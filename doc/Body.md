This page describes the various forms for supplying a request
body in [UnRAVL](Reference.md) scripts.

There are four different forms for creating the request body within a script:

* [`"body" : { "json" : json-request-body }`](#json)
* [`"body" : { "text" : text-request-body }`](#text)
* [`"body" : { "binary" : binary-request-body }`](#binary)
* [`"body" : { "form" : form-request-body }`](#form)

As a convenience, you can also use the shortcut form
for a JSON request body:

* [`"body" : json-request-body`](#json)

## json

To pass JSON, simply supply the JSON object or array. For example:

```
{
  "POST" : "http://www.example.com/api",
  "body" : { "json" :
             { "command" : "start",
               "port" : 80,
               "args" : [ "--verbose", "--output-file=/tmp/out.dat" ],
               "validate" : true
             }
           }
}
```

The *`json-request-body`* can contain variable references
inside its string values, as per
[Environment](Reference.md#Environment).

You can also name a variable that is bound to a JSON object or array:

```JSON
  "body" : { "json" : "varName" }
```

Finally, you can read the JSON from a file or URL and
pass the resulting JSON as the request body. (The test
will fail if the referenced resource does not exist
or cannot be parsed as JSON.)

You can also name a variable that is bound to a JSON object or array:

```JSON
  "body" : { "json" : "@file-or-url" }
```
The referenced JSON resource can contain variable references
which will be expanded as per
[Environment](Reference.md#Environment).

In addition, if the value of `"body"` does not match any other body generator, such as
* `{ "json" : "varName" }`
* `{ "json" : "@file-or-URL" }`
* `{ "json" : *json-body-specification* }`
* `{ "text" : *text-body-specification* }`
* `{ "binary" : *binary-body-specification* }`
* `{ "form" : *form-body-specification* }`
then the entire value of the `"body"` item is used as a JSON body.

### Examples
```JSON
  "body" : { "x" : 0, "y" : 1, "z" : -1 }
```
is processed as if it were
```JSON
  "body" : { "json": { "x" : 0, "y" : 1, "z" : -1 } }
```
Using `{ "json" : *json-body-specification* }` is safer since it avoid conflict with body generator
names that may be added in the future, but the direct JSON body
is more concise and easier to use.

## text

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
               "@{templates}/epilog.txt" ]
   }
```

The text can contain variable references as per
[Environment](Reference.md#Environment), including in @paths.

At present, the text source is the only way to PUT, PATCH or POST XML content;
the JSON notation for UnRAVL scripts does not allow directly embedding raw XML text.
The text value may contain XML, or an array of strings, or @strings that reference
external files or URLs that contain XML content.

## binary

The *`"binary"`* element is for passing binary data as the request body.

```JSON
  { "binary" : array-of-bytes }
```
```JSON
  { "binary" : "@binary-file-or-url" }
```

such as
```JSON
  { "binary" : [85, 110, 82, 65, 86, 76, 82, 111, 99, 107, 115, 33] }
```

## form

The *`"form"`* element is used to POST
`application/x-www-form-urlencoded`
data.

The form body may be passed in a number of ways:
an embedded JSON object consisting of *n* `"name"` : *`value`* pairs;
a reference to a variable in the environment that contains
such data; a reference to a resource or file that contains
such JSON, or a simple string that contains
`application/x-www-form-urlencoded` formatted data.
```JSON
  { "form" : { "name" : "value", "name" : "value" } }
```
```JSON
  { "form" : "varName" }
```
```JSON
  { "form" : "@file-or-url-containing-JSON" }
```
```JSON
  { "form" : "name=url-encoded-value1&b=name2=url-encoded-value2" }
```

The *`"form"`* element will add a
`Content-Type` header with the value
`application/x-www-form-urlencoded`.
