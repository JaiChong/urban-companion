/* P2: CityPlanner.java
 * CSS 436: Cloud Computing
 * Based on ../p1/WebCrawl.java by Prof. Dimpsey and Jaimi Chong
 * Completed by Jaimi Chong in Feb. 2024
 * 
 * Testing:
 * $ javac CityPlanner.java
 * $ java CityPlanner location
 *    e.g. args = [ "seattle" ]
 */

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;


public class CityPlanner
{
  public static void main(String[] args)
  {
    prog_setup(args);     // 1.   Program Setup
    print_intro();        // 0.2. Prints
    if (http_call());     // 2.   HTTP Call
    print_res();          // 0.2. Prints
  }


  //================
  // 0. REFERENCED ||
  //================

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


  //================
  // 0.2. Prints //
  //==============

  public static void print_intro() {}
  public static void print_res()   {}


  //===================
  // 1. PROGRAM SETUP ||
  //===================

  public static void prog_setup(String[] args)
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


  //===============
  // 2. HTTP CALL ||
  //===============

  public static boolean http_call()
  {
    System.out.println("\nMaking HTTP Call...");
    client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    request = HttpRequest.newBuilder().uri(URI.create("http://google.com")).build();  //todo
    
    curr_url = normalize_url(true, request.uri().toString());    // 2.1 Treats URLs as HTTPS and having a trailing slash
    status = send_req_and_get_status_code();                                // 2.2 Retries Server Errors once
    return status >= 200 && status < 400;
  }


  //==============================
  // 2.1. Normalize URL Format //
  //============================

  public static String normalize_url (boolean is_current, String url)
  {
    if (url.charAt(4) != 's')
      url = url.substring(0, 4) + 's' + url.substring(4);
    if (url.charAt(url.length()-1) != '/')
      url += '/';
    return url;
  }


  //==============================================
  // 2.2. Send Request and Get its Status Code //
  //============================================

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
}