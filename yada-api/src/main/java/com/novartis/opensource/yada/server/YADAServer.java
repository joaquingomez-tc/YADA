/**
 * 
 */
package com.novartis.opensource.yada.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * @author dvaron
 *
 */
public class YADAServer {

  /**
   * 
   */
  public YADAServer() {
    
  }

  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    
    System.out.println("Starting YADA Server...");
    
    // Create and configure a ThreadPool.
    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setName("server");

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
    ((AbstractNetworkConnector) connector).setPort(8080);


    // Add the Connector to the YADAServer
    server.addConnector(connector);

    // Set a simple Handler to handle requests/responses.
    server.setHandler(new AbstractHandler()
    {
        @Override
        public void handle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response)
        {
            // Mark the request as handled so that it
            // will not be processed by other handlers.
            jettyRequest.setHandled(true);
        }
    });

    // Start the YADAServer so it starts accepting connections from clients.
    server.start();

  }

}
