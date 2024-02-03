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

    // 2. API HANDLING
    if (http_call(URL_API_WEATHER))     parse_print_resp("weather");
    if (http_call(URL_API_HOTELS))      parse_print_resp("hotels");
    if (http_call(URL_API_RESTAURANTS)) parse_print_resp("restaurants");
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
  
  // API URLs
  public static final String URL_API_WEATHER     = "http://api.openweather.org/data/2.5/weather?q=&appid=";
  public static final String URL_API_HOTELS      = "";
  public static final String URL_API_RESTAURANTS = "";

  // HTTP calls
  public static HttpClient           client;
  public static HttpRequest          req;
  public static HttpResponse<String> resp;
  public static int                  resp_status;
  

  //=============================
  // 0.2. HTTP Call Functions //
  //===========================
  
  public static boolean http_call(String url)
  {
    // System.out.println("\nCalling " + api_name + " ...");
    client  = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    req     = HttpRequest.newBuilder().uri(URI.create(url + city + "&appid=" + secret)).build();
    update_resp_exp_backoff();
    return resp_status >= 200 && resp_status < 400;
  }

  public static void update_resp_exp_backoff()
  {
    // client.send() -> Exceptions
    try
    {
      // retries Server Errors once
      int retries = 0;
      do
      {
        resp = client.send(req, BodyHandlers.ofString());
        resp_status = resp.statusCode();
        if (resp_status >= 500) retries++;
      }
      while (retries == 1);
    }
    catch (IOException | InterruptedException e) { System.out.println(e.getMessage()); System.exit(1); }
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


  //==================
  // 2. API HANDLING ||
  //==================

  public static void parse_print_resp(String api_type)
  {
    // RootObject weatherDetails = JsonConvert.DeserializeObject<RootObject>(response)!;
    switch (api_type)
    {
      case "weather":
        // int todayTemp = (int) weatherDetails.main.temp;
        // if (todayTemp > 55) Console.WriteLine("surprisingly warm today");
        break;

      case "hotels":
        break;
      
      case "restaurants":
        break;
    }
  }
}