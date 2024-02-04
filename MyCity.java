/* MyCity.java
 *  CSS 436: Cloud Computing, Project 2
 * 
 * Input:
 *  $ javac MyCity.java
 *  $ java MyCity [String:api_key_0]    [String:api_key_1]  [String:api_key_2]    [String:city]
 *         e.g.   (OpenWeatherMap key)  (Yelp Fusion key)   (TomTom Traffic key)  San_Francisco
 * 
 * Output:
 *  Data gathered from 3 independent RESTful API
 *  
 * API Documentation:
 *  https://openweathermap.org/api/geocoding-api#direct
 *  https://openweathermap.org/current
 *  https://docs.developer.yelp.com/docs/fusion-intro
 *  https://docs.developer.yelp.com/reference/v3_events_search
 *  https://developer.tomtom.com/traffic-api/documentation/traffic-incidents/incident-details
 * 
 * URL Documentation:
 *  https://docs.oracle.com/javase/8/docs/api/java/net/URLEncoder.html
 * 
 * GSON Documentation:
 *  https://www.geeksforgeeks.org/how-to-install-gson-module-in-java/
 *  https://javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/package-summary.html
 *  https://javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/JsonParser.html
 *  https://javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/JsonObject.html
 *  https://javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/JsonElement.html
 *  https://javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/JsonArray.html
 *  https://javadoc.io/static/com.google.code.gson/gson/2.10.1/com.google.gson/com/google/gson/stream/JsonReader.html
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
import java.lang.Double;
import java.lang.Thread;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
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
    call_parse_apis();
    System.out.println(results);
  }


  //===============
  // 0. RESOURCES ||
  //===============

  //===============================
  // 0.1. Class-scope Variables //
  //=============================

  // APIs
  public  static String[] api_nams = 
  {
    "OpenWeatherMap Geocoding",
    "OpenWeatherMap Current Weather Data",
    "Yelp Fusion",
    "TomTom Traffic Incident Details"
  };
  public  static String[] api_urls =
  {
    "http://api.openweather.org/geo/1.0/direct?q=%s&limit=1&appid=%s%s%s%s",
    "http://api.openweather.org/data/2.5/weather?lat=%s&lon=%s&appid=%s%s%s",
    "https://api.yelp.com/v3/categories/events?sort_on=time_start&latitude=%s&longitude=%s",
    // "https://api.yelp.com/v3/categories/restaurants?latitude=%s&longitude=%s",
    "https://api.tomtom.com/traffic/services/5/incidentDetails?key=%s&bbox=%s,%s,%s,%s&fields={incidents{properties{iconCategory,magnitudeOfDelay,events{code,description,iconCategory},startTime,endTime,from,to,length,delay,roadNumbers,timeValidity,probabilityOfOccurrence,numberOfReports,lastReportTime}}}&language=en-US&timeValidityFilter=present,future"
  };
  private static String[] api_keys = new String[4]; // { null, args[0], args[1], args[2] }
  public  static String   city;                     // args[3]
  public  static String   lat = "0";                // City Latitude
  public  static String   lon = "0";                // City Longitude

  // HTTP calls
  public static HttpClient           client;
  public static HttpRequest          req;
  public static HttpResponse<String> resp;
  public static int                  resp_status;
  public static final int            RETRY_BASE = 2;
  public static final int            RETRY_MAX  = 4;

  // Results
  public static String results = "\n\n//============" +
                                     "// RESULTS //" +
                                     "//==========";

  //=============================
  // 0.2. HTTP Call Functions //
  //===========================
  
  public static void http_build_req_v1 (int num, String arg1, String arg2, String arg3, String arg4, String arg5)
  {
    req = HttpRequest.newBuilder()
            .uri(URI.create(String.format(api_urls[num], arg1, arg2, arg3, arg4, arg5)))
            .header("accept", "application/json")
            .build();
  }

  public static void http_build_req_v2 (int num, String arg1, String arg2, String hdr_key)
  {
    req = HttpRequest.newBuilder()
            .uri(URI.create(String.format(api_urls[num], arg1, arg2)))
            .header("accept", "application/json")
            .header("Authorization", hdr_key)
            .build();
  }

  public static boolean http_call(int num)
  {
    System.out.println("\nCalling " + api_nams[num] + "...");
    
    // try client.send()
    try
    {
      // for (4 retries or until success)
      for (int retry = 0; retry <= RETRY_MAX; retry++)
      {
        resp = client.send(req, BodyHandlers.ofString());
        resp_status = resp.statusCode();
        System.out.printf(" Status %s: ", resp_status);
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
            System.out.printf(", retrying in %d seconds ...\n", (RETRY_BASE ^ retry));
            Thread.sleep(1000 * (RETRY_BASE ^ retry));  // 1000ms*(2^n) -> 1s, 2s, 4s, 8s, give up
          }
          else
          {
            System.out.println(".");
            retry = RETRY_MAX;                            // exit loop
          }
        }
      }
    }
    catch (IOException e)          { System.out.println("\nERROR: IOException");          System.exit(1); }
    catch (InterruptedException e) { System.out.println("\nERROR: InterruptedException"); System.exit(1); }
    return resp_status < 400;
  }


  //===================
  // 1. PROGRAM SETUP ||
  //===================

  public static void process_args(String[] args)
  {
    // Ensure correct num of args
    if (args.length != 4)
    {
      System.out.println("Error: Incorrect number of arguments.");
      System.exit(1);
    }
    
    // Store args
    for (int i = 0; i < 3; i++) { api_keys[i+1] = args[i]; }
                                  city          = args[3];
  }

  public static void print_intro()
  {
    System.out.println("\n//==========");
    System.out.println(  "// INTRO //");
    System.out.println(  "//========");

    System.out.println("\nMyCity.java");
    System.out.println(  " Inp:  U.S. city name (spaces -> underscores)");
    System.out.println(  " Outp: Local info from 3 independent APIs");
    System.out.printf (  " APIs: %s & %s, %s, %s\n", api_nams[0], api_nams[1].substring(15), api_nams[2], api_nams[3]);
    System.out.println(  " Algo: Exponential Backoff (delay between calls increases exponentially)");
    System.out.printf (  " Vars: RETRY_BASE=%d, RETRY_MAX=%d\n", RETRY_BASE, RETRY_MAX);

    System.out.println("\nHTTP Response Status Codes Key:");
    System.out.println(  " Accepted: 100s=Informtional,  200s=Success,  300s=Redirects");
    System.out.println(  " Rejected: 400s=ClientErr");
    System.out.println(  " Retried:  429 =TooManyReqs,   500s=ServerErr");


    System.out.println("\n\n//==============");
    System.out.println(    "// API CALLS //");
    System.out.println(    "//============");
  }


  //===========================
  // 2. API RESPONSE HANDLING ||
  //===========================

  public static void call_parse_apis()
  {
    client  = HttpClient.newBuilder().version(Version.HTTP_1_1).followRedirects(Redirect.ALWAYS).build();
    for (int i = 0; i < 4; i++) switch (i)
    {
      case 0:
        http_build_req_v1(i, city, null, null, null, api_keys[i]);
        if (http_call(i))   // Fails: not HTTPS
        {
          System.out.println(resp.toString());
          lat = "90";
          lon = "90";
          results += String.format
          (
            "\n%s & %s:\n"
            + " Latitude:          %s\n"
            + " Longitude:         %s\n",
            api_nams[0], api_nams[1].substring(15), lat, lon
          );
        }
        break;
        
      case 1:
        // RootObject weatherDetails = JsonConvert.DeserializeObject<RootObject>(response)!;
        // int todayTemp = (int) weatherDetails.main.temp;
        // if (todayTemp > 55) {Console.WriteLine("surprisingly warm today");}
        http_build_req_v1(i, lat, lon, null, null, api_keys[i]);
        if (http_call(i))   // Fails: not HTTPS
        {
          System.out.println(resp.toString());
          results += String.format
          (
              " Curr. Weather:     %s\n"
            + " Chance of Rain:    %s\n"
            + " Humidity:          %s\n"
            + " High  Temperature: %s\n"
            + " Curr. Temperature: %s\n"
            + " Low   Temperature: %s\n",
            "weath", "rain", "humid", "hi", "curr", "lo"
          );
        }
        break;

      case 2:
        http_build_req_v2(i, lat, lon, api_keys[i]);
        if (http_call(i))   // Fails: status 400
        {
          System.out.println(resp.toString());
          results += String.format
          (
            "\n%s:\n"
            + " Event: %s\n",
            api_nams[i], "event"
          );
        }
        break;

      case 3:
        http_build_req_v1
        (
          i,                                            api_keys[i],
          Double.toString(Double.parseDouble(lat)-0.5), Double.toString(Double.parseDouble(lon)-0.5),
          Double.toString(Double.parseDouble(lat)+0.5), Double.toString(Double.parseDouble(lon)+0.5)
        );
        if (http_call(i))   // Fails: illegal char 129
        {
          System.out.println(resp.toString());
          results += String.format
          (
            "\n%s\n"
            + " Event: %s\n",
            api_nams[i], "event"
          );
        }
        break;
        
    }
  }
}