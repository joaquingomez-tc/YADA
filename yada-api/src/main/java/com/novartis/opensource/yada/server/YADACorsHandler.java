/**
 * 
 */
package com.novartis.opensource.yada.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * <p>Keighty has <a href="https://katieleonard.ca/blog/2016/2016-03-29-preflight-check-with-cors/">a good explanation</a> of what's going on in here.</p>
 * <p>Also, there is the <a href="https://www.w3.org/TR/2020/SPSD-cors-20200602/">W3C CORS Specification</a>, and 
 * the new-ish <a href="https://fetch.spec.whatwg.org/#http-cors-protocol">W3C Fetch Specification</a>.</p>
 * <p>And of course, the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">MDN page</a> 
 * @author dvaron
 * @since 10.1.0
 *
 */
public class YADACorsHandler extends AbstractHandler {

  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_ALLOW_ORIGIN = "YADA.server.CORS.allow.origin";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_ALLOW_METHODS = "YADA.server.CORS.allow.methods";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_ALLOW_CREDENTIALS = "YADA.server.CORS.allow.credentials";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_ALLOW_HEADERS= "YADA.server.CORS.allow.headers";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_EXPOSE_HEADERS= "YADA.server.CORS.expose.headers";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_MAX_AGE = "YADA.server.CORS.max.age";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_CHAIN_PREFLIGHT = "YADA.server.CORS.chain.preflight";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_DEFAULT_METHODS = "GET,HEAD,POST";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_DEFAULT_ALLOW_HEADERS = "X-Requested-With,Content-Type,Accept,Origin";
  /**
   * Constant equal to {@value}. Used for enabling cors
   */
  private static final String CORS_WILDCARD = "*";
  /**
   * Content of YADA.properties, {@link YADAServer#getProperties()}
   */
  private static Properties props = YADAServer.getProperties();
  
  /**
   * Null constructor
   */
  public YADACorsHandler() {
    
  }
  
  /**
   * Processes {@code Origin} request header and compares with {@code Access-Control-Allow-Origin}
   * @param baseRequest the Jetty request object
   * @param request the servlet request object
   * @param response the servlet response object
   * @throws ServletException when the origins don't match
   * @throws IOException when the response cannot be modified
   */
  private void handleOrigins(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String val = props.getProperty(CORS_ALLOW_ORIGIN);
    if(val == null
        || val.equals(CORS_WILDCARD) 
        || val.equals("."+CORS_WILDCARD) 
        || val.length() == 0)
    {
      response.addHeader("Access-Control-Allow-Origin", CORS_WILDCARD);
    }
    else if(val != null && val.length() > 0)
    {
      String  origin    = request.getHeader("Origin");     
      String  prop      = props.getProperty(CORS_ALLOW_ORIGIN);
      String  rx        = prop.startsWith("http") ? prop : "^https?://"+prop;
      Pattern allowOrig = Pattern.compile(rx);
      Matcher origMatch = allowOrig.matcher(origin);      
      if(origMatch.matches())
      {
        response.addHeader("Access-Control-Allow-Origin", origin);
      }
      else
      {
        fail(baseRequest);
      }
    }
  }
  
  /**
   * Evaluates requested methods and sets to configured value in {@code YADA.server.CORS.allow.methods} or
   * the default set of {@code GET, HEAD, POST}
   * @param baseRequest the Jetty request object
   * @param request the servlet request object
   * @param response the servlet response object
   */
  private void handleAllowMethods(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
    String val = props.getProperty(CORS_ALLOW_METHODS);
    if(val == null)
    {
      response.addHeader("Access-Control-Allow-Methods", CORS_DEFAULT_METHODS);
    }
    else
    {
      response.addHeader("Access-Control-Allow-Methods", val);
    }
  }
  
  /**
   * Evaluates requested headers and sets to configured value in {@code YADA.server.CORS.allow.headers} or
   * the default set of {@code X-Requested-With,Content-Type,Accept,Origin}
   * @param baseRequest the Jetty request object
   * @param request the servlet request object
   * @param response the servlet response object
   */
  private void handleAllowHeaders(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
    String val = props.getProperty(CORS_ALLOW_HEADERS);
    if(val == null || val.contentEquals(CORS_WILDCARD) || val.contentEquals("."+CORS_WILDCARD))
    {
      response.addHeader("Access-Control-Allow-Headers", CORS_DEFAULT_ALLOW_HEADERS);
    }
    else
    {
      response.addHeader("Access-Control-Allow-Headers", val);
    }
  }
  
  /**
   * Evaluates requested max age headers and sets to configured value in {@code YADA.server.CORS.max.age} or
   * the default value of {@code 1800}
   * @param baseRequest the Jetty request object
   * @param request the servlet request object
   * @param response the servlet response object
   */
  private void handleMaxAge(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
    String val = props.getProperty(CORS_MAX_AGE);
    if(val == null)
    {
      response.addHeader("Access-Control-Max-Age", "1800");
    }
    else
    {
      try
      {
        int v = Integer.parseInt(val);
        response.addHeader("Access-Control-Max-Age", String.valueOf(v));
      }
      catch(NumberFormatException e)
      {
        response.addHeader("Access-Control-Max-Age", val);
      }
    }
  }
  
  /**
   * Evaluates requested credentials header and sets to configured value in {@code YADA.server.CORS.allow.credentials} or
   * the default value of {@code true}
   * @param baseRequest the Jetty request object
   * @param request the servlet request object
   * @param response the servlet response object
   */
  private void handleCredentials(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
    String val = props.getProperty(CORS_ALLOW_CREDENTIALS);
    if(val == null)
    {
      response.addHeader("Access-Control-Allow-Credentials", "true");
    }
    else
    {
      response.addHeader("Access-Control-Allow-Credentials", val);      
    }
  }
      
  /**
   * Evaluates requested exposed headers header and sets to configured value in {@code YADA.server.CORS.expose.headers} or
   * the default value of an empty string.
   * @param baseRequest the Jetty request object
   * @param request the servlet request object
   * @param response the servlet response object
   */
  private void handleExposeHeaders(Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
    String val = props.getProperty(CORS_EXPOSE_HEADERS);
    if(val == null)
    {
      response.addHeader("Access-Control-Expose-Headers", "");
    }
    else
    {
      response.addHeader("Access-Control-Expose-Headers", val);
    }
  }
  
  /**
   * Checks to ensure a request contains one of {@code GET}, {@code HEAD}, or {@code POST} 
   * for {@code method} and that there is no {@code Access-Control-Request-Header} 
   * header as required for preflight.
   * @param request the HTTP request object
   * @return true or false depending on header and method content
   */
  private boolean isSimpleRequest(HttpServletRequest request) {
    return Arrays.binarySearch(CORS_DEFAULT_METHODS.split(","), request.getMethod()) > -1
        && request.getHeader("Access-Control-Request-Method") == null;
    
  }
  
  /**
   * Checks to ensure a request is for method {@code OPTIONS}, and
   * that there is an {@code Access-Control-Request-Method} header required for preflight.
   * @param request the HTTP request object
   * @return true or false depending on header and method content
   */
  private boolean isPreflightRequest(HttpServletRequest request) {
    return 
        request.getMethod().contentEquals("OPTIONS") 
        && request.getHeader("Access-Control-Request-Method") != null;
  }
  
  /**
   * Sorts the list of methods in {@code YADA.server.CORS.allowed.methods} property and 
   * searches it for the requested method
   * @param request the HTTP request object
   * @return true or false depending on header and method content
   */
  private boolean isMethodAllowed(HttpServletRequest request) {
    String requestedMethod = request.getHeader("Access-Control-Request-Method");    
    String[] allowedMethods = props.getProperty(CORS_ALLOW_METHODS).split(",");
    Arrays.sort(allowedMethods);
    return Arrays.binarySearch(allowedMethods,requestedMethod) > -1;
  }
  
  /**
   * Sorts the list of methods in {@code YADA.server.CORS.allowed.methods} property and 
   * searches it for the requested method
   * @param request the HTTP request object
   * @return true or false depending on header and method content
   */
  private boolean isHeadersAllowed(HttpServletRequest request) {
    boolean allow = false;
    String requestedHeaders = request.getHeader("Access-Control-Request-Headers");
    if(requestedHeaders == null 
        || requestedHeaders.length() == 0
        || requestedHeaders.contentEquals(CORS_WILDCARD)
        || requestedHeaders.contentEquals("."+CORS_WILDCARD))
    {
      allow = true;
    }
    else
    {
      for(String rh : requestedHeaders.split(","))
      {
        for(String ah : props.getProperty(CORS_ALLOW_HEADERS).split(","))
        {
          if(ah.trim().equalsIgnoreCase(rh))
          {
            allow = true;
            break;
          }        
        }
        if(allow)
          break;
      }
    }
    return allow;
  }
  
  /**
   * Convenience method to return {@code 403 Forbidden} when CORS request
   * fails for any reason.
   * @param baseRequest the jetty request object
   * @throws ServletException the exception to throw
   * @throws IOException when the response can't be modified for some reason
   */
  private void fail(Request baseRequest) throws ServletException, IOException {
    baseRequest.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
    throw new ServletException();
  }
      

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    
    String origin = request.getHeader("Origin");
    if(origin != null)
    {
      handleOrigins(baseRequest,request,response);
      if(isSimpleRequest(request))
      {
        handleCredentials(baseRequest, request, response);
        handleExposeHeaders(baseRequest, request, response);
      }
      else if(isPreflightRequest(request))
      {
        if(!(isMethodAllowed(request) && isHeadersAllowed(request)))
        {
          fail(baseRequest);
        }
        else
        {
          handleAllowMethods(baseRequest,request,response);
          handleAllowHeaders(baseRequest,request,response);
          handleCredentials(baseRequest,request,response);
          handleMaxAge(baseRequest,request,response);
          handleExposeHeaders(baseRequest, request, response);
          if(!Boolean.parseBoolean(props.getProperty(CORS_CHAIN_PREFLIGHT)))
          {
            baseRequest.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
            baseRequest.setHandled(true);
          }
        }                 
      }
      else
      {
        fail(baseRequest);
      }
    }
    return;
  }

}
