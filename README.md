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

The only real complexity in dealing with the schemas is setting up the infrastructure for resolving `$ref` references between the schema files.  The reference resolution depends on mapping the schema `id` to a URI for accessing the schema.  In this code we modify the configuration of the `JsonSchemaFactory` to:

* default the uri namespace to `http://cleo.com/schemas/` (it doesn't really matter where, but it gives us a hook to...)
* map references within the default namespace to the `file:` path where we expect to find the files.

```
schema_uri = "http://cleo.com/schemas/";
schema_dir = new File("../rest-api/schema/").getCanonicalPath();
        
schema_factory = JsonSchemaFactory.byDefault().thaw()
                 .setLoadingConfiguration(
                     LoadingConfiguration.byDefault().thaw()
                     .setURITranslatorConfiguration(
                         URITranslatorConfiguration.newBuilder()
                         .setNamespace(schema_uri)
                         .addPathRedirect(schema_uri, "file:"+schema_dir+"/")
                         .freeze())
                     .freeze())
                 .freeze();
```

This `schema_factory` can now be used to load schemas from the same `schema_dir` directory with proper resolution of `$ref`.  A JSON schema is of course itself a JSON document, so the technique involves loading the file into a `JSONNode` which can then be passed through `schema_factory.getJsonSchema(JsonNode)` to return a `JsonSchema`, which finally exposes a `validate(JsonNode)` method.

```
JsonSchema schema = schema_factory.getJsonSchema(JsonLoader.fromFile(new File(schema_dir, fn)));
assertTrue(schema.validate(node));
```

This example shows the use of the `JsonLoader` convenience wrapper, included with the `json-schema-validator`, for loading a `File` into a `JSONNode` instead of `ObjectMapper.readTree`.  Note that the source code for [`JsonLoader`](https://github.com/fge/jackson-coreutils/blob/master/src/main/java/com/github/fge/jackson/JsonLoader.java) is a good example of several Guava idoms.