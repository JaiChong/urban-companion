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
    print_intro();

    // 2. API CALLS
    call_apis();
    print_res();
  }


  //===============
  // 0. RESOURCES ||
  //===============

  //==========================
  // 0.1. Class-scope Vars //
  //========================

  // Program args
  public static String location;
  
  // HTTP calls
  public static HttpClient client;
  public static HttpRequest request;
  public static boolean loop_calls;
  public static HttpResponse<String> response;
  public static int status;
  
  // Storage for backtracking
  public static String curr_base_url;
  public static String curr_url;
  
  // Prints
  public static String stop_reason = "reaching the num_hops limit";


  //=========================
  // 0.2. HTTP Call Funcs //
  //=======================

  public static String normalize_url (String url)
  {
    if (url.charAt(4) != 's')
      url = url.substring(0, 4) + 's' + url.substring(4);
    if (url.charAt(url.length()-1) != '/')
      url += '/';
    return url;
  }

  public static boolean http_call()
  {
    System.out.println("\nMaking HTTP Call...");
    client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    request = HttpRequest.newBuilder().uri(URI.create("http://google.com")).build();  //todo
    
    curr_url = normalize_url(request.uri().toString());    // 2.1 Treats URLs as HTTPS and having a trailing slash
    status = send_req_and_get_status_code();               // 2.2 Retries Server Errors once
    return status >= 200 && status < 400;
  }

  public static int send_req_and_get_status_code()
  {
    // client.send() -> Exceptions
    try
    {
      // retries Server Errors once
      int retries = 0;
      do
      {
        response = client.send(request, BodyHandlers.ofString());
        status = response.statusCode();
        if (status >= 500) retries++;
      }
      while (retries == 1);
    }
    catch (IOException | InterruptedException e) { System.out.println(e.getMessage()); System.exit(1); }
    return status;
  }


  //===================
  // 1. PROGRAM SETUP ||
  //===================

  public static void process_args(String[] args)
  {
    // Ensure correct num of args
    if (args.length != 1)
    {
      System.out.println("Error: Incorrect number of arguments.");
      System.exit(1);
    }
    
    // Store args
    location = args[0];
  }

  public static void print_intro() {}


  //===============
  // 2. API CALLS ||
  //===============

  public static void call_apis()
  {
    // Console.WriteLine("Making the API Call...");
    // using (var client = new HttpClient())
    // {
    //   client.BaseAddress = new Uri ("http://api.openweather.org/data/2.5/");
    //   HttpResponseMessage response = client.GetAsync("weather?q=Chicago&appid=<secret>").Result();
    //   response.EnsureSuccessStatusCode();
    //   string result = response.Content.ReadAsStringAsync().Result;
    //   Console.WriteLine("Result: " + result);
    //   Rootobject weatherDetails = JsonConvert.DeserializeObject<Rootobject>(result)!;
    //   int todayTemp = (int) weatherDetails.main.temp;
    //   if (todayTemp > 55) Console.WriteLine("surprisingly warm today");
    // }
  }

  public static void print_res() {}
}