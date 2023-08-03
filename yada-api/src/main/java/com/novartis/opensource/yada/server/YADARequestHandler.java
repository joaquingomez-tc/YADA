package com.novartis.opensource.yada.server;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import com.novartis.opensource.yada.Service;
import com.novartis.opensource.yada.YADARequest;
import com.novartis.opensource.yada.util.YADAUtils;

/**
 * @author dvaron
 * @since 10.0.0
 */
public class YADARequestHandler extends AbstractHandler {

  /**
   * Container for response
   */
  private String  result   = "";
  
  /**
   * Constant equal to {@value}
   */
  private final static String YADA_PATH = "yp";
  
  /**
   * Null constructor
   */
  public YADARequestHandler() {

  }

  @SuppressWarnings("deprecation")
@Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    
    /*
     * If default context is configured then any path would be accepted.
     * It seems the only way around this is to check, here, for the syntax of 
     * that path after rewrite, and throw an error if necessary.  
     * 
     * If the conditions below don't trap errant requests, resulting in a 404
     * short circuit, in path-style cases, a 400 Bad Request error will eventually 
     * be returned due to parameter checking in the handleRequest method
     *  
     */
    
    String context = (String)YADAServer.getProperties().get(YADAServer.YADA_SERVER_CONTEXT); 
    if((context.contentEquals("") || context.contentEquals("/")) 
        && target.matches("^\\/.+$")
        && !target.matches("^\\/yada[^\\/]$"))
    {
      String paramShortNames = String.join("|", YADARequest.fieldAliasMap.keySet());
      String paramLongNames  = String.join("|", new HashSet<String>(YADARequest.fieldAliasMap.values()));
      String allParams       = "(?:" + paramShortNames + "|" + paramLongNames + ")";
      
          // first case, querystring syntax:  /foo?
      if((request.getParameter(YADA_PATH) == null 
          && request.getParameterMap().size() > 0
          && request.getQueryString() != null)
          // 2nd case, path syntax, _should_ always result in a 400 error because the
          // parameters won't parse correctly.  However, we'll short circuit that here by
          // checking for a param in the first position.  Then it will return a 404 instead.            
         || !target.matches("\\/"+allParams+"\\/"))        
      {
        baseRequest.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
        throw new ServletException();
      }       
    }
    
    Service service = new Service();
    
    try
    {
      if(request.getParameter(YADA_PATH) != null)
      {
        service.handleRequest(request, request.getParameter(YADA_PATH));
      }
      else
      {
        service.handleRequest(request);
      }
      response.addHeader("X-YADA-VERSION",YADAUtils.getVersion());
      if(request.getParameter("method") == null
          || !request.getParameter("method").equals("upload"))
      {          
        result = service.execute();       
        String fmt    = service.getYADARequest().getFormat();
        
        if (service.getYADARequest().getExport())
        {
          response.setStatus(HttpServletResponse.SC_CREATED);
          response.addHeader("Location", result);
          response.setContentType("text/plain");
          fmt = YADARequest.FORMAT_PLAINTEXT;
        }
        else if(service.getYADARequest().getMethod().contentEquals(YADARequest.METHOD_UPDATE))
        {
          response.setContentType("text/plain");
          fmt = YADARequest.FORMAT_PLAINTEXT;
        }
        
        if (YADARequest.FORMAT_JSON.equals(fmt))
        {
          response.setContentType("application/json;charset=UTF-8");
          // add timestamp to the JSON object, unless ... this is not a JSON object but a JSON array.
          if(!result.startsWith("[")) {
            JSONObject jo = new JSONObject(result);
            // add 1 to account for remaining steps (tested this--it's very consistent)
            long elapsed = (new Date().getTime() - baseRequest.getTimeStamp()) + 1;
            jo.put("elapsed", elapsed);
            result = jo.toString();
          }
        }
        else if (YADARequest.FORMAT_XML.equals(fmt))
        {
          response.setContentType("text/xml");
        }
        else if (YADARequest.FORMAT_CSV.equals(fmt))
        {
          response.setContentType("text/csv");
        }
        else if (YADARequest.FORMAT_TSV.equals(fmt) || YADARequest.FORMAT_TAB.equals(fmt))
        {
          response.setContentType("text/tab-separated-values");
        }
        else if (YADARequest.FORMAT_PIPE.equals(fmt))
        {
          response.setContentType("text/pipe-separated-values");
        }
        else if (YADARequest.FORMAT_HTML.equals(fmt))
        {
          response.setContentType("text/html");
        }
        else if (YADARequest.FORMAT_BINARY.equals(fmt))
        {
          String  ct = "application/octet-stream";
          Pattern rx = Pattern.compile("^data:(.+/.+);base64, .+$",Pattern.DOTALL);
          Matcher m  = rx.matcher(result);
          if(m.matches())
          {
            ct = m.group(1);
          }
          response.setContentType(ct);
        }
      }
      // result      
      
      response.getWriter().print(result);
      baseRequest.setHandled(true);
    }
    catch(Exception e)
    {
      String exceptionClass = e.getClass().getName();
      Integer errorCode = YADAErrorHandler.statusCodes.get(YADAErrorHandler.UNHANDLED_EXCEPTION);
      if(YADAErrorHandler.statusCodes.containsKey(exceptionClass))
      {
        errorCode = YADAErrorHandler.statusCodes.get(exceptionClass);
      }
      baseRequest.setAttribute(Dispatcher.ERROR_EXCEPTION, e);
      baseRequest.getResponse().sendError(errorCode);
    }
    finally
    {
      baseRequest.setHandled(true);
    }
  }
}
