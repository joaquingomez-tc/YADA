/**
 * 
 */
package com.novartis.opensource.yada.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.novartis.opensource.yada.Finder;
import com.novartis.opensource.yada.YADARequest;
import com.novartis.opensource.yada.security.IdentityCache;


/**
 * @author dvaron
 * @since 10.0.0
 */
public class YADAServer {
  
  /**
   * Local logger handle
   */
  static Logger              l           = LoggerFactory.getLogger(YADAServer.class);
  
  /**
   * Container of configuration data which shouldn't be hardcoded. 
   */
  private static Properties YADA_PROPERTIES = loadYADAProperties();
  
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
  private final static IdentityCache YADA_IDENTITY_CACHE = new IdentityCache();

  /* Constant equal to {@value}. Used for request log configuration
   * @since 10.1.3
   */
  public final static String YADA_SERVER_REQUEST_LOG_FILE = "YADA.server.request.log.file";
  
  /**
   * Constant equal to {@value}. Used for request log configuration
   * @since 10.1.3
   */
  public final static String YADA_SERVER_REQUEST_LOG_FORMAT = "YADA.server.request.log.format";

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
    String allParams       = "(?:" + paramShortNames + "|" + paramLongNames + ")";
    String params          = allParams.replaceAll("\\|q(?:name)?[|)]", "|");
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
    
    // Set the YADARequest and Cors Handlers
    YADARequestHandler yadaRequestHandler = new YADARequestHandler();
    YADACorsHandler    yadaCorsHandler    = new YADACorsHandler();
           
    // Set handlers hierarchy
    
    HandlerList yadaHandlerList = new HandlerList();
    yadaHandlerList.addHandler(yadaCorsHandler);
    yadaHandlerList.addHandler(yadaRequestHandler);
    rewriteHandler.setHandler(yadaHandlerList);    
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
     *   |       |                                   +-- HandlerList (yadaHandlerList)
     *   |       |                                           |
     *   |       |                                           +-- YADACorsHandler
     *   |       |                                           |
     *   |       |                                           +-- YADARequestHandler
     *   |       +-- DefaultHandler
     *   |       
     *   |
     *   +-- YADAErrorHandler
     */
    
    // attach the handler to the server
    server.setHandler(handlerList);
    String reqLogFile = getProperties().getProperty(YADA_SERVER_REQUEST_LOG_FILE);
    String reqLogFmt  = getProperties().getProperty(YADA_SERVER_REQUEST_LOG_FORMAT);
    server.setRequestLog(new CustomRequestLog(reqLogFile, reqLogFmt));
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
    try(InputStream is = new FileInputStream(new File(path)))
    {
      props.load(is);
      l.info(String.format("Loaded %s", path));
    }
    catch (IOException e)
    {
      String msg = String.format("Cannot find or load YADA properties file %s", path);
      l.error(msg);
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

  /**
   * @return
   */
  public static IdentityCache getIdentityCache() {
    return YADA_IDENTITY_CACHE;
  }
}
