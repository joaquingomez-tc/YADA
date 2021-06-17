/**
 * 
 */
package com.novartis.opensource.yada.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewriteRegexRule;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.novartis.opensource.yada.Finder;
import com.novartis.opensource.yada.YADARequest;


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
  public final static String YADA_SERVER_CONTEXT =  "YADA.server.context";
  
  /**
   * Constant equal to {@value}. Used for setting keystore path
   */
  public final static String YADA_SERVER_KEYSTORE_PATH = "YADA.server.keystore.path";
  
  /**
   * Constant equal to {@value}. Used for setting keystore path
   */
  public final static String YADA_SERVER_KEYSTORE_SECRET = "YADA.server.keystore.secret";
  
  /**
   * Constant equal to {@value}. Used for setting keystore path
   */
  public final static String YADA_REPOSITORY = "YADA.repository";
  

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
    
    // Secure Handler (if needed)
    SecuredRedirectHandler securedHandler = new SecuredRedirectHandler();
    
    // Create and configure a ThreadPool.
    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setDetailedDump(true);
    threadPool.setName("yada");

    // Create a Server instance.
    org.eclipse.jetty.server.Server server = new Server(threadPool);
    
    // The HTTP configuration object.
    int connectorPort = Integer.valueOf((String)getProperties().get(YADA_SERVER_HTTP_PORT));    
    HttpConfiguration httpConfig = new HttpConfiguration();
    
    // The ConnectionFactory for HTTP/1.1.
    HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

    // The ConnectionFactory for clear-text HTTP/2.
    HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);


    // Create a ServerConnector to accept connections from clients.
    Connector connector = new ServerConnector(server, http11, h2c);    
    ((AbstractNetworkConnector) connector).setPort(connectorPort);
    // Add the Connector to the YADAServer
    server.addConnector(connector);    

    if(isSecured())
    {
      // Configure the SslContextFactory with the keyStore information.
      int securePort = Integer.valueOf((String)getProperties().get(YADA_SERVER_HTTPS_PORT));   
      httpConfig.setSecurePort(securePort);
      httpConfig.addCustomizer(new SecureRequestCustomizer());    
      SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
      String keystorePath = getProperties().getProperty(YADA_SERVER_KEYSTORE_PATH);
      String keystoreSecret = getProperties().getProperty(YADA_SERVER_KEYSTORE_SECRET);
      sslContextFactory.setKeyStorePath(keystorePath);
      sslContextFactory.setKeyStorePassword(keystoreSecret);
      // The ConnectionFactory for TLS.
      SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, http11.getProtocol());
      ServerConnector secureConnector = new ServerConnector(server, tls, http11);
      secureConnector.setPort(securePort);
      server.addConnector(secureConnector);
    }
    

    // Handlers    
    HandlerList handlerList = new HandlerList();
    ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
    
    // Set the Context
    ContextHandler yadaPropContextHandler = new ContextHandler();
    yadaPropContextHandler.setAllowNullPathInfo(true);
    String ctx = getProperties().getProperty(YADA_SERVER_CONTEXT);
    // context will be "/" if unset, or "/whatever" if set.
    yadaPropContextHandler.setContextPath(ctx.startsWith("/") ? ctx : "/"+ctx);  
    String ctxPath = yadaPropContextHandler.getContextPath();
    
        
    // Rewrites for path-style, converts /param/value/param/value 
    // into ?yp=param/value/param/value
    String paramShortNames = String.join("|", YADARequest.fieldAliasMap.keySet());
    String paramLongNames  = String.join("|", new HashSet<String>(YADARequest.fieldAliasMap.values()));
    String dirtyParams     = "(?:" + paramShortNames + "|" + paramLongNames + ")";
    String params          = dirtyParams.replaceAll("\\|q(?:name)?[|)]", "|");
    String pathRx  = "^(?:\\/)?((?:" + params + "\\/.+)*?[\\/{]?q(?:name)?[:\\/].+)$";
    String pathFmt = ctxPath+"?yp=%s";
    // This rule will change path-syntax into "/context?yp=uri" which is then handled by the Service class
    RewriteRegexRule pathRule = new RewriteRegexRule(pathRx, String.format(pathFmt,"$1"));  
    pathRule.setTerminating(true);
        
    // This rule will change requests for "yada.jsp?querystring" into "/context?querystring", 
    // i.e., strip off the jsp extension    
    String jspRx   = "^\\/yada\\.jsp";
    String jspFmt  = ctxPath+"?$Q";
    RewriteRegexRule jspRule = new RewriteRegexRule(jspRx, jspFmt);            
    
    // Add the rules to the handler
    RewriteHandler rewriteHandler = new RewriteHandler();
    rewriteHandler.addRule(pathRule);
    rewriteHandler.addRule(jspRule);
    
    // Set the YADA Handler
    YADARequestHandler yadaRequestHandler = new YADARequestHandler();
           
    // Set handlers hierarchy
    rewriteHandler.setHandler(yadaRequestHandler);
    yadaPropContextHandler.setHandler(rewriteHandler);    
    contextHandlerCollection.addHandler(yadaPropContextHandler);    

    if(isSecured())
    {
      securedHandler.setHandler(contextHandlerCollection);
      handlerList.addHandler(securedHandler);    
    }
    else
    {
      handlerList.addHandler(contextHandlerCollection);
    }
    handlerList.addHandler(new DefaultHandler());
    
    /* 
     * Handler Hierarchy:
     * 
     * Server
     *   |
     *   +-- HandlerList
     *   |       |
     *   |       +-- SecuredHandler
     *   |       |        |
     *   |       |        +-- ContextHandlerCollection
     *   |       |                 |
     *   |       |                 +-- ContextHandler (/context)
     *   |       |                          |        
     *   |       |                          +-- RewriteHandler (2 Rule)
     *   |       |                                   |
     *   |       |                                   +-- YADARequestHandler
     *   |       +-- DefaultHandler
     *   |
     *   +-- YADAErrorHandler
     */
    
    // attach the handler to the server
    server.setHandler(handlerList);
    ErrorHandler errorHandler = new YADAErrorHandler();
    server.setErrorHandler(errorHandler);
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
  
  /**
   * Returns {@code true} if the {@link #YADA_SERVER_HTTPS_PORT} property is set, implying
   * the other security-related props are set and valid
   * 
   * @return {@code true} if the {@link #YADA_SERVER_HTTPS_PORT} property is set
   */
  private static boolean isSecured() {
    return getProperties().get(YADA_SERVER_HTTPS_PORT) != null
        && getProperties().get(YADA_SERVER_KEYSTORE_PATH) != null
        && getProperties().get(YADA_SERVER_KEYSTORE_SECRET) != null;
  }
}
