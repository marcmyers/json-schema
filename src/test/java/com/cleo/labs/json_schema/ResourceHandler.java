package com.cleo.labs.json_schema;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.google.common.io.Resources;

public class ResourceHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String path = u.getPath();

        if (path.startsWith("/")) {
            path = path.substring(1);

        }
        System.out.println("resolving "+u.toString()+" at "+path);

        return Resources.getResource(path).openConnection();

    }

}
