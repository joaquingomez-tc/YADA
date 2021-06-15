package com.novartis.opensource.yada.server;

import java.io.IOException;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONObject;

import com.novartis.opensource.yada.Service;
import com.novartis.opensource.yada.YADARequest;
import com.novartis.opensource.yada.YADARequestException;

/**
 * @author dvaron
 * @since 10.0.0
 */
public class YADARequestHandler extends AbstractHandler {

  
  final String EXCEPTION = "(?s)^.*\\{\\n\\s+.+\"Exception\":.*$";
  final String PACKAGE   = "com.novartis.opensource.yada.";
  final String BASE_EXCEPTION                = PACKAGE + "YADAException";
  final String EXECUTION_EXCEPTION           = PACKAGE + "YADAExecutionException";
  final String PARSER_EXCEPTION              = PACKAGE + "YADAParserException";
  final String CONNECTION_EXCEPTION          = PACKAGE + "YADAConnectionException";
  final String FINDER_EXCEPTION              = PACKAGE + "YADAFinderException";
  final String QUERY_CONFIGURATION_EXCEPTION = PACKAGE + "YADAQueryConfigurationException";
  final String REQUEST_EXCEPTION             = PACKAGE + "YADARequestException";
  final String UNSUPPORTED_ADAPTOR_EXCEPTION = PACKAGE + "YADAUnsupportedAdaptorException";
  final String ADAPTOR_EXCEPTION             = PACKAGE + "adaptor.YADAAdaptorException";
  final String ADAPTOR_EXECUTION_EXCEPTION   = PACKAGE + "adaptor.YADAAdaptorExecutionException";
  final String CONVERTER_EXCEPTION           = PACKAGE + "format.YADAConverterException";
  final String RESPONSE_EXCEPTION            = PACKAGE + "format.YADAResponseException";
  final String IO_EXCEPTION                  = PACKAGE + "io.YADAIOException";
  final String PLUGIN_EXCEPTION              = PACKAGE + "plugin.YADAPluginException";
  final String SECURITY_EXCEPTION            = PACKAGE + "YADASecurityException";
  final String UNHANDLED_EXCEPTION           = "java.lang\\.*";

  final String HTTP_SC_NOT_FOUND       = "Not Found";
  final String HTTP_SC_BAD_REQUEST     = "Bad Request";
  final String HTTP_SC_NOT_IMPLEMENTED = "Not Implemented";
  final String HTTP_SC_FORBIDDEN       = "Forbidden";
  final String HTTP_SC_INTERNAL_SERVER_ERROR = "Internal Server Error";

  final Hashtable<Integer,String> statusText = new Hashtable<>();
  

  final Hashtable<String,Integer> statusCodes = new Hashtable<>();
  



  String  result   = "";
  /**
   * Null constructor
   */
  public YADARequestHandler() {
    statusText.put(HttpServletResponse.SC_NOT_FOUND,HTTP_SC_NOT_FOUND);
    statusText.put(HttpServletResponse.SC_BAD_REQUEST,HTTP_SC_BAD_REQUEST);
    statusText.put(HttpServletResponse.SC_NOT_IMPLEMENTED,HTTP_SC_NOT_IMPLEMENTED);
    statusText.put(HttpServletResponse.SC_FORBIDDEN,HTTP_SC_FORBIDDEN);
    statusText.put(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,HTTP_SC_INTERNAL_SERVER_ERROR);
    // FinderExcepion 404
    statusCodes.put(FINDER_EXCEPTION, HttpServletResponse.SC_NOT_FOUND);
    // QueryConfigurationException, RequestException 403
    statusCodes.put(QUERY_CONFIGURATION_EXCEPTION, HttpServletResponse.SC_BAD_REQUEST);
    statusCodes.put(REQUEST_EXCEPTION, HttpServletResponse.SC_BAD_REQUEST);
    // UnsupportedAdaptorException 501
    statusCodes.put(UNSUPPORTED_ADAPTOR_EXCEPTION, HttpServletResponse.SC_NOT_IMPLEMENTED);
    // SecurityException 403
    statusCodes.put(SECURITY_EXCEPTION, HttpServletResponse.SC_FORBIDDEN);
    // All others 500
    statusCodes.put(BASE_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(EXECUTION_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(PARSER_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(CONNECTION_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(ADAPTOR_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(ADAPTOR_EXECUTION_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(CONVERTER_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(RESPONSE_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(IO_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(PLUGIN_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    statusCodes.put(UNHANDLED_EXCEPTION, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    // Mark the request as handled so that it
    // will not be processed by other handlers.
      Service service = new Service();
      if(request.getParameter("yp") != null)
      {
        service.handleRequest(request, request.getParameter("yp"));
      }
      else
      {
        try
        {
          service.handleRequest(request);
        }
        catch (YADARequestException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      response.addHeader("X-YADA-VERSION","${project.version}");
      if(request.getParameter("method") == null
          || !request.getParameter("method").equals("upload"))
      {
        result = service.execute();
        String fmt    = service.getYADARequest().getFormat();
        // TODO confirm response content type defaults to json even though the call below follows
        // the call to execute
        boolean exception = result.matches(EXCEPTION);
        
        if (service.getYADARequest().getExport())
        {
          response.setStatus(HttpServletResponse.SC_CREATED);
          response.addHeader("Location", result);
          response.setContentType("text/plain");
          fmt = YADARequest.FORMAT_PLAINTEXT;
        }
        
        if (YADARequest.FORMAT_JSON.equals(fmt) || exception)
        {
          response.setContentType("application/json;charset=UTF-8");
          if(exception)
          {
            JSONObject e = new JSONObject(result);
            String exceptionClass = e.getString("Exception");
            Integer errorCode = statusCodes.get(UNHANDLED_EXCEPTION);
            if(statusCodes.containsKey(exceptionClass))
            {
              errorCode = statusCodes.get(exceptionClass);
            }
            int ec = errorCode.intValue();
            e.put("Status",ec);
            e.put("StatusText",statusText.get(errorCode));
  //          request.getSession().setAttribute("YADAException",e.toString());
            response.sendError(errorCode);
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
}
