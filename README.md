## Uniform REST API Validation Language (UnRAVL) - a JSON domain-specific language for validating REST APIs

UnRAVL is a domain-specific language, coded in JSON, for validating REST APIs.
UnRAVL scripts consist of a JSON description of a REST API call:

1. HTTP method
1. URI
1. HTTP headers (optional)
1. Request body (optional)
1. Authentication (optional)

For each API call, an UnRAVL script also contains one or more
assertions which validate the results. You can assert:

1. The result body matches expected JSON, text or other data
1. Specific headers exist with specific values
1. HTTP status code is a specific value or is in a specific set

UnRAVL also supports extracting data from a REST API call's results
and using those values for future API calls and validations.

A template facility provides reusable API call and validation constructs.

UnRAVL was designed and initially implemented and is patent pending by David Biesack @sasdjb

## A basic REST validation

The most fundamental form of validation is to invoke an API and
validate the response code and response body.

The REST call

 GET  http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false
should return the JSON body:
```JSON
{
   "results" : [
      {
         "elevation" : 8815.7158203125,
         "location" : {
            "lat" : 27.988056,
            "lng" : 86.92527800000001
         },
         "resolution" : 152.7032318115234
      }
   ],
   "status" : "OK"
}
```

It would be nice to code this interaction as a script:

```
name: GoogleEverestElevation
GET: http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false
assert:
  response: 200
  json:
{ "results" : [ { "elevation" : 8815.7158203125,
        "location" : { "lat" : 27.988056,
            "lng" : 86.92527800000001
          },
        "resolution" : 152.7032318115234
      } ],
  "status" : "OK"
}
```

(This is not a formal syntax, just an outline of what might be possible.
It may look like YAML but it is not. YAML ain't markup language, and this ain't YAML)

Because of its indentation rules, when you want
to nest other content such as JSON structure, XML, or even markdown text
(that is, anything that is ''not'' YAML),
YAML is really unwieldy and cumbersome.

Thus, UnRAVL scripts use simple JSON documents
that encodes the call and response and assertions.

This is useful since much of the test data (request bodies, responses)
will be JSON.

Here goes:

```JSON
{
  "name" : "GoogleEverestElevation",
  "GET" : "http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false",
  "assert": [
    { "status" : 200 },
    { "json" :

{ "results" : [ { "elevation" : 8815.7158203125,
        "location" : { "lat" : 27.988056,
            "lng" : 86.92527800000001
          },
        "resolution" : 152.7032318115234
      } ],
  "status" : "OK"
}
           }
           ]
}
```

This is not quite as clean as the non-JSON representation, but this is
something that a folding hierarchical JSON sensitive editor can work
with.

This shows invoking an API using the GET method. "GET", "HEAD", "POST", "PUT", "DELETE", "PATCH" are the allowed HTTP verbs. For POST, PUT and PATCH you can also pass a request body.

Next, we handle the result with a set of assertions:

1. the expected HTTP status code, 200 OK ; this could also be an array of allowed response types, such as [200, 204]. You can also supply a string value which is a regular expression, such as "2.." If omitted, the status code must match "2.."; that is, be a 200-level status code.
1. assert the JSON body matches an expected JSON structure. This is based on JSON structural equality, not exact text.

The simplest response body assertion is a literal assertion that the body matches the expected JSON,
although admittedly this is somewhat fragile.
The implementation of such benchmark comparison assertions would perform a structural comparison
(JSONAssert) and allow for additional values or different order, and would
need support for some annotations, such as ignoring the values of certain fields
like date/time values, which may vary across test invocations.
Note that the numeric floating point comparisons should use fuzzy comparisons using an ''epsilon'' value
(for example, note that the <code>lng</code> 86.92527800000001 value is not exactly equal to
86.925278.)

Another form of response assertion would allow comparing the received body to the contents of a benchmark file
rather than literal JSON in the file.
(In the JSON based DSL, this form would be required for XML benchamrks.)

```
{
  "name" : "GoogleEverestElevation",
  "GET" : "http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false",
  "assert": [
    { "status" : 200 },
    { "json" :  "@{benchmarks}/{name}.json" }
    ]
}
```

The @ notation indicates the JSON is located at a file or URL, not in line.
This example expects two variables to be bound in the UnRAVL environment:
<code>benchmarks</code> is the name of a directory containing JSON files.
name is the current test name (set by the <code>"name"</code> element of the script),
with the value <code>"GoogleEverestElevation"</code> in this case. The
<code>{varName}</code> notation is used to evaluate the value of an environment
variable. More on this later.

However, because representation assertions are fragile,
an alternate approach would be to perform more specific assertions for data elements in the body.

```
{
  "name" : "GoogleEverestElevation",
  "GET" : "http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false",
  "bind" : { "json" : "response" },
  "assert": [
     "response.results[0].elevation.doubleValue() == 8815.7158203125",
     "response.results[0].location.lat.doubleValue() == 27.988056",
     "response.results[0].location.lng.doubleValue() == 86.925278",
     "response.status.textValue().equals('OK')"
    ]
}
```

UnRAVL scripts also have an Environment, which is a set of name/value pairs or variables.
You can set variables explicitly, or bind them based on the result of API calls.
For example, the above binds the JSON response body to a JsonNode (using the Jackson
library for JSON) named <code>response</code>. This variable may be used to compare nested
values, as seen in the <code>assert</code> array. Each assertion string is a Groovy
expression that must evaluate to true for the test to pass.
Many string values in UnRAVL scripts are subject to environment substitution
which replaces substrings of the form <code>{varName}</code> with the value
of the named variable from the environment.

## Syntax and reference

To see the full syntax for UnRAVL scripts, refer to [Reference](doc/Reference.md)

## Releases

[Release v0.2.0](https://github.com/sassoftware/unravl/releases/tag/v0.2.0) is the first public release of UnRAVL

## Running UnRAVL

You can download the source from this repo and run
```
    ./gradlew clean build copyDeps
```
Run UnRAVL as:
```bash
    bin/unravl.sh script.json  # from Linux
    bin\unravl.bat script.json # from Windows
```

Alternatively, you can download the binary release.
Create a directory `unravl` for your local copy of UnRAVL, `cd unravl` to that directory,
then download the release [unravl-0.2.0.zip](https://github.com/sassoftware/unravl/releases/download/v0.2.0/unravl-0.2.0.zip). Unzip the release file in the `unravl` directory.
Then run UnRAVL using the scripts in the `bin` directory, as described above

## Contributing

See [ContributorAgreement.txt](ContributorAgreement.txt)

Contributions should use the Eclipse format configuration in `eclipse-java-format.xml`
and organize imports in com, java, javax, org order (alphabetical, with grouping)

Contributors are listed in [CONTRIBUTORS.md](CONTRIBUTORS.md).

## License

UnRAVL is released under the [Apache 2.0 License](LICENSE).

