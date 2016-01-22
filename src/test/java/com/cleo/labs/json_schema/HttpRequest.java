package com.cleo.labs.json_schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    // Does a POST but returns an Object instead of a jsonResponse as string
    // Mostly used when avoiding schema validation
    public static ObjectNode PostReturnObj(Object reqObj, int expStatus, String url) throws Exception {
        Response resp = given().contentType("application/json").and().body(reqObj.toString()).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        // JsonNode myNode = new ObjectMapper().readTree(reqResp);
        JsonNode postNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)postNode;

    }

    public static ObjectNode PostReturnObj(String requestJson, int expStatus, String url) throws Exception{
        Response resp = given().contentType("application/json").and().body(requestJson).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode postNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)postNode;

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

    // Does a PUT but returns an Object instead of a jsonResponse as string
    // Mostly used when avoiding schema validation
    public static ObjectNode PutReturnObj(Object reqObj, int expStatus, String url) throws Exception{
        Response resp = given().contentType("application/json").and().body(reqObj.toString()).put(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode putNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)putNode;

    }

    public static ObjectNode PutReturnObj(String requestJson, int expStatus, String url) throws Exception{
        Response resp = given().contentType("application/json").and().body(requestJson).put(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode putNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)putNode;

    }

    public static String Get(String conId, int expStatus, String url) {
        Response resp = given().when().get(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static ObjectNode GetReturnObj(String conId, int expStatus, String url) throws Exception{
        Response resp = given().when().get(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode getNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)getNode;

    }

    public static void Delete(String conId, int expStatus, String url) {
        Response resp = given().and().delete(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);

    }

}
