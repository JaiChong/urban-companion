/* MyCity.java
 *  Project 2 for CSS 436: Cloud Computing
 * 
 * Input:
 *  $ javac MyCity.java
 *  $ java MyCity location
 *      e.g. args = [ "seattle" ]
 * 
 * Output:
 *  Data fetched from 3 independent RESTful API
 *  
 * Based on:
 *  ../p1/WebCrawl.java by Prof. Dimpsey, Jaimi Chong
 *  10 LOC Boilerplate  by Prof. Dimpsey
 * 
 * Completed by:
 *  Jaimi Chong in Feb. 2024
 */


import java.io.Console;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.net.URLEncoder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class MyCity
{
  public static void main(String[] args)
  {
    // 1. PROGRAM SETUP
    process_args(args);
    print_intro();

    // 2. API HANDLING
    if (http_call(API_1_NAM, API_1_URL)) parse_resp_append_results(1);
    if (http_call(API_2_NAM, API_2_URL)) parse_resp_append_results(2);
    if (http_call(API_3_NAM, API_3_URL)) parse_resp_append_results(3);
    System.out.println(results);
  }


  //===============
  // 0. RESOURCES ||
  //===============

  //===============================
  // 0.1. Class-scope Variables //
  //=============================

  // Program args
  private static String secret;
  public  static String city;

  // APIs
  public static final String API_1_NAM = "OpenWeatherMap";
  public static final String API_1_URL = "http://api.openweather.org/data/2.5/weather?q=&appid=";
  public static final String API_2_NAM = "";
  public static final String API_2_URL = "";
  public static final String API_3_NAM = "";
  public static final String API_3_URL = "";

  // HTTP calls
  public static HttpClient           client;
  public static HttpRequest          req;
  public static HttpResponse<String> resp;
  public static int                  resp_status;
  public static final int RETRY_BASE = 2;
  public static final int RETRY_MAX  = 4;

  // Results
  public static String results = "\n\n//============" +
                                     "// RESULTS //" +
                                     "//==========";

  //=============================
  // 0.2. HTTP Call Functions //
  //===========================
  
  public static boolean http_call(String nam, String url)
  {
    System.out.println("\nCalling " + nam + "...");
    client  = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    req     = HttpRequest.newBuilder().uri(URI.create(url + city + "&appid=" + secret)).build();
    update_resp_exp_backoff();
    return resp_status < 400;
  }

  public static void update_resp_exp_backoff()
  {
    // client.send() -> Exceptions
    try
    {
      // for (4 retries or until success)
      for (int retry = 0; retry <= RETRY_MAX; retry++)
      {
        resp = client.send(req, BodyHandlers.ofString());
        resp_status = resp.statusCode();
        System.out.print(" Status "+resp_status+": ");
        if (resp_status < 400)
        {
          System.out.println("Success!");
          retry = RETRY_MAX;                            // exit loop
        }
        else
        {
          System.out.print("Failure");
          if (retry < RETRY_MAX && (resp_status >= 500 || resp_status == 429))
          {
            System.out.println(", retrying in "+(RETRY_BASE ^ retry)+" seconds ...");
            Thread.sleep(1000 * (RETRY_BASE ^ retry));  // 1000ms*(2^n) -> 1s, 2s, 4s, 8s, give up
          }
          else System.out.println(".");
        }
      }
    }
    catch (IOException | InterruptedException e) { System.out.println("\n" + e.getMessage()); System.exit(1); }
  }


  //===================
  // 1. PROGRAM SETUP ||
  //===================

  public static void process_args(String[] args)
  {
    // Ensure correct num of args
    if (args.length != 2)
    {
      System.out.println("Error: Incorrect number of arguments.");
      System.exit(1);
    }
    
    // Store args
    MyCity.secret = args[0];
    city          = args[1];
  }

  public static void print_intro()
  {
    System.out.println("\n//==========");
    System.out.println(  "// INTRO //");
    System.out.println(  "//========");

    System.out.println("\nMyCity.java");
    System.out.println(  " Input:  City name");
    System.out.println(  " Output: Local info gathered from three independent API");
    System.out.println(  " Algo:   Exponential Backoff (delay between calls increases exponentially)");
    System.out.println(  " Consts: RETRY_BASE="+RETRY_BASE+", RETRY_MAX="+RETRY_MAX+", three URL_API_*'s");

    System.out.println("\nHTTP Response Status Codes Key:");
    System.out.println(  " Accepted: 100s=Informtional,  200s=Success,  300s=Redirects");
    System.out.println(  " Rejected: 400s=ClientErr");
    System.out.println(  " Retried:   429=TooManyReqs,   500s=ServerErr");


    System.out.println("\n\n//==============");
    System.out.println(    "// API CALLS //");
    System.out.println(    "//============");
  }


  //===========================
  // 2. API RESPONSE HANDLING ||
  //===========================

  public static void parse_resp_append_results(int api_num)
  {
    // RootObject weatherDetails = JsonConvert.DeserializeObject<RootObject>(response)!;
    switch (api_num)
    {
      case 1:
        // int todayTemp = (int) weatherDetails.main.temp;
        // if (todayTemp > 55) Console.WriteLine("surprisingly warm today");
        results += "\n"+API_1_NAM+":" +
                       " info";
        break;

      case 2:
        results += "\n"+API_2_NAM+":" +
                       " info";
        break;
      
      case 3:
        results += "\n"+API_2_NAM+":" +
                       " info";
        break;
    }
  }
}