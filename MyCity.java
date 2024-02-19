/* MyCity.java
 *  CSS 436: Cloud Computing, Project 2
 * 
 * Input:
 *  $ javac MyCity.java
 *  $ java MyCity [String:city] [String:api_key_1]    [String:api_key_2]  [String:api_key_3]
 *         e.g.   San_Francisco (OpenWeatherMap key)  (Yelp Fusion key)   (TomTom Traffic key)
 * 
 * Output:
 *  Data gathered from 3 independent RESTful API
 * 
 * Based on:
 *  ../p1/WebCrawl.java by Prof. Dimpsey, Jaimi Chong
 *  10 LOC Boilerplate  by Prof. Dimpsey
 * 
 * Completed by:
 *  Jaimi Chong in Feb. 2024
 */


import java.io.IOException;
import java.lang.Double;
import java.lang.Thread;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MyCity
{
  //================================
  // 0. MAIN() / TABLE OF CONTENTS ||
  //================================
  
  public static void main(String[] args)
  {
    // 1.   PROGRAM SETUP
            process_args(args);
            print_intro();

    // 2.   API HANDLING
            call_parse_APIs();
            System.out.println(results+"\n\n");

    // R.   RESOURCES
    // R.0. API Documentation
    // R.1. Class-scope Variables
    // R.2. HTTP Call Functions
    // R.3. RegEx Parse Function
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
                                  city        = args[0].replaceAll(" ", "%20");   // Accounts for URL encoding
    for (int i = 1; i < 4; i++) { api_keys[i] = args[i]; }
  }

  public static void print_intro()
  {
    System.out.println("\n\n//==========");
    System.out.println(    "// INTRO //" );
    System.out.println(    "//========"  );

    System.out.println(  "\nMyCity.java"                                                                                     );
    System.out.println(    " Input:      City name"                                                                          );
    System.out.println(    " Output:     Local info from 3 independent APIs"                                                 );
    System.out.printf (    " APIs:       %s & %s, %s, %s\n", api_nams[0], api_nams[1].substring(15), api_nams[2], api_nams[3]);
    System.out.println(    " Techniques: Exponential Backoff for response statuses 429 and 500s, RegEx for parsing"          );
    System.out.printf (    " Constants:  RETRY_BASE=%d, RETRY_MAX=%d\n", RETRY_BASE, RETRY_MAX                               );

    System.out.println(  "\nHTTP Response Status Codes Key:"                              );
    System.out.println(    " Accepted: 100s=Informtional,  200s=Success,   300s=Redirects");
    System.out.println(    " Rejected: 400s=ClientErr"                                    );
    System.out.println(    " Retried:  429 =TooManyReqs,   500s=ServerErr"                );


    System.out.println("\n\n//==============");
    System.out.println(    "// API CALLS //" );
    System.out.println(    "//============"  );
  }


  //===========================
  // 2. API RESPONSE HANDLING ||
  //===========================
  
  public static void call_parse_APIs()
  {
    client  = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    for (int i = 0; i < 4; i++) switch (i)
    {
      case 0:   // OpenWeatherMap Geocoding
        http_build_req(i, city, "", "", "", api_keys[1]);
        results += String.format("\n%s & %s:\n", api_nams[0], api_nams[1].substring(15));
        if (http_call(i))
        {
          parse_elements("state", "country", "lat", "lon");
          api_elements.replace("state",
          (
            (api_elements.get("state") != null)
            ? api_elements.get("state") + ", "
            : ""
          ));
          lat = api_elements.get("lat");
          lon = api_elements.get("lon");
          results += String.format
          (
              " Location:         %s, %s%s    \n"
            + " Coordinates:      %s° N, %s° E\n",

            city.replaceAll("%20", " "),  api_elements.get("state"),  api_elements.get("country"),
            lat,                          lon
          );
        }
        else { results += " Call failed with status " + resp_status + "."; }
        break;
        
        case 1:   // OpenWeatherMap Current Weather Data
        http_build_req(i, lat, lon, "", "", api_keys[1]);
        if (http_call(i))
        {
          parse_elements("description", "humidity", "temp_max", "temp", "temp_min");
          results += String.format
          (
            " Current Weather:  %s  \n"
          + " Temp, High:       %.3f°F\n"
          + " Temp, Current:    %.3f°F\n"
          + " Temp, Low:        %.3f°F\n"
          + " Humidity:         %s%%\n",

            api_elements.get("description"),
            ((Double.parseDouble(api_elements.get("temp_max")) * 9 / 5) - 459.67),
            ((Double.parseDouble(api_elements.get("temp"    )) * 9 / 5) - 459.67),
            ((Double.parseDouble(api_elements.get("temp_min")) * 9 / 5) - 459.67),
            api_elements.get("humidity")
          );
        }
        else { results += " Call failed with status " + resp_status + "."; }
        break;

      case 2:   // Yelp Fusion
        // // Provided call syntax, using a package
        // OkHttpClient client = new OkHttpClient();
        // Request request = new Request.Builder()
        //   .url("https://api.yelp.com/v3/events?limit=3&sort_by=desc&sort_on=popularity")
        //   .get()
        //   .addHeader("accept", "application/json")
        //   .build();
        // Response response = client.newCall(request).execute();

        // // Debug
        // req = HttpRequest.newBuilder()
        //   .uri(URI.create("https://api.yelp.com/v3/events?locale=en_US"))
        //   .GET()
        //   .header("User-Agent", "")            // didn't work
        //   .header("Accept", "application/json")
        //   .header("Authorization", "Bearer "+api_keys[i])
        //   .build();
        // try
        // {
        //   resp = client.send(req, BodyHandlers.ofString());
        //   resp_status = resp.statusCode();
        //   System.out.println(resp_status);    // Breakpoint here
        // }
        // catch (IOException e)          { System.out.println("IO");          System.exit(1); }
        // catch (InterruptedException e) { System.out.println("Interrupted"); System.exit(1); }

        http_build_req_Yelp(i, lat, lon, api_keys[i]);
        results += String.format("\n%s:\n", api_nams[i]);
        if (http_call(i))   // FIXME: 403 Forbidden access to resource
        {
          // System.out.println(resp.body());
          parse_elements("element1", "element2", "element3");
          results += String.format
          (
              " Element1: %s\n"
            + " Element2: %s\n"
            + " Element3: %s\n",

            api_elements.get("element1"),
            api_elements.get("element2"),
            api_elements.get("element3")
          );
        }
        else { results += " Call failed with status " + resp_status + ".\n"; }
        break;

      case 3:   // TomTom Traffic Incident Details
        double num_lat = Double.parseDouble(lat);
        double num_lon = Double.parseDouble(lon);
        http_build_req
        (
          i,                            api_keys[i],
          Double.toString(num_lat-0.5), Double.toString(num_lat-0.5),
          Double.toString(num_lat+0.5), Double.toString(num_lon+0.5)
        );
        results += String.format("\n%s:\n", api_nams[i]);
        if (http_call(i))   // FIXME: 403 Forbidden access to resource
        {
          // System.out.println(resp.body());
          parse_elements("element1", "element2", "element3");
          results += String.format
          (
              " Element1: %s\n"
            + " Element2: %s\n"
            + " Element3: %s\n",

            api_elements.get("element1"),
            api_elements.get("element2"),
            api_elements.get("element3")
          );
        }
        else { results += " Call failed with status " + resp_status + "."; }
        break;
        
    }
  }


  //===============
  // R. RESOURCES ||
  //===============

  //===========================
  // R.0. API Documentation //
  //=========================

  // https://openweathermap.org/api/geocoding-api#direct
  // https://openweathermap.org/current
  // https://docs.developer.yelp.com/reference/v3_events_search
  // https://developer.tomtom.com/traffic-api/documentation/traffic-incidents/incident-details

  //===============================
  // R.1. Class-scope Variables //
  //=============================

  // HTTP Calls
  public static HttpClient           client;
  public static HttpRequest          req;
  public static HttpResponse<String> resp;
  public static int                  resp_status;
  public static final int            RETRY_BASE = 2;
  public static final int            RETRY_MAX  = 4;

  // API Requests
  public static String[] api_nams = 
  {
    "OpenWeatherMap Geocoding",
    "OpenWeatherMap Current Weather Data",
    "Yelp Fusion",
    "TomTom Traffic Incident Details"
  };
  public static String[] api_urls =
  {
    "https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s%s%s%s",
    "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s%s%s",
    "https://api.yelp.com/v3/events?sort_on=time_start&latitude=%s&longitude=%s",
    // "https://api.yelp.com/v3/categories/events?sort_on=time_start&latitude=%s&longitude=%s",
    // "https://api.yelp.com/v3/categories/restaurants?latitude=%s&longitude=%s",
    "https://api.tomtom.com/traffic/services/5/incidentDetails?key=%s&bbox=%s,%s,%s,%s&fields=&rcub;incidents&rcub;properties&rcub;iconCategory,magnitudeOfDelay,events&rcub;code,description,iconCategory&lcub;,startTime,endTime,from,to,length,delay,roadNumbers,timeValidity,probabilityOfOccurrence,numberOfReports,lastReportTime&lcub;&lcub;&lcub;&language=en-US&timeValidityFilter=present,future"
  };
  public  static String   city;                     // args[0]
  private static String[] api_keys = new String[4]; // { null, args[1], args[2], args[3] }
  public  static String   lat      = "-1";
  public  static String   lon      = "-1";
  
  // API Responses
  public static Map<String, String> api_elements = new HashMap<String, String>();
  public static String              results      = 
    "\n\n//============\n"
    +   "// RESULTS // \n"
    +   "//==========  \n"  ;

  //=============================
  // R.2. HTTP Call Functions //
  //===========================
  
  public static void http_build_req (int num, String arg1, String arg2, String arg3, String arg4, String arg5)
  {
    req = HttpRequest.newBuilder()
      .uri(URI.create(String.format(api_urls[num], arg1, arg2, arg3, arg4, arg5)))
      .header("Accept", "application/json")
      .GET()
      .build();
  }

  public static void http_build_req_Yelp (int num, String arg1, String arg2, String hdr_key)
  {
    req = HttpRequest.newBuilder()
      .uri(URI.create(String.format(api_urls[num], arg1, arg2)))
      .header("Accept", "application/json")
      .header("Authorization", "Bearer " + hdr_key)
      .GET()
      .build();
  }

  public static boolean http_call(int num)
  {
    System.out.println("\nCalling " + api_nams[num] + " ...");
    
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

  //==============================
  // R.3. RegEx Parse Function //
  //============================

  public static void parse_elements(String... elements)
  {
    Pattern pattern;
    Matcher matcher;
    for (String element : elements)
    {
      pattern = Pattern.compile(element +"\":\"?(.+?)\"?(?:,|}|])");
      matcher = pattern.matcher(resp.body());
      if (matcher.find()) { api_elements.put(element, matcher.group(1)); }
    }
  }
}