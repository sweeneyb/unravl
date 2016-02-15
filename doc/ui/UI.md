# Interactive mode

UnRAVL also has a basic user interface which allows you
to interactively submit UnRAVL scripts and see the results.

To start UnRAVL in interactive mode, run the unravl
start script without any UnRAVL script files on the command line:

Linux and Mac OS X:
```
unravl.sh
```
Windows:
```
unravl.bat
```

There are four tabs in the interface: [Source](#source),
[Output](#output), [Calls](#calls), and [Variables](#variables).

## Source

In this **Source** tab,
you can enter an UnRAVL script using JSON notation.
The interface does basic JSON syntax checking as you type,
highlighting errors. Click the arrow button at the bottom
left to move the cursor to the first syntax error.

![The UnRAVL Source tab](UnRAVL-UI-source.png)

UnRAVL automatically saves the source and restores
it the next time you start the application.
Future enhancements will add the normal **File -> Open**,
**File -> Save** and **File -> Save** as menu operations.

If the JSON is valid, the **Run** button will be enabled.
Click the **Run** button to
submit the UnRAVL script from the source pane.
The interface will  automatically switch to the Output tab where you
can see the results of running the script.

## Output

The **Output** tab shows the standard output and standard error
captured while running the script.

![The UnRAVL Output tab](UnRAVL-UI-output.png)
For example, the output of any [Bind](../Bind.md) elements
will appear hear. For example, if the script contains

```JSON
  "bind" : { "json" : "@-" }
```
the output from the REST API call will be printed to
the Output tab.

## Calls

The **Calls** tab shows the history of API calls.
Use the buttons at the top right to move back and forth
in the history. The calls tab shows each
test name, the HTTP method, the URL,
the request headers, response headers,
HTTP status code, and the response body.

The **Reset** button clears the API call history
as well as removing variables defined by the
tests.

## Variables

You may also select the **Variables** tab to view UnRAVL
variables. Click on a variable name to see its
value in the bottom of the **Variables** tab.

![The UnRAVL Variables tab](UnRAVL-UI-variables.png)

UnRAVL interface will pretty-print JSON objects
and arrays for variables that have JSON values
(but not simple string values, even if they contain
JSON text.) You may only view one variable at a time.

If you uncheck the **Show all** checkbox, the variables
list will show only variables that were bound or
modified when the most recent UnRAVL script executed.

You can also type in a **Search:** string and only variables
whose names contain the search text will be shown.
Clear the search box to remove the filtering.

