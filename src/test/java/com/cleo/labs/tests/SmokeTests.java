package com.cleo.labs.tests;

import com.cleo.labs.json_schema.HttpRequest;
import com.cleo.labs.json_schema.SchemaValidation;
import com.cleo.labs.json_schema.Utils;
import org.testng.annotations.BeforeTest;

/**
 * Created by mmyers on 11/2/2015.
 */
public class SmokeTests {
    static HttpRequest httpRequest = new HttpRequest();
    static SchemaValidation schemaValid = new SchemaValidation();
    static Utils util = new Utils();

    @BeforeTest
    public static void beforeTest() {
        util.testSetup("http://162.243.186.156:5080", "/api/", "administrator", "Admin");

    }


}
