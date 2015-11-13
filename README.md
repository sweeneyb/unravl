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

Although initially conceived as as a REST validation too, UnRAVL
is also well-suited for use as a REST scripting language, for
stringing together a set of interrelated REST calls, extracting
results and headers from early calls to be used when making
subsequent calls. Users may find it useful to associate `.unravl`
files with the UnRAVL jar file via the batch scripts in the
[`bin`](https://github.com/sassoftware/unravl/tree/master/bin) directory
so you can run scripts from your file explorer.

UnRAVL was designed and initially implemented and is patent pending by David Biesack [@DavidBiesack](https://github.com/DavidBiesack) (GitHub)
[@DavidBiesack](https://twitter.com/DavidBiesack) (Twitter).

## A basic REST validation

The most fundamental form of validation is to invoke an API and
validate the response code and response body.

The REST call
```
 GET  http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false
``` 
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

We also want to assert that we received a `200 OK` HTTP response.

UnRAVL scripts use JSON documents
that encode the call, response, and assertions.

This is useful since much of the test data (request bodies, responses)
will be JSON.

Below is an UnRAVL script that performs the above REST call, asserts
the result matches the expected JSON and that the HTTP response is 200:

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

This shows invoking an API using the GET method. "GET", "HEAD", "POST", "PUT", "DELETE", "PATCH" are the allowed HTTP verbs. For POST, PUT and PATCH you can also pass a request body.

Next, we verify the result with a set of assertions:

1. the expected HTTP status code, 200 OK ; this could also be an array of allowed response types, such as [200, 204]. You can also supply a string value which is a regular expression, such as "2.." If omitted, the status code must match "2.."; that is, be a 200-level status code.
1. assert the JSON body matches an expected JSON structure. This is based on JSON structural equality, not exact text.

The simplest response body assertion is a literal assertion that the body matches the expected JSON,
although admittedly this is somewhat fragile.
The implementation of such benchmark comparison assertions  performs a structural comparison
and allows for different order of items in JSON objects.

Another form of response assertion allows comparing the received body to the contents of a benchmark file
rather than literal JSON in the file.
(In the JSON based DSL, this form is be required for XML benchamrks.)

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
`benchmarks` is the name of a directory containing JSON files.
name is the current test name (set by the `"name"` element of the script),
with the value `"GoogleEverestElevation"` in this case. The
`{varName}` notation is used to evaluate the value of an environment
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
library for JSON) named `response`. This variable may be used to compare nested
values, as seen in the `assert` array. Each assertion string is a Groovy
expression that must evaluate to true for the test to pass.
(You can also use JavaScript.)
Many string values in UnRAVL scripts are subject to environment substitution
which replaces substrings of the form `{varName}` with the value
of the named variable from the environment.

## Syntax and reference

To see the full syntax for UnRAVL scripts, refer to [Reference](doc/Reference.md).

## Releases

* [Release v1.0.0](https://github.com/sassoftware/unravl/releases/tag/v1.0.0)
  * Removed deprecated features (equals assertion)
  * Reduced dependency on JUnit. All behaviors, assertions, etc. do not depend on JUnit.
* [Release v0.2.1](https://github.com/sassoftware/unravl/releases/tag/v0.2.1) is the current release of UnRAVL.
  * Added UnRVAL.cancel() support for test scripts and runtime
  * Added UnRAVL.execute() method to allow running multiple JsonObject scripts
* [Release v0.2.0](https://github.com/sassoftware/unravl/releases/tag/v0.2.0) is the first public release of UnRAVL.

## Running UnRAVL

You can download the source from this repo and run
```
    git clone git@github.com:sassoftware/unravl.git
    cd unravl
    ./gradlew clean build copyDeps
```
Run UnRAVL as:
```bash
    bin/unravl.sh src/test/scripts/hello.json  # from Linux or Mac OS X
    bin\unravl.bat src/test/scripts/hello.json # from Windows
```

Alternatively, you can download the binary release.
Create a directory `unravl` for your local copy of UnRAVL, `cd unravl` to that directory,
then download [a release](https://github.com/sassoftware/unravl/releases).
Unzip the release file in the `unravl` directory.
Run UnRAVL using the scripts in the `bin` directory, as described above.

## Contributing

Contributors are welcome to join the project.
See [ContributorAgreement.txt](ContributorAgreement.txt).

To contribute, submit issues for bugs or enhancments. 
[Fork this repo](https://help.github.com/articles/fork-a-repo),
then clone your GitHub fork:

```
$ git clone git@github.com:userid/unravl.git
```

(Change *`userid`* to your GutHub user id). Next, set the Git upstream
which will allow you to merge from master:
```
$ git remote add upstream git@github.com:sassoftware/unravl.git
```

Before doing local development, be sure to sync your local code:

To [sync your fork with master](https://help.github.com/articles/syncing-a-fork/)
after changes have been merged at https://github.com/sassoftware/unravl :
```
$ cd unravl # change to your local clone
$ git fetch upstream # get all branches
$ git checkout master 
$ git merge upstream/master
```

Create a local branch for your changes, then push to your personal unravl repo
and [create a GitHub pull request](https://help.github.com/articles/using-pull-requests/) 
for submitting new contributions.

Contributions should use the Eclipse format configuration in `eclipse-java-format.xml`
and organize imports in com, java, javax, org order (alphabetical, with grouping)

Contributors are listed in [CONTRIBUTORS.md](CONTRIBUTORS.md).

```
## License

UnRAVL is released under the [Apache 2.0 License](LICENSE).
