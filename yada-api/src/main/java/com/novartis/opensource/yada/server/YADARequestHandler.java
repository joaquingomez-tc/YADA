package com.novartis.opensource.yada.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class YADARequestHandler extends AbstractHandler {

  public YADARequestHandler() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    // Mark the request as handled so that it
    // will not be processed by other handlers.
    baseRequest.setHandled(true);

  }

}
