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
package com.novartis.opensource.yada.format;

import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.novartis.opensource.yada.YADAQueryConfigurationException;
import com.novartis.opensource.yada.YADAQueryResult;
import com.novartis.opensource.yada.YADARequest;
import com.novartis.opensource.yada.YADARequestException;
import com.novartis.opensource.yada.util.YADAUtils;


/**
 * For returning results in XML format.
 * @author David Varon
 *
 */
public class XMLResponse extends AbstractResponse {

	/**
	 * Local logger handle
	 */
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(XMLResponse.class);
	/**
	 * Component of Java XML API
	 */
	private DocumentBuilderFactory docFactory;
	/**
	 * Component of Java XML API
	 */
	private DocumentBuilder        docBuilder;
	/**
	 * The result, ultimately, to be returned by this class's {@link #toString()} method
	 * @see #toString()
	 */
	private Document               doc;
	
	/**
	 * Default constructor, instantiates ivars.
	 * @throws YADAConverterException when xml api components can't be instantiated, i.e., parser can't be configured
	 */
	public XMLResponse() throws YADAConverterException {
		try 
		{
			this.docFactory = DocumentBuilderFactory.newInstance();
			this.docBuilder = this.docFactory.newDocumentBuilder();
			this.doc        = this.docBuilder.newDocument();
		} 
		catch (ParserConfigurationException e) 
		{
			throw new YADAConverterException("XML Document creation failed.");
		}
	}
	
	@Override
	public Response compose(YADAQueryResult[] yqrs) throws YADAResponseException, YADAConverterException, YADAQueryConfigurationException {
		setYADAQueryResults(yqrs);
		create();
		for(YADAQueryResult lYqr : yqrs)
		{
			setYADAQueryResult(lYqr);
			for(Object result : lYqr.getResults())
			{
				this.append(result);
			}
		}
		return this;
	}
	
	/**
	 * Creates root xml elements
	 * @see com.novartis.opensource.yada.format.AbstractResponse#create()
	 */
	@Override
	public Response create() throws YADAResponseException	
	{
		Element element = null;
		if(hasMultipleResults())
			element = this.doc.createElement(RESULTSETS);
		else
			element = this.doc.createElement(RESULTSET);
		try 
		{
      element.setAttribute(VERSION, YADAUtils.getVersion());
    } 
		catch (DOMException e) 
		{
		  String msg = "There was an issue appending the version attribute to the DOM.";
		  throw new YADAResponseException(msg,e);
		} 
		this.doc.appendChild(element);
		return this;
	}

	/**
	 * Objects {@link DocumentFragment} from {@link Converter} and appends it to 
	 * the response.
	 * @throws YADAQueryConfigurationException when the {@link Converter} spec in the request is malformed 
	 * @see com.novartis.opensource.yada.format.AbstractResponse#append(java.lang.Object)
	 */
	@Override
	public Response append(Object o) throws YADAResponseException, YADAConverterException, YADAQueryConfigurationException
	{
		//TODO handle harmonyMap.  
		try
		{
			Converter converter = getConverter(this.yqr);
			if(getHarmonyMap() != null)
				converter.setHarmonyMap(getHarmonyMap());
			boolean count = Boolean.parseBoolean(this.yqr.getYADAQueryParamValue(YADARequest.PS_COUNT));
			DocumentFragment rows  = (DocumentFragment)	converter.convert(o);
			Element resultSet = null;
			if(hasMultipleResults())
			{
				Element resultSets = (Element) this.doc.getFirstChild();
				resultSet  = this.doc.createElement(RESULTSET);
				resultSets.appendChild(resultSet);
			}
			else
			{
				resultSet = (Element) this.doc.getFirstChild();
			}
			resultSet.appendChild(this.doc.importNode(rows,true));			//TODO this could be an issue for harmonyMap.
			resultSet.setAttribute(RECORDS, String.valueOf(rows.getChildNodes().getLength()));
			resultSet.setAttribute(QNAME, this.yqr.getYADAQueryParamValue(YADARequest.PS_QNAME));
			if(count)
			{
				resultSet.setAttribute(TOTAL, String.valueOf(this.yqr.getCountResult(0)));
				resultSet.setAttribute(PAGE, this.yqr.getYADAQueryParamValue(YADARequest.PS_PAGESTART));
			}
		} 
		catch (YADARequestException e)
		{
			String msg = "There was problem creating the Converter.";
			throw new YADAResponseException(msg,e);
		}
		
		return this;
	}
	
	/**
	 * Outputs a {@link String} generated by Java XML API serialization methods.
	 * @return the document as a string
	 * @see org.w3c.dom.ls.DOMImplementationLS
	 * @see org.w3c.dom.ls.LSSerializer
	 * @see org.w3c.dom.ls.LSOutput
	 */
	@Override
	public String toString() {
		DOMImplementationLS domImplementation = (DOMImplementationLS) this.doc.getImplementation();
	  LSSerializer        lsSerializer      = domImplementation.createLSSerializer();
	  LSOutput            lsOutput          = domImplementation.createLSOutput();
	  Writer stringWriter                   = new StringWriter();
	  lsOutput.setEncoding("UTF-8");
	  lsOutput.setCharacterStream(stringWriter);
	  lsSerializer.write(this.doc, lsOutput);      
	  return stringWriter.toString();
	}

	
	/**
	 * Calls {@link #toString()}, ignoring {@code prettyPrint}
	 * @see com.novartis.opensource.yada.format.AbstractResponse#toString(boolean)
	 */
	@Override
	public String toString(boolean prettyPrint) throws YADAResponseException {
		return this.toString();
	}

	

}
