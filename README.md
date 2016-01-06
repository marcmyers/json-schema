## Sample code for json-schema ##

This project contains a set of TestNG test cases that illustrate the use of `json`, and `json-schema` using jackson and `json-schema-validator` from [Francis Galiegue's GitHub site](https://github.com/fge/json-schema-validator).

The validations are performed against the Cleo REST-API schemas.  These schemas are not (yet) publically hosted, so the test cases assume that you have checked out the `rest-api` project from [Cleo's GitHub site](https://github.com/CleoDev/rest-api) as a sibling to this project.  In particular, the schema files are expected to be found in `../rest-api/schema/` from the root of this project.

Although not explicitly included as a dependency in the POM, the code also makes use of convenience utilities from [Google Guava 16.0.1](http://docs.guava-libraries.googlecode.com/git-history/v16.0.1/javadoc/index.html), which is pulled in by `json-schema-validator` and `jackson`.

The code illustrates:

* loading and processing JSON schemas from schema files, including handling references between files using `$ref` by defining and using a `JsonSchemaFactory`.
* loading a JSON file into a [Jackson Databind](https://github.com/FasterXML/jackson-databind) `JSONNode` (from JAR resources, but similar code would be used to load from external files).
* validating a `JSONNode` against a `JsonSchema`.

### Loading files from JAR resources using Guava ###

This code uses Guava `Resources` to conveniently load a string from a resource:

```
return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);
```

Guava has similarly simple methods for loading files using its `Files` package:

```
return Files.toString(new File(filename), Charsets.UTF_8);
```

### Reading JSON into a `JSONNode` ###

JSON parsing is handled by Jackson's `ObjectMapper`, which directly provides the [`readTree(String)`](http://fasterxml.github.io/jackson-databind/javadoc/2.5/com/fasterxml/jackson/databind/ObjectMapper.html#readTree(java.lang.String)) method for parsing.

```
JsonNode node = new ObjectMapper().readTree(content);
```

### Loading and validating JSON Schemas ###

To run the tests you'll need to have access to Cleo's nexus repository since the tests pull the schemas down based on the version of SecureShare_WebServices that's referred to in the pom.xml

If you need to test against a different version of the schemas, you'll need to go into the pom.xml file of the project and change the cleo version value here:

```

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <jackson.version>2.6.0</jackson.version>
        <cleo.version>5.3-SNAPSHOT</cleo.version>
    </properties>

```

Which in turn sets the value here:

```

        <dependency>
            <groupId>com.cleo</groupId>
            <artifactId>SecureShare_WebServices</artifactId>
            <version>${cleo.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>*</groupId>
                    <artifactId>*</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

```

* default the uri namespace to `http://cleo.com/schemas/` (it doesn't really matter where, but it gives us a hook to...)

```
    private static final URI SCHEMA_URI = uriOrElse("http://cleo.com/schemas/");
    private static final URI SCHEMA_DIR = uriOrElse("resource:/com/cleo/versalex/json/schemas/");

    static {
        UrlSchemeRegistry.register("resource", ResourceHandler.class);

    }

    private static JsonSchemaFactory instance;

    private JsonSchemaFactorySingleton() {}

    public static JsonSchemaFactory getInstance() {
        if(instance == null) {
            instance = JsonSchemaFactory.byDefault().thaw()
                    .setLoadingConfiguration(
                            LoadingConfiguration.byDefault().thaw()
                                    .setURITranslatorConfiguration(
                                            URITranslatorConfiguration.newBuilder()
                                                    .setNamespace(SCHEMA_URI)
                                                    .addPathRedirect(SCHEMA_URI, SCHEMA_DIR)
                                                    .freeze())
                                    .freeze())
                    .freeze();

        }

        return instance;
    }
```

This `schema_factory` can now be used to load schemas from the same `schema_dir` directory with proper resolution of `$ref`.  A JSON schema is of course itself a JSON document, so the technique involves loading the file into a `JSONNode` which can then be passed through `schema_factory.getJsonSchema(JsonNode)` to return a `JsonSchema`, which finally exposes a `validate(JsonNode)` method.

```
JsonSchema schema = schema_factory.getJsonSchema(JsonLoader.fromFile(new File(schema_dir, fn)));
assertTrue(schema.validate(node));
```

This example shows the use of the `JsonLoader` convenience wrapper, included with the `json-schema-validator`, for loading a `File` into a `JSONNode` instead of `ObjectMapper.readTree`.  Note that the source code for [`JsonLoader`](https://github.com/fge/jackson-coreutils/blob/master/src/main/java/com/github/fge/jackson/JsonLoader.java) is a good example of several Guava idoms.

## Test Creation/Usage ##

This section details the day to day usage of the test project.

### @BeforeTest ###

This is a JUnit/TestNG annotation so that certain actions are performed before each test run. 
In our case, it's used to Setup the default URL, API base path, and Preemptive Credentials to use throughout the test run.

```

    @BeforeTest
    public static void testSetup() {
        util.testSetup("http://localhost:5082", "/api/", "administrator", "Admin");

    }

```

Here's the testSetup method that we're calling in @BeforeTest

```

    public static void testSetup(String baseURI, String basePath, String userName, String userPass) {
        // Setup the default URL, API base path, and Preemptive Credentials to use throughout the test
        RestAssured.baseURI = baseURI;
        RestAssured.basePath = basePath;
        RestAssured.authentication = preemptive().basic(userName, userPass);

    }

```

This way those values will only have to be set in one place before the test run. Eventually these values can/should come from a csv, data provider, maven test profile, and/or test.xml
From this point on, to make a request you'll only need to provide the resource of the particular endpoint your trying to reach such as /connections or /certs. Further examples in the Http Requests section below.

### Http Requests ###

To make Http Requests such as Put, Post, etc there is a utility class called HttpRequest.

```

static HttpRequest httpRequest = new HttpRequest();

```

Here's the overloaded Post method for example:

```

    public static String Post(String requestJson, int expStatus, String url) {
        Response resp = given().contentType("application/json").and().body(requestJson).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String Post(Object reqObj, int expStatus, String url) {
        Response resp = given().contentType("application/json").and().body(reqObj.toString()).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

```

So, if you need to attempt a Post, you're going to need to pass either the Json your requesting as string or an Object with your Request (a JSONNode or JSONObject), the expected response as an integer, and the resource endpoint such as "/connections".

The methods were overloaded to make it flexible based on what state your Json Request was in and so that we could send raw Json from a JSONNode or Object based on what the test required.
The method will return the jsonResponse as a String after attempting to assert the status code that was returned against what expStatus argument was passed in.

Keep in mind Posts are sort of generic, you determine what you're posting when you pass in the resource endpoint as String url ("/connections", "/certs", etc.).
RestAssured will resolve the full url and credentials automatically based on what you set in the @BeforeTest method so it's not necessary to refer to them here, though you can override it in your own method if need be.

Here's an example of an attempted Post:

```

        JSONObject newConnection = new JSONObject();
        newConnection.put("type", "as2");

        // Attempt a POST to generate a new connection
        String postResp = httpRequest.Post(newConnection, 201, "/connections");

```

In this example we're simply asking for a new as2 connection, we expect a 201 to be returned, and we're making this request to the /connections endpoint.
The method should return the response as a string and populate our postResp String variable.

### Schema Validation ###

Handled by the SchemaValidation utility class which relies on the JsonSchemaFactorySingelton.

```

static SchemaValidation schemaValid = new SchemaValidation();

```

When you want to validate a JSON response, use SchemaValidation.validate(String reqResp, String schemaType);
String reqResp is the JSON response as String and the String schemaType is the schema you want to validate your JSON against.
This method also returns the JsonNode node it used for validation because most often you'll want to validate specific values in the response as part of further testing.

```

    public JsonNode validate(String reqResp, String schemaType) throws IOException, ProcessingException {
        JsonNode myNode = new ObjectMapper().readTree(reqResp);

        // Verify that the response is not null
        Assert.assertNotNull(myNode);

        assertSuccess(schemas.get(schemaType).validate(myNode));

        return myNode;

    }

```

The correct schema is retrieved based on a Hash map of the schemaType requested and the .schema files from Nexus.

```

    private static HashMap<String, JsonSchema> schemas = Maps.newHashMap(new ImmutableMap.Builder<String, JsonSchema>()
                    .put("connection", schema("connection.schema") )
                    .put("certificate", schema("cert.schema"))
                    .put("action", schema("action.schema"))
                    .put("collection", schema("collection.schema"))
                    .put("common",schema("common.schema"))
                    .put("event", schema("event.schema"))
                    .put("job", schema("job.schema"))
                    .put("transfer", schema("transfer.schema"))
                    .build()

    );

```

So, if you needed to validate a connection response, your code would look similar to this:

```

JsonNode postNode = schemaValid.validate(postResp, "connection");

```

## Running the Tests ##

This project is fully mavenized. By default if you run the following command at the root of the project:

```
mvn test
```

It will run the SmokeTests.xml against http://localhost:5082 and expects certain users to have access to rest for the tests to succeed. (at this time, the default administrator account)
To run the test against a different url and/or port, use the following command specifying the url you want to test against:

```
mvn test -DserverURL=http://192.168.0.1:5080
```

If you want to run a different suite file, use the following command specifying the xml you want to test against:

```
mvn test -DsuiteXML=RegressionTest.xml
```