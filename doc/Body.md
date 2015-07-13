This page describes the various forms for supplying a request
body in [UnRAVL](Reference.md) scripts.

There are three different forms for creating the request body within a script:

* <code>["body" : { "json" : json-request-body }](#json)</code>
* <code>["body" : { "text" : text-request-body }](#text)</code>
* <code>["body" : { "binary" : binary-request-body }](#binary)</code>

As a convenience, you can also use the shortcut form
for a JSON request body:

* <code>["body" : json-request-body](#json)</code>

## json ## 

To pass JSON, simply supply the JSON object or array:

```
{
  "POST" : "http://www.example.com/api",
  "body" : { "json" : json-request-body }
}
```

The *<code>json-request-body</code>* can contain variable references
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

In addition, if the value of <code>"body"</code> does not match any other body generator, such as
* <code>{ "json" : "varName" }</code>
* <code>{ "json" : "@file-or-URL" }</code>
* <code>{ "json" : *json-body-specification* }</code>
* <code>{ "text" : *text-body-specification* }</code>
* <code>{ "binary" : *binary-body-specification* }</code>
then the entire value of the <code>"body"</code> item is used as a JSON body.

=== Examples ===
```JSON
  "body" : { "x" : 0, "y" : 1, "z" : -1 }
```is processed as if it were
```JSON
  "body" : { "json": { "x" : 0, "y" : 1, "z" : -1 } }
```
Using <code>{ "json" : *json-body-specification* }</code> is safer since it avoid conflict with body generator
names that may be added in the future, but the direct JSON body
is more concise and easier to use.

## text ##

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

## binary ##

This form is for passing binary data as the request body.

```JSON
  "binary" : array-of-bytes
```

```JSON
  "binary" : "@binary-file-or-url"
```
