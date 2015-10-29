package com.cleo.labs.json_schema;

import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.skife.url.UrlSchemeRegistry;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by mmyers on 10/29/2015.
 */
public class JsonSchemaFactorySingleton
{
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

    private static URI uriOrElse(String u) {
        try {
            return new URI(u);

        } catch (URISyntaxException ignore) {}
        return null;

    }

}
