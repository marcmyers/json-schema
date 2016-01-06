package com.cleo.labs.json_schema;

import com.jayway.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;

import static com.jayway.restassured.RestAssured.given;

/**
 * Created by mmyers on 10/29/2015.
 */
public class HttpRequest {
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

    public static String Put(String requestJson, int expStatus, String url) {
        Response resp = given().contentType("application/json").and().body(requestJson).put(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String Put(Object reqObj, int expStatus, String url) {
        Response resp = given().contentType("application/json").and().body(reqObj.toString()).put(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String Get(String conId, int expStatus, String url) {
        Response resp = given().when().get(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static void Delete(String conId, int expStatus, String url) {
        Response resp = given().and().delete(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);

    }

}
