/**
 * 
 */
package com.novartis.opensource.yada.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;

import org.eclipse.jetty.server.handler.ErrorHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import com.novartis.opensource.yada.YADARequest;
import com.novartis.opensource.yada.util.YADAUtils;

/**
 * @author dvaron
 * @since 10.0.0
 */
public class YADAErrorHandler extends ErrorHandler {

  
  /**
   * Constant equal to regular expression {@code ^at com\\.novartis\\.opensource\\.yada\\.([a-zA-Z.]+)\\.[a-zA-Z]+\\([a-zA-Z]+\\.java:(\\d+)\\)$}
   */
  static Pattern RX_LINKS = Pattern.compile("^at com\\.novartis\\.opensource\\.yada\\.([a-zA-Z.]+)\\.[a-zA-Z]+\\([a-zA-Z]+\\.java:(\\d+)\\)$");
  /**
   * Constant equal to {@code ^org\\.eclipse\\.jetty.+}
   */
  final static Pattern RX_JETTY = Pattern.compile("^org\\.eclipse\\.jetty.+");
  /**
   * Constant equal to repository property
   */
  final static String YADA_REPOSITORY = YADAServer.getProperties().getProperty(YADAServer.YADA_REPOSITORY);
  /**
   * Constant equal to {@value}
   */
  final static String EXCEPTION = "(?s)^.*\\{\\n\\s+.+\"Exception\":.*$";
  /**
   * Constant equal to {@value}
   */
  final static String PACKAGE   = "com.novartis.opensource.yada.";  
  /**
   * Constant equal to {@value}
   */
  final static String BASE_EXCEPTION                = PACKAGE + "YADAException";
  /**
   * Constant equal to {@value}
   */
  final static String EXECUTION_EXCEPTION           = PACKAGE + "YADAExecutionException";
  /**
   * Constant equal to {@value}
   */
  final static String PARSER_EXCEPTION              = PACKAGE + "YADAParserException";
  /**
   * Constant equal to {@value}
   */
  final static String CONNECTION_EXCEPTION          = PACKAGE + "YADAConnectionException";
  /**
   * Constant equal to {@value}
   */
  final static String FINDER_EXCEPTION              = PACKAGE + "YADAFinderException";
  /**
   * Constant equal to {@value}
   */
  final static String QUERY_CONFIGURATION_EXCEPTION = PACKAGE + "YADAQueryConfigurationException";
  /**
   * Constant equal to {@value}
   */
  final static String REQUEST_EXCEPTION             = PACKAGE + "YADARequestException";
  /**
   * Constant equal to {@value}
   */
  final static String UNSUPPORTED_ADAPTOR_EXCEPTION = PACKAGE + "YADAUnsupportedAdaptorException";
  /**
   * Constant equal to {@value}
   */
  final static String ADAPTOR_EXCEPTION             = PACKAGE + "adaptor.YADAAdaptorException";
  /**
   * Constant equal to {@value}
   */
  final static String ADAPTOR_EXECUTION_EXCEPTION   = PACKAGE + "adaptor.YADAAdaptorExecutionException";
  /**
   * Constant equal to {@value}
   */
  final static String CONVERTER_EXCEPTION           = PACKAGE + "format.YADAConverterException";
  /**
   * Constant equal to {@value}
   */
  final static String RESPONSE_EXCEPTION            = PACKAGE + "format.YADAResponseException";
  /**
   * Constant equal to {@value}
   */
  final static String IO_EXCEPTION                  = PACKAGE + "io.YADAIOException";
  /**
   * Constant equal to {@value}
   */
  final static String PLUGIN_EXCEPTION              = PACKAGE + "plugin.YADAPluginException";
  /**
   * Constant equal to {@value}
   */
  final static String SECURITY_EXCEPTION            = PACKAGE + "YADASecurityException";
  /**
   * Constant equal to {@value}
   */
  final static String UNHANDLED_EXCEPTION           = "java.lang\\.*";
  /**
   * Constant equal to {@value}
   */
  final static String HTTP_SC_NOT_FOUND             = "Not Found";
  /**
   * Constant equal to {@value}
   */
  final static String HTTP_SC_BAD_REQUEST           = "Bad Request";
  /**
   * Constant equal to {@value}
   */
  final static String HTTP_SC_NOT_IMPLEMENTED       = "Not Implemented";
  /**
   * Constant equal to {@value}
   */
  final static String HTTP_SC_FORBIDDEN             = "Forbidden";
  /**
   * Constant equal to {@value}
   */
  final static String HTTP_SC_INTERNAL_SERVER_ERROR = "Internal Server Error";
  /**
   * Hashtable to store status text
   */
  final static Hashtable<Integer,String> statusText = new Hashtable<>();  
  /**
   * Hashtable to store status codes
   */
  final static Hashtable<String,Integer> statusCodes = new Hashtable<>();
  /**
   * Constant equal to {@value}
   */
  final static String KEY_HELP = "Help";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_SOURCE = "Source";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_VERSION = "Version";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_EXCEPTION = "Exception";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_MESSAGE = "Message";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_PARAMS = "Params";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_STACKTRACE = "Stacktrace";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_LINKS = "Links";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_STATUS = "Status";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_ELAPSED = "elapsed";
  /**
   * Constant equal to {@value}
   */
  final static String KEY_URI = "URI";
  /**
   * Constant equal to {@code #other} page in repo
   */
  final static String VAL_HELP = YADA_REPOSITORY+"#other";
  /**
   * Constant equal to YADA_REPOSITORY value
   */
  final static String VAL_SOURCE = YADA_REPOSITORY;
  /**
   * Constant equal to current version
   */
  final static String VAL_VERSION = YADAUtils.getVersion();
  /**
   * Constant equal to path to {@code /blob/master/yada-api/src/main/java/com/novartis/opensource/yada/} in repository
   */
  final static String VAL_REPO_URI = YADA_REPOSITORY+"/blob/master/yada-api/src/main/java/com/novartis/opensource/yada/";

  /**
   * Variable for time reporting
   */
  private long started = 0;
  
  static {
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
  /**
   * 
   */
  public YADAErrorHandler() {

  }
  
  @Override
  protected void generateAcceptableResponse(Request baseRequest, HttpServletRequest request, HttpServletResponse response, int code, String message, String mimeType)
          throws IOException
  {      
    baseRequest.setHandled(true);
    started = baseRequest.getTimeStamp();
    Writer writer = getAcceptableWriter(baseRequest, request, response);
    if (null != writer) {
        response.setContentType(MimeTypes.Type.APPLICATION_JSON.asString());
        response.setStatus(code);
        handleErrorPage(request, writer, code, message);
    }
  }
  
  @Override
  protected Writer getAcceptableWriter(Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException
  {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    return response.getWriter();
  }
  
  @Override
  protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
          throws IOException
  {      
    try 
    {
      // request
      String uri = request.getRequestURI();
      // exception 
      Throwable excp = (Throwable)request.getAttribute(Dispatcher.ERROR_EXCEPTION);
      // container for wrapped exception result
      JSONObject error = new JSONObject();
      // for stacktrace and links
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      excp.printStackTrace(pw);
      String[] strace = sw.toString().replace("\t","").split("\n");      
      JSONArray st = new JSONArray();
      JSONArray links = new JSONArray();
      for(int i=0;i<strace.length;i++)
      {
        String trace = strace[i];
        Matcher m = RX_LINKS.matcher(trace);
        if(m.matches())
        {
          links.put(links.length(),VAL_REPO_URI+m.group(1).replace('.','/')+".java#L"+m.group(2));
        }
        st.put(i,trace);
      }
      // params processing
      JSONObject params = new JSONObject();
      if(request.getParameter("yp") != null
          || request.getParameterMap().size() == 0)
      {
        String[] pathElements;
        if(request.getParameter("yp") != null)
        {
          pathElements = request.getParameter("yp").split("/");
        }
        else
        {
          String[] orig = uri.split("/");
          int start = ((String)YADAServer
              .getProperties()
              .get(YADAServer.YADA_SERVER_CONTEXT))
              .contentEquals("/") 
              ? 1 : 2;
          pathElements = Arrays.copyOfRange(orig,start,orig.length);
        }
        for(int i=1;i<pathElements.length;i++)
        {
          if(pathElements[i-1].contentEquals(YADARequest.PS_QNAME)
            ||pathElements[i-1].contentEquals(YADARequest.PL_QNAME))
          {
            params.put(pathElements[i-1], pathElements[i]+"/"+pathElements[++i]);
          }
          else
          {
            params.put(pathElements[i-1], pathElements[i]);
          }          
        }
      }
      else
      {
        Map<String,String[]> pmap = request.getParameterMap();
        for(String key : pmap.keySet())
        {
          String[] valArr = pmap.get(key);
          if(valArr.length == 1)
            params.put(key, pmap.get(key)[0]);
          else
          {
            params.put(key, String.join(",",Arrays.asList(pmap.get(key))));
          }
        }
      }
      
      long elapsed = (new Date().getTime() - started) + 1;            
      
      error.put(KEY_URI, uri);
      error.put(KEY_HELP, VAL_HELP);
      error.put(KEY_SOURCE, VAL_SOURCE);
      error.put(KEY_VERSION, VAL_VERSION);
      error.put(KEY_STATUS, code);
      error.put(KEY_MESSAGE, message);
      error.put(KEY_EXCEPTION, excp);
      error.put(KEY_PARAMS, params);
      error.put(KEY_STACKTRACE, st);
      error.put(KEY_LINKS, links);
      error.put(KEY_ELAPSED, elapsed);
      writer.write(error.toString());
    }
    catch (Exception e) 
    {
        // Log if needed
    }      
  }

}
