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
