/**
 * Copyright 2016 Novartis Institutes for BioMedical Research Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.novartis.opensource.yada.test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.novartis.opensource.yada.ConnectionFactory;
import com.novartis.opensource.yada.YADAConnectionException;
import com.novartis.opensource.yada.YADAQuery;

/**
 * Test class responsible for validating {@link ConnectionFactory} methods, and also for setup of JNDI context necessary for testing.
 * @author David Varon
 * @since 4.0.0
 */
public class ConnectionFactoryTest {

	/**
	 * Instance variable to hold the connection object
	 */
	private Connection connection;

	/**
	 * Container of properties, mainly passwords, which shouldn't be hardcoded. 
	 */
	private static Properties props = new Properties();
	
	/**
	 * In memory query store
	 * @since 10.1.1
	 */
	private Map<String,YADAQuery> YADAIndex = new ConcurrentHashMap<>();
	
	/**
	 * Test prep method which creates and populates a local JNDI context to facilitate testing independently of Tomcat.
	 * @param properties the path to the properties file, expected to be set in the TestNG xml config file.
	 */
	@Parameters({"properties"})
	@BeforeSuite(alwaysRun = true)
	public void init(String properties) {
		try 
		{
		  
		  
			// load properties
			setProps(properties);		      
		}    
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
  /**
   * Tests {@link ConnectionFactory#getConnection(String)} by attempting to connect to the YADA Index.
   * @throws YADAConnectionException when the connection can't be opened
   */
  @Test (groups = {"core"}, enabled = false)
  public void getConnection() throws YADAConnectionException {
    this.connection = ConnectionFactory.getConnectionFactory().getConnection(ConnectionFactory.YADA_APP);
  }
  
  /**
   * Tests exception handling in {@link ConnectionFactory#getConnection(String)} by attempting to connect to a non-existent JNDI string.
   * The test is successful if a {@link YADAConnectionException} is thrown.
   * @throws YADAConnectionException when the connection can't be opened
   */
  @Test (groups = {"core"}, expectedExceptions=YADAConnectionException.class, enabled = false)
  public void getUnknownConnectionFail() throws YADAConnectionException 
  {
  	ConnectionFactory.getConnectionFactory().getConnection("jdbc/yomama");
  }

  /**
   * Tests {@link ConnectionFactory#getSOAPConnection()} by opening a generic {@link javax.xml.soap.SOAPConnection}
   * The test is successful if no exception is thrown.
   * @throws YADAConnectionException when the connection can't be opened
   */
	@Test (groups = {"core"})
  public void getSOAPConnection() throws YADAConnectionException {
    //soapConnection = 
    ConnectionFactory.getConnectionFactory().getSOAPConnection();
  }
  
  /**
   * Tests YADA clean-up method {@link ConnectionFactory#releaseResources(Connection)}.
   * The test is successful if no exception is thrown.
   * @throws YADAConnectionException when the connection can't be opened
   */
  //@AfterSuite (groups = {"core", "json","standard","api","jsp","options","plugins"})
	@AfterSuite(alwaysRun = false)
  public void releaseResources() throws YADAConnectionException 
  {
  	ConnectionFactory.releaseResources(this.connection);
  }

	/**
	 * @return the props
	 */
	public static Properties getProps()
	{
		return props;
	}

	/**
	 * For loading properties
	 * @param props the props to set
	 */
	public void setProps(Properties props)
	{
		ConnectionFactoryTest.props = props;
	}
  
	/**
	 * For loading properties from disk
	 * @param properties the properties file
	 * @throws IOException when the file can't be loaded
	 */
	@Parameters({"properties"})
	@BeforeSuite(alwaysRun = true)
	public static void setProps(String properties) throws IOException 
	{		
		if(!"".equals(properties))
		{
			try(InputStream fis = ConnectionFactoryTest.class.getResourceAsStream(properties)) 
			{
			  props.load(fis);
			}
		}
	}
}
