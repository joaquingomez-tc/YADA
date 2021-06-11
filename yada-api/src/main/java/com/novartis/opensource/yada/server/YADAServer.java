/**
 * 
 */
package com.novartis.opensource.yada.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.rewrite.handler.CompactPathRule;
import org.eclipse.jetty.rewrite.handler.RedirectPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.*;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.novartis.opensource.yada.Finder;


/**
 * @author dvaron
 * @since 10.0.0
 */
public class YADAServer {
  
  /**
   * Local logger handle
   */
  static Logger              l           = Logger.getLogger(YADAServer.class);
  
  /**
   * Container of configuration data which shouldn't be hardcoded. 
   */
  private static Properties YADA_PROPERTIES = loadYADAProperties();
  
  /**
   * Constant equal to {@value}. Default location for {@code YADA.properties}
   * file, in {@code WEB-INF/classes}
   *
   * (moved from {@link Finder})
   */
  public final static String YADA_DEFAULT_PROPERTIES_PATH = "/YADA.properties";
  
  /**
   * Constant equal to {@value}. Used for retrieving config for specific YADA
   * index.
   *
   * (moved from {@link Finder})
   */
  private final static String YADA_PROPERTIES_PATH = "YADA.properties.path";
  
  /**
   * Constant equal to {@value}. Used for setting non-ssl port
   */
  private final static String YADA_SERVER_HTTP_PORT =  "YADA.server.http.port";

  /**
   * Constant equal to {@value}. Used for setting ssl port
   */
  private final static String YADA_SERVER_HTTPS_PORT =  "YADA.server.https.port";
  
  /**
   * Constant equal to {@value}. Used for setting server context path
   */
  private final static String YADA_SERVER_CONTEXT =  "YADA.server.context";

  /**
   * 
   */
  public YADAServer() {
    
  }

  /**
   * @param args command line args
   * @throws Exception catch all for any startup exceptions
   */
  public static void main(String[] args) throws Exception {
    
    System.out.println("Starting YADA Server...");
    
    // Create and configure a ThreadPool.
    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setName("yada");

    // Create a Server instance.
    org.eclipse.jetty.server.Server server = new Server(threadPool);
    
    // The HTTP configuration object.
    HttpConfiguration httpConfig = new HttpConfiguration();

    // The ConnectionFactory for HTTP/1.1.
    HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

    // The ConnectionFactory for clear-text HTTP/2.
    HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);


    // Create a ServerConnector to accept connections from clients.
    Connector connector = new ServerConnector(server, http11, h2c);
    ((AbstractNetworkConnector) connector).setPort((Integer.valueOf((String)getProperties().get(YADA_SERVER_HTTP_PORT))));


    // Add the Connector to the YADAServer
    server.addConnector(connector);
    
    // Handlers
    HandlerList handlerList = new HandlerList();
    
    // Set the Context
    ContextHandler contextHandler = new ContextHandler();
    String ctx = getProperties().getProperty(YADA_SERVER_CONTEXT);
    contextHandler.setContextPath(ctx.startsWith("/") ? ctx : "/"+ctx);    
    String ctxPath = contextHandler.getContextPath();
    
    
    // Rewrites (for path-style)
    RewriteHandler rewriteHandler = new RewriteHandler();
    
    String rxPath = "^(?:"+ctxPath+")?(.*[\\/{]q(?:name)?[:\\/].+)$";
    String rxYp   = ctxPath+"?yp=%s";        
    
    RewriteRegexRule pathRule = new RewriteRegexRule(rxPath, String.format(rxYp,"$1"));    
    RewriteRegexRule jspRule = new RewriteRegexRule("/yada.jsp",ctxPath+"?$Q");
    
    rewriteHandler.addRule(pathRule);
    rewriteHandler.addRule(jspRule);
    
    // Set the Handler
    YADARequestHandler yadaRequsetHandler = new YADARequestHandler();
    
    // Set handlers
    contextHandler.setHandler(rewriteHandler);
    handlerList.addHandler(contextHandler);
    handlerList.addHandler(yadaRequsetHandler);
    
    // attach the handler to the server
    server.setHandler(handlerList);

    // Start the YADAServer so it starts accepting connections from clients.
    server.start();

  }
  
  /**
   * For loading properties
   * @param props the props to set
   */
  public void setProps(Properties props)
  {
    YADAServer.YADA_PROPERTIES = props;
  }
  
  
  
  /**
   * @return the props
   */
  public static Properties getProperties()
  {
    return YADA_PROPERTIES;
  }

  /**
   * Sets {@code static} {@link #YADA_PROPERTIES} object.  Moved from {@link Finder}
   *
   * @return {@link java.util.Properties}
   * 
   */
  private final static Properties loadYADAProperties() {
    Properties props = new Properties();
    String     path  = System.getProperty(YADA_PROPERTIES_PATH);
    if (path == null || "".equals(path))
      path = YADAServer.YADA_DEFAULT_PROPERTIES_PATH;
    InputStream is = Finder.class.getResourceAsStream(path);
    try
    {
      props.load(is);
      l.info(String.format("Loaded %s", path));
    }
    catch (IOException e)
    {
      String msg = String.format("Cannot find or load YADA properties file %s", path);
      l.fatal(msg);
      System.exit(1);
    }
    return props;
  }
}
