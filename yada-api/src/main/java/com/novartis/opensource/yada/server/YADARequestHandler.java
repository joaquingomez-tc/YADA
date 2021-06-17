package com.novartis.opensource.yada.server;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

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
   * Null constructor
   */
  public YADARequestHandler() {

  }

   
  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
      // Mark the request as handled so that it
      // will not be processed by other handlers.
      Service service = new Service();
      
      try
      {
        if(request.getParameter("yp") != null)
        {
          service.handleRequest(request, request.getParameter("yp"));
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
          
          if (YADARequest.FORMAT_JSON.equals(fmt))
          {
            response.setContentType("application/json;charset=UTF-8");
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
