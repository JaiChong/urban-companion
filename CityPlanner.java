/* P2: CityPlanner.java
 * CSS 436: Cloud Computing
 * Based on P1: WebCrawl.java by Prof. Dimpsey and Jaimi Chong
 * Completed by Jaimi Chong in Feb. 2024
 * 
 * Testing:
 * $ javac CityPlanner.java
 * $ java CityPlanner /*(String start_URL) (int num_hops)*\/
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
  // 0. Grader vars
  public static boolean treat_all_as_having_trailing_backslashes = false;

  // 1. Class vars
  // 1.1. Program args
  public static String start_URL;
  public static int num_hops;
  
  // 1.2. Vars for URL storage
  public static String curr_base_URL = "-1";
  public static ArrayList<String> hist_URLs = new ArrayList<String>();

  // 1.3. Vars for prints
  public static int curr_hop = 0;     // measured against num_hops and used in backtracking (see 1.3.)
  public static Map<String, String> map_of_alt_URLs = new HashMap<String, String>();
  public static String stop_reason = "reaching the num_hops limit";
  
  // 1.4. Counters and storage for backtracking
  public static int num_backtracks = 0;
  public static boolean is_backtrack = false;
  public static Map<String, Integer> map_of_matcher_starts = new HashMap<String, Integer>();
  
  public static void main(String[] args)
  {
    // 2. Program setup
    // 2.1. Ensure correct num of args
    if (args.length != 2)
    {
      System.out.println("Error: Incorrect number of arguments.");
      note_to_grader();
      System.exit(1);
    }
    
    // 2.2. Store args
    start_URL = args[0];
    try { num_hops = Integer.parseInt(args[1]); }   // limit on max hops
    catch (NumberFormatException e) { System.out.println(e.getMessage() + ", a non-int type was used."); note_to_grader(); System.exit(1); }
    
    // 2.3. Print key for response status codes
    System.out.println("Key for Reponse Status Codes:");
    System.out.println(" [100,199]: Informational");
    System.out.println(" [200,299]: Success");
    System.out.println(" [300,399]: Redirect");
    System.out.println(" [400,499]: Client Error");
    System.out.println(" [500,599]: Server Error (retried once)");
    
    // 3. HTTP call
    // 3.1. Initial HTTP call setup
    System.out.println("\nMaking HTTP Call...");
    HttpClient client = HttpClient.newBuilder()
      .followRedirects(Redirect.ALWAYS)
      .build();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(start_URL))
      .build();

    // 3.2. HTTP call loop
    boolean loop_calls = true;
    while (loop_calls && curr_hop <= num_hops)
    {
      // client.send() -> IO and Interrupted Exceptions
      try
      {
        // 3.2.1. Scope vars
        HttpResponse<String> response;
        int status;

        // 3.2.2. Normalize curr_URL format (logs any changes) and log into history
        String curr_URL = normalize_URL(true, request.uri().toString());
        if (!is_backtrack) hist_URLs.add(curr_URL);
        
        // 3.2.3. Send request, get response and its status code (retries [500,599]'s once)
        int retries = 0;
        do
        {
          response = client.send(request, BodyHandlers.ofString());
          status = response.statusCode();
          if (status >= 500) retries++;
        }
        while (retries == 1);
        
        // 3.2.4. Open URL in next loop if (status == [200,299]: Success or
        //                                              [300,399]: Redirect)
        if (status >= 200 && status < 400)
        {
          // RegEx Search of current page
          Pattern pattern = Pattern.compile("<a href=\\\".+?\\\"");
          Matcher matcher = pattern.matcher(response.body());
          int matcher_start_char = 0;
          
          // Absolute URL matcher loop
          String next_URL = "";
          do {
            // if (not a backtrack) Log matcher start char index
            // else Update matcher_start_char
            if (map_of_matcher_starts.putIfAbsent(curr_URL, 0) != null)
              matcher_start_char = map_of_matcher_starts.get(curr_URL);
            
              // if (RegEx found a first/next URL) Prepare for next loop
            if (matcher.find(matcher_start_char))
            {
              // Normalize next_URL and update curr_URL's matcher start char index
              next_URL = normalize_URL(false, matcher.group().replaceAll("(.+)?\"((.+)?)\"", "$2"));
              map_of_matcher_starts.replace(curr_URL, matcher.end());
              
              if (is_backtrack || !hist_URLs.contains(next_URL)) {
                // if (not a backtrack) Revert curr_URL, and print status and hop results
                if (!is_backtrack)
                {
                  if (map_of_alt_URLs.containsKey(curr_URL)) curr_URL = map_of_alt_URLs.get(curr_URL);
                  System.out.println(" Hop " + curr_hop + " (Status " + status + "): " + curr_URL);
                  // System.out.println(String.format(" Hop %2d (Status " + status + "): " + curr_URL, curr_hop));   // Breaks on some Wikipedia links
                }
                
                // Increment curr_hop;
                curr_hop++;
                
                // Set up next HTTP request
                request = HttpRequest.newBuilder()
                  .uri(URI.create(next_URL))
                  .build();
              }
            }
            // else if (RegEx didn't find a first/next URL) stop HTTP call loop and update reason
            else
            {
              loop_calls = false;
              stop_reason = "reaching a page end before finding more hyperlinks";
            }
          }
          while (hist_URLs.contains(next_URL));
          
          // Reset is_backtrack
          is_backtrack = false;
        }
        // 2.2.4.3. Backtrack in next loop if (status == [100,199]: Informational,
        //                                               [400,499]: Client Error, or
        //                                               [500,599]: Server Error (retried once))
        else
        {
          // if (start_URL) stop loop and update reason
          if (curr_hop == 0)
          {
            loop_calls = false;
            stop_reason = "start_URL responded with status " + status;
          }
          // if (not start_URL)
          else
          {
            // if (curr_URL was altered in storage) revert before print
            if (map_of_alt_URLs.containsKey(curr_URL)) curr_URL = map_of_alt_URLs.get(curr_URL);
            System.out.println(" Hop " + curr_hop + " ( ERROR " + status + "): Backtracking from " + curr_URL + " ...");
            // System.out.println(String.format(" Hop %2d ( ERROR " + status + "): Backtracking from " + curr_URL + " ...", curr_hop));    // Breaks on some Wikipedia links

            // Update is_backtrack, increment backtracks, and decrement curr_hop
            is_backtrack = true;
            num_backtracks++;
            curr_hop--;
  
            // Backtrack to prev HTTP request
            request = HttpRequest.newBuilder()
              .uri(URI.create(hist_URLs.get(curr_hop + num_backtracks - 1)))
              .build();
          }
        }
      }
      catch (IOException | InterruptedException e) { System.out.println(e.getMessage()); note_to_grader(); System.exit(1); }
    }

    // 3.3. Print final result
    System.out.println("Result: Stopped before Hop " + (curr_hop) + " due to " + stop_reason + ".");
    note_to_grader();
  }

  public static String normalize_URL (boolean is_current, String orig_URL)
  {
    // if (URL lacks base address) Add it to the start
    if (orig_URL.charAt(0) != 'h') {
      orig_URL = curr_base_URL + orig_URL;
    }
    // else if (URL has base address but it has changed), update it
    else if (is_current)
    {
      // if (orig_URL has symbol chars after the first 2 '/' chars)
      if (orig_URL.lastIndexOf('/' | '#') >= 8) {
          curr_base_URL = orig_URL.substring(0, orig_URL.indexOf('/' | '#', 8));
      }
      // else if (orig_URL has no symbol chars after the first 2 '/' chars)
      else curr_base_URL = orig_URL;
    }
    
    String res_URL = orig_URL;

    // Treat all URLs as following HTTPS
    if (res_URL.charAt(4) != 's') res_URL = res_URL.substring(0, 4) + 's' + res_URL.substring(4);
    
    // Treat all URLs as either having or lacking trailing backslashes, based on global var
    if (treat_all_as_having_trailing_backslashes) {
      if (res_URL.charAt(res_URL.length()-1) != '/') res_URL += '/';
    }
    else {
      if (res_URL.charAt(res_URL.length()-1) == '/') res_URL = res_URL.substring(0, res_URL.length()-1);
    }

    // if (changed) Store orig_URL for prints
    if (!res_URL.equals(orig_URL)) map_of_alt_URLs.putIfAbsent(res_URL, orig_URL);

    return res_URL;
  }

  public static void note_to_grader() { System.out.println("\nNOTE TO GRADER: I have added a boolean variable `treat_all_as_having_trailing_backslashes` as the first variable in the file for your convenience!"); }
}