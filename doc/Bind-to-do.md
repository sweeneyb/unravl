Below are some (possible) future enhancements to `"bind"`:

### jsonPath

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

## xml

TODO

```
 { "xml" : XPath, "class" : className }
 { "xml" : XPath, "class" : array-of-classNames }
```

The `"xml"` binder will bind (a fragment of) the XML response body to a Java object,
identified via an XPath expression, using JAXB and place it in the environment.
Use the JSONPath "/" for the entire body.

If the target class or class array is omitted, the XML org.w3c.dom.Node will be stored.

## xPath

TODO

Binds values from the XML response by extracting data via their XPath.

