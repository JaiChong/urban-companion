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
  //===================
  // #region 0. MAIN()
  //===================
  public static void main(String[] args)
  {
    // 1. PROGRAM SETUP
    process_args(args);
    print_intro();

    // 2. API HANDLING
    call_parse_APIs();
    System.out.println(results+"\n\n");
  }


  //==========================
  // #region 1. PROGRAM SETUP
  //==========================

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

    System.out.println(  "\nMyCity.java"                                                                                      );
    System.out.println(    "  Input:      City name"                                                                          );
    System.out.println(    "  Output:     Local info from 3 independent APIs"                                                 );
    System.out.printf (    "  APIs:       %s & %s, %s, %s\n", api_nams[0], api_nams[1].substring(15), api_nams[2], api_nams[3]);
    System.out.println(    "  Techniques: Exponential Backoff for response statuses 429 and 500s, RegEx for parsing"          );
    System.out.printf (    "  Constants:  RETRY_BASE=%d, RETRY_MAX=%d\n", RETRY_BASE, RETRY_MAX                               );

    System.out.println(  "\nHTTP Response Status Codes Key:"                              );
    System.out.println(    "  Accepted: 100s=Informtional,  200s=Success,   300s=Redirects");
    System.out.println(    "  Rejected: 400s=ClientErr"                                    );
    System.out.println(    "  Retried:  429 =TooManyReqs,   500s=ServerErr"                );


    System.out.println("\n\n//==============");
    System.out.println(    "// API CALLS //" );
    System.out.println(    "//============"  );
  }


  //=========================
  // #region 2. API HANDLING
  //=========================
  public static void call_parse_APIs()
  {
    client  = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
    for (int i = 0; i < 4; i++) switch (i)
    {
      //=================================
      // #region 2.0. OpenWeatherMap (1)
      //=================================
      case 0:
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
              "  Location:         %s, %s%s    \n"
            + "  Coordinates:      %s° N, %s° E\n",

            city.replaceAll("%20", " "),  api_elements.get("state"),  api_elements.get("country"),
            lat,                          lon
          );
        }
        else { results += "  Call failed with status " + resp_status + "."; }
        break;
        
      //=================================
      // #region 2.1. OpenWeatherMap (2)
      //=================================
      case 1:
        http_build_req(i, lat, lon, "", "", api_keys[1]);
        if (http_call(i))
        {
          parse_elements("description", "humidity", "temp_max", "temp", "temp_min");
          results += String.format
          (
            "  Current Weather:  %s  \n"
          + "  Temp, High:       %.3f°F\n"
          + "  Temp, Current:    %.3f°F\n"
          + "  Temp, Low:        %.3f°F\n"
          + "  Humidity:         %s%%\n",

            capitalize_first_letters(api_elements.get("description")),
            ((Double.parseDouble(api_elements.get("temp_max")) * 9 / 5) - 459.67),
            ((Double.parseDouble(api_elements.get("temp"    )) * 9 / 5) - 459.67),
            ((Double.parseDouble(api_elements.get("temp_min")) * 9 / 5) - 459.67),
            api_elements.get("humidity")
          );
        }
        else { results += "  Call failed with status " + resp_status + "."; }
        break;

      //==========================
      // #region 2.2. Yelp Fusion
      //==========================
      case 2:
        http_build_req_Yelp(i, lat, lon, api_keys[i]);
        results += String.format("\n%s:\n", api_nams[i]);
        if (http_call(i))
        {
          int num_events = parse_set(
            "name",
            "cost",
            "time_start", "time_end",
            "business_id", "address1",
            "event_site_url"
          );
          for (int j = 1; j <= num_events; j++)
          {
            String business_id = "";
            if (!api_elements.get(j+"_business_id").equals("null"))
            {
              business_id = capitalize_first_letters(api_elements.get(j+"_business_id"));
              if (business_id.lastIndexOf(" "+city) != -1)
              {
                business_id = business_id.substring(0, business_id.lastIndexOf(" "+city)) + ", ";
              }
            }

            results += String.format
            (
                "  %d. %s:\n"
              + "       Cost:     $%.2f\n"
              + "       Start:    %s\n%s"
              + "       Address:  %s%s\n"
              + "       URL:      %s\n",
  
              j,
              api_elements.get(j+"_name"),
              !api_elements.get(j+"_cost")       .equals("null")
                ? Float.valueOf(api_elements.get(j+"_cost"))
                : 0,
              (!api_elements.get(j+"_time_start").equals("null") && api_elements.get(j+"_time_start").length() >= 16)
                ?   api_elements.get(j+"_time_start").substring( 0, 10) + ", "
                  + api_elements.get(j+"_time_start").substring(11, 16)
                : "",
              (!api_elements.get(j+"_time_end")  .equals("null") && api_elements.get(j+"_time_end")  .length() >= 16)
                ? "       End:      "
                  + api_elements.get(j+"_time_end")  .substring( 0, 10) + ", "
                  + api_elements.get(j+"_time_end")  .substring(11, 16) + "\n"
                : "",
              business_id,
              api_elements.get(j+"_address1"),
              api_elements.get(j+"_event_site_url")  .substring(0, api_elements.get(j+"_event_site_url").indexOf('?'))
            );
          }
        }
        else { results += "  Call failed with status " + resp_status + ".\n"; }
        break;

      //=============================
      // #region 2.3. TomTom Traffic
      //=============================
      case 3:
        double num_lon = Double.parseDouble(lon);
        double num_lat = Double.parseDouble(lat);
        http_build_req
        (
          i,                            api_keys[i],
          Double.toString(num_lon-0.5), Double.toString(num_lat-0.5),
          Double.toString(num_lon+0.5), Double.toString(num_lat+0.5)
        );
        results += String.format("\n%s:\n", api_nams[i]);
        if (http_call(i))
        {
          int num_incidents = parse_set("element1", "element2", "element3");
          results += String.format
          (
              "  Element1:  %s\n"
            + "  Element2:  %s\n"
            + "  Element3:  %s\n",

            api_elements.get("element1"),
            api_elements.get("element2"),
            api_elements.get("element3")
          );
        }
        else { results += "  Call failed with status " + resp_status + "."; }
        break;
    }
  }


  //======================
  // #region R. RESOURCES
  //======================

  //=======================
  // #region R.0. API Docs
  //=======================
  // https://openweathermap.org/api/geocoding-api#direct
  // https://openweathermap.org/current
  // https://docs.developer.yelp.com/reference/v3_events_search
  // https://developer.tomtom.com/traffic-api/documentation/traffic-incidents/incident-details


  //=========================
  // #region R.1. Class Vars
  //=========================

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
    "Yelp Fusion Events",
    "TomTom Traffic Incidents"
  };
  public static String[] api_urls =
  {
    "https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s%s%s%s",
    "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s%s%s",
    "https://api.yelp.com/v3/events?sort_on=time_start&latitude=%s&longitude=%s",
    // "https://api.yelp.com/v3/categories/events?sort_on=time_start&latitude=%s&longitude=%s",
    // "https://api.yelp.com/v3/categories/restaurants?latitude=%s&longitude=%s",
    "https://api.tomtom.com/traffic/services/5/incidentDetails?key=%s&bbox=%s,%s,%s,%s&fields=%%7Bincidents%%7Bproperties%%7BiconCategory,magnitudeOfDelay,events%%7Bcode,description,iconCategory%%7D,startTime,endTime,from,to,length,delay,roadNumbers,timeValidity,probabilityOfOccurrence,numberOfReports,lastReportTime%%7D%%7D%%7D&language=en-US&timeValidityFilter=present"
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
  

  //=========================
  // #region R.2. HTTP Funcs
  //=========================
  
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
      // .method("GET", HttpRequest.BodyPublishers.noBody())
      .GET()
      .build();
  }

  public static boolean http_call(int num)
  {
    System.out.println("\nCalling " + api_nams[num] + " ...");
    
    // client.send()
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
          System.out.println("Failure.");
          parse_failure();
          if (api_elements.get("failure") != null)
          {
            System.out.printf("  %s.\n", api_elements.get("failure"));
          }
          
          if (retry < RETRY_MAX && (resp_status >= 500 || resp_status == 429))
          {
            System.out.printf("  Retrying in %d seconds ...\n", (RETRY_BASE ^ retry));
            Thread.sleep(1000 * (RETRY_BASE ^ retry));  // 1000ms*(2^n) -> 1s, 2s, 4s, 8s, give up
          }
          else
          {
            retry = RETRY_MAX;                          // exit loop
          }
        }
      }
    }
    catch (IOException e)          { System.out.println("\n  ERROR: IOException");          System.exit(1); }
    catch (InterruptedException e) { System.out.println("\n  ERROR: InterruptedException"); System.exit(1); }
    return resp_status < 400;
  }


  //==========================
  // #region R.3. RegEx Funcs
  //==========================

  public static void parse_elements(String... elements)
  {
    Pattern pattern;
    Matcher matcher;
    for (String element : elements)
    {
      pattern = Pattern.compile(element + "\":\"?(.+?)\"?(?:,|}|])");
      matcher = pattern.matcher(resp.body());
      if (matcher.find()) { api_elements.put(element, matcher.group(1)); }
    }
  }

  public static int parse_set(String... elements)
  {
    Pattern pattern;
    Matcher matcher;
    int count = 0;
    for (String element : elements)
    {
      count = 0;
      pattern = Pattern.compile(element + "\": \"?(.+?)\"?(?:,|}|])");
      matcher = pattern.matcher(resp.body());
      while (matcher.find()) { api_elements.put(((++count)+"_"+element), matcher.group(1)); }
    }
    return count;
  }

  public static String capitalize_first_letters(String element)
  {
    Pattern pattern = Pattern.compile("( |-)?(\\w)(\\w+)?");
    Matcher matcher = pattern.matcher(element);
    
    StringBuilder res = new StringBuilder();
    if    (matcher.find()) { matcher.appendReplacement(res,       (matcher.group(2).toUpperCase() + matcher.group(3))); }   // fencepost
    while (matcher.find()) { matcher.appendReplacement(res, " " + (matcher.group(2).toUpperCase() + matcher.group(3))); }
    return res.toString();
  }
  
  public static void parse_failure()
  {
    Pattern pattern = Pattern.compile("code\": \"(.+?)\"?(?:,)");
    Matcher matcher = pattern.matcher(resp.body());

    if (matcher.find()) {
      String code = matcher.group(1);
      pattern = Pattern.compile("(description|message)\": \"(.+?)\\.\"");
      matcher = pattern.matcher(resp.body());  
      if (matcher.find()) { api_elements.put("failure", (code + ": " + matcher.group(2))); }
    }
    else
    {
      pattern = Pattern.compile("<h\\d>(.+?)</h\\d>");
      matcher = pattern.matcher(resp.body());
      if (matcher.find()) { api_elements.put("failure", matcher.group(1)); }
    }

  }
}