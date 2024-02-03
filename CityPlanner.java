/* P2: CityPlanner.java
 * CSS 436: Cloud Computing
 * Based on ../p1/WebCrawl.java by Prof. Dimpsey and Jaimi Chong
 * Completed by Jaimi Chong in Feb. 2024
 * 
 * Testing:
 * $ javac CityPlanner.java
 * $ java CityPlanner start_URL num_hops
 *    e.g. args = [ "http://faculty.washington.edu/dimpsey/", "100" ]
 */

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CityPlanner
{
  public static void main(String[] args)
  {
    prog_setup(args);     // 1. Program setup
    http_call_loop();     // 2. HTTP call loop
    print_res_final();    // 3. Print results
    print_grader_note();
  }


  //=======================
  // 0. CLASS-SCOPE VARS //
  //=======================

  // Grader
  public static boolean treat_all_as_trailing_slash = false;

  // Program args
  public static String start_url;
  public static int num_hops;
  
  // HTTP calls
  public static HttpClient client;
  public static HttpRequest request;
  public static boolean loop_calls;
  public static HttpResponse<String> response;
  public static int status;
  
  // Counters and storage for backtracking
  public static String curr_base_url;
  public static String curr_url;
  public static ArrayList<String> hist_urls = new ArrayList<String>();
  public static int num_backtracks = 0;
  public static boolean is_backtrack = false;
  public static Map<String, Integer> map_of_matcher_starts = new HashMap<String, Integer>();
  
  // Prints
  public static int curr_hop = 0;
  public static Map<String, String> map_of_alt_urls = new HashMap<String, String>();
  public static String stop_reason = "reaching the num_hops limit";
  

  //====================
  // 1. PROGRAM SETUP //
  //====================

  public static void prog_setup(String[] args)
  {
    // Ensure correct num of args
    if (args.length != 2)
    {
      System.out.println("Error: Incorrect number of arguments.");
      System.exit(1);
    }
    
    // Store args
    start_url = args[0];
    try { num_hops = Integer.parseInt(args[1]); }   // limit on max hops
    catch (NumberFormatException e) { System.out.println(e.getMessage() + ", a non-int type was used."); System.exit(1); }
    
    print_response_status_codes_key();
  }


  //=====================
  // 2. HTTP Call Loop //
  //=====================

  public static void http_call_loop()
  {
    // Initial setup
    System.out.println("\nMaking HTTP Call...");
    client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    request = HttpRequest.newBuilder().uri(URI.create(start_url)).build();

    // Call loop
    loop_calls = true;
    while (loop_calls && curr_hop <= num_hops)
    {
      curr_url = normalize_url(true, request.uri().toString());    // 2.1.
      if (!is_backtrack) hist_urls.add(curr_url);                             //      Log to history
      status = send_req_and_get_status_code();                                // 2.2. Retries Server Errors once
      if (status >= 200 && status < 400) get_next_url();                      // 2.3. if      (Success or Redirect)
      else                               backtrack_url();                     //      else if (Informational, Client Error, retried Server Error)
    }
  }


  //=====
  // 2.1 
  public static String normalize_url (boolean is_current, String orig_URL)
  {
    // if (URL lacks base address) Add it to the start, else if (base address has changed) Update it
    if (orig_URL.charAt(0) != 'h') orig_URL = curr_base_url + orig_URL;
    else if (is_current)
    {
      // (orig_URL has symbol chars after the first 2 '/' chars) Update curr_base_url
      if (orig_URL.lastIndexOf('/' | '#') >= 8) curr_base_url = orig_URL.substring(0, orig_URL.indexOf('/' | '#', 8));
      else curr_base_url = orig_URL;
    }    
    String res_url = orig_URL;

    // Treat all URLs as following HTTPS
    if (res_url.charAt(4) != 's')
    res_url = res_url.substring(0, 4) + 's' + res_url.substring(4);
    
    // Treat all URLs as having/lacking a trailing slash
    if (treat_all_as_trailing_slash)
    {
      if (res_url.charAt(res_url.length()-1) != '/')
        res_url += '/';
    }
    else
      if (res_url.charAt(res_url.length()-1) == '/')
        res_url = res_url.substring(0, res_url.length()-1);

    // Log any changes
    if (!res_url.equals(orig_URL)) map_of_alt_urls.putIfAbsent(res_url, orig_URL);

    return res_url;
  }


  //=====
  // 2.2 
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


  //======
  // 2.3a
  public static void get_next_url()
  {
    // RegEx Search of current page
    Pattern pattern = Pattern.compile("<a href=\\\".+?\\\"");
    Matcher matcher = pattern.matcher(response.body());
    int matcher_start_char = 0;
    
    // Absolute URL matcher loop
    String next_url = "";
    do {
      // if (not a backtrack) Log matcher start char index, else Update matcher_start_char
      if (map_of_matcher_starts.putIfAbsent(curr_url, 0) != null)
        matcher_start_char = map_of_matcher_starts.get(curr_url);
      
      // if (RegEx found a first/next URL) Prepare for next loop, else Stop HTTP call loop and update reason
      if (matcher.find(matcher_start_char))
      {
        next_url = normalize_url(false, matcher.group().replaceAll("(.+)?\"((.+)?)\"", "$2"));  // 2.1.
        map_of_matcher_starts.replace(curr_url, matcher.end());                                            //      Update curr_url's matcher start char index
        
        if (is_backtrack || !hist_urls.contains(next_url))
        {
          if (!is_backtrack) print_res_per_hop(true);
          curr_hop++;
          request = HttpRequest.newBuilder().uri(URI.create(next_url)).build();
        }
      }
      else
      {
        loop_calls = false;
        stop_reason = "reaching a page end before finding more hyperlinks";
      }
    }
    while (hist_urls.contains(next_url));
    is_backtrack = false;
  }


  //======
  // 2.3b
  public static void backtrack_url()
  {
    // if (start_URL) stop loop and update reason
    if (curr_hop == 0)
    {
      loop_calls = false;
      stop_reason = "start_URL responded with status " + status;
    }
    else
    {
      print_res_per_hop(false);
      curr_hop--;
      is_backtrack = true;
      num_backtracks++;
      request = HttpRequest.newBuilder()
        .uri(URI.create(hist_urls.get(curr_hop + num_backtracks - 1)))
        .build();
    }
  }


  //=============
  // 3. PRINTS //
  //=============

  public static void print_response_status_codes_key()
  {
    System.out.println("\nKey for Reponse Status Codes:");
    System.out.println(" [100,199]: Informational");
    System.out.println(" [200,299]: Success");
    System.out.println(" [300,399]: Redirect");
    System.out.println(" [400,499]: Client Error");
    System.out.println(" [500,599]: Server Error (retried once)");
  }

  public static void print_res_per_hop(boolean is_200s_300s)
  {
    // if (curr_url was altered in storage) Revert before print
    if (map_of_alt_urls.containsKey(curr_url)) curr_url = map_of_alt_urls.get(curr_url);
    
    if (is_200s_300s)
    System.out.println(" Hop " + curr_hop + " (Status " + status + "): " + curr_url);                             // Breaks on Wikipedia links: (String.format(" Hop %2d (Status " + status + "): " + curr_URL, curr_hop))
    else
    System.out.println(" Hop " + curr_hop + " ( ERROR " + status + "): Backtracking from " + curr_url + " ...");  // Breaks on Wikipedia links: (String.format(" Hop %2d ( ERROR " + status + "): Backtracking from " + curr_URL + " ...", curr_hop))
  }
  
  public static void print_res_final()    { System.out.println("\nResult:\n Stopped before Hop " + (curr_hop) + " due to " + stop_reason + "."); }
  public static void print_grader_note()  { System.out.println("\nNOTE TO GRADER:\n Added boolean `treat_all_as_having_trailing_backslashes` as first var in file for your convenience!"); }
}