This page describes the "auth" element of [UnRAVL scripts](Reference.md).

The scriptlet form is
```
  "auth" : { name : value, options... }
```
the *`name`* determines what type of authentication,
and the *`value`* and *`options`* are used to configure that authentication.
Credentials can be included in the options, or stored
separately from the script; see [Credentials](#Credentials) below.

UnRAVL supports two authentication models,
* [basic](#basic)
* [cas](#cas)

Tip: You can put the `"auth"` member in a template and all scripts which inherit from that template will use that authentication method.

## basic

Basic Authentication locates credentials for the REST API call host
via the `.netrc` file (see above) and adds an

`Authentication: Basic *encoded-credentials*`

header to the request.

The basic auth syntax is
```JSON
  "auth" : { "basic" : true }
  "auth" : { "basic" : true, "login" : "testuserid" }
  "auth" : { "basic" : true, "login" : "testuserid", "password" : "testSecret" }
```

If the *`"password"`* or *`"login"`* id are omitted, they are obtained
from the credentials file, described below, based on the host
name of the API call.

## cas

Central Authentication Service authentication will
use a login URL to autheticate the user credentials
and acquire a *Ticket Granting Ticket* (TGT). For each API call
in an UnRAVL script, UnRAVL will use the TGT to request a *Service Ticket*
for that API URL and append the Service Ticket to
the request URL.

The form is
```JSON
  "auth" : { "cas" : "login-Url" }
  "auth" : { "cas" : "login-Url", "login" : "testuserid" }
  "auth" : { "cas" : "login-Url", "login" : "testuserid", "password" : "testSecret" }
```

Example:

```JSON
  "auth" : { "cas" : "http://{sas.logon.host}:7980/SASLogon/rest/v1/tickets" },
  "GET" : "http://{my.app.host}/SASMyApi/rest/myEndpoint/myResource"
```

UnRAVL will lookup the
The credentials for the {sas.logon.host} URL
in the file `.netrc` as [described below](#Credentials), then use the full URL
"http://{sas.logon.host}:7980/SASLogon/rest/v1/tickets" to obtain a TGT.
Then using that TGT, UnRAVL will request a service ticket for the URL
"http://{my.app.host}/SASMyApi/rest/myEndpoint/myResource" (after expanding the
environment variables), then append the ticket. The net result will look something
like
 GET http://www.example.com/SASMyApi/rest/myEndpoint/myResource?ticket=ST-188763-kEcYVdVfAVYdmEyyfZWg-cas

The TGT is stored in the environment using `&lt;<em>hostname</em>&gt;.TGT`,
where `&lt;<em>hostname</em>&gt;` is taken from the `login-Url`. The TGT
will be resused in other scripts that call the same host.

## Credentials

For best security, the credentials for authentication are stored in
the file `.netrc` in the current directory (if it exists),
or in the user's home directory (`~/.netrc` on Linux,
or `%USERPROFILE%\\_netrc` on Windows, for example).
Users can protect these files using file system security.
For example, on Linux:
```
 chmod go-rwx .netrc
```
will not allow others users to read or write the `.netrc` file.

The format of the file is a simplified version of the standard
[Unix netrc file format](http://www.lehman.cuny.edu/cgi-bin/man-cgi?netrc+4).

Warning: The *`default`* entry and
*`macdef`* in the `[.netrc](http://www.lehman.cuny.edu/cgi-bin/man-cgi?netrc+4)` spec are not supported.

Credentials must be specified entirely on one line:

`machine` *`hostname`* `login` *`userid`* `password` *`password`*

such as

```
 machine rdcesx51019.race.sas.com login sasdemo password sasDemoSecret123
```
The *hostname* field must exactly match the hostname in UnRAVL API calls.

You may also embed the credentials directly inside the authentication element in the script.
These may be the login id and password (if there are no security issues with directly embedding
the credentials in the script).

```JSON
  "auth" : { "basic" : true,
             "login" : "sasdemo",
             "password" : "sasDemoSecret123" }
```
Your script `"auth"` elements may also use UnRAVL environment variable
substitution. For example, you can pass the credentials at startup:

```
$ export UNRAVL_OPT='-Dhostname.login=sasdemo -Dhostnme.password=sasDemoSecret123
$ unravl script1.json scipt2.json
```

Warning: Doing this may leave the credentials readable in the process tables or to other tools that can read the currently running commands and all their parameters.

You may set credentials in Java system properties before creating the UnRAVL runtime
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

If the `"login"` is embedded but no `"password"`,
UnRAVL will look up the password for that host/login pair in the `.netrc` file.

