Postman and similar browser tools are nice for basic interactive
interaction because it makes the API calls so evident: here's the URL,
here's the body, here's the headers, here's the methdod - run it!
Contrasted with calling REST APIs in Java or even Groovy, it's simpler
and cleaner. However, Postman it is not directly
automatable/scriptable (although some tools support this), nor does it
do much validation.

David Biesack sought a simpler REST testing framework that would
remove all the Java boilerplate and make the REST API call, headers,
request bodies and response bodies easier to code, inspect and verify.

His research also uncovered some interesting options when he started searching for
REST testing ''domain-specific languages'' (DSLs):
* [https://github.com/IainHull/resttest IainHull/resttest] may the most interesting, but Scala may be a barrier. See [http://www.slideshare.net/IainHull/rest-test REST Test – Exploring DSL design in Scala]
* Pact provides an RSpec DSL for service consumers to define the HTTP requests they will make to a service provider and the HTTP responses they expect back
** [https://github.com/realestate-com-au/pact original Ruby version]
** [https://github.com/DiUS/pact-jvm - JVM version] (in Scala!)
* [https://www.runscope.com/ RunScope] - use JavaScript to drive/test REST APis
* [http://devblog.songkick.com/2012/12/06/introducing-aspec-a-black-box-api-testing-dsl/ Introducing Aspec: A black box API testing DSL] - pretty primitive
* [https://github.com/balanced/balanced-api/tree/54ec6f0d0cb5bcdfd1e36c1493c7ee247438db27 Balanced API scenarios] uses YAML to define test cases (see the <code>scenarios</code> folder), in a manner very similar to UnRAVL (but not as comprehensive). See also [http://blog.balancedpayments.com/tdd-your-api/ TDD your API]. (First seen April 6, 2015)

David sought a REST testing DSL that is really designed around REST API validation,
something that can be generated from a REST API specification (such as RAML or Swagger).

Thus, he implemented a Uniform REST API Validation Language, or UnRAVL.
