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
package com.novartis.opensource.yada.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * See VCF specification here: <a href="http://www.1000genomes.org/wiki/Analysis/Variant%20Call%20Format/vcf-variant-call-format-version-41">1000Genomes</a>
 * @author David Varon
 *
 */
public class VCFHelper extends TabHelper{

	/**
   * Local logger handle
   */
	private static final Logger LOG = LoggerFactory.getLogger(VCFHelper.class);
	/**
	 * Constant equal to: {@code "##(.*)=(.*)"}
	 */
	protected final static Pattern HEADER_RX   = Pattern.compile("##(.*)=(.*)");
	/**
	 * Constant equal to: {@code "#CHROM\\s.*"}
	 */
	protected final static Pattern COL_HEAD_RX = Pattern.compile("#CHROM\\s.*");
	/**
	 * Constant equal to: {@code "##(INFO|FILTER|FORMAT|ALT)=<((ID|Number|Type|Description)=(\"?.*\"?))+>"}
	 */
	protected final static Pattern H_FIELDS_RX = Pattern.compile("##(INFO|FILTER|FORMAT|ALT)=<((ID|Number|Type|Description)=(\"?.*\"?))+>");	
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String FILE_FORMAT = "fileFormat";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String DESCRIPTION = "Description";
	/**
	 * Constant equal to: {@value}
	 */	
	protected final static String CHROM  = "CHROM";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String POS    = "POS";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String ID     = "ID";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String REF    = "REF";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String ALT    = "ALT";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String QUAL   = "QUAL";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String FILTER = "FILTER";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String INFO   = "INFO";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String FORMAT = "FORMAT";
	/**
	 * Constant equal to: {@value}
	 */
	protected final static String DOT    = ".";
	
	/**
	 * Processes fileheader an column headers in VCF files
	 * @see com.novartis.opensource.yada.io.TabHelper#setHeaders()
	 */
	@Override
	protected void setHeaders() throws YADAIOException
	{
		String       line = "";
		StringBuffer fh   = new StringBuffer();
		
		boolean areHeadersSet = false;
		
		try
		{
			while(!areHeadersSet && (line = ((BufferedReader)this.reader).readLine()) != null)
			{
				Matcher m = COL_HEAD_RX.matcher(line);
				if(m.matches())
				{
					areHeadersSet = true;
					setColumnHeader(line);
					setFileHeader(fh.toString());				
				}
				else
				{
					fh.append(line);
					fh.append(NEWLINE);
				}
			}
			setColHeaderArray();
			setFileHeaderMap();
		}
		catch(IOException e)
		{
			throw new YADAIOException(e.getMessage(),e);
		}
	}
	
	/**
	 * The VCF file header spec is well defined, and is handled here. 
	 * See <a href="http://www.1000genomes.org/wiki/analysis/variant%20call%20format/vcf-variant-call-format-version-41">VCF Spec</a>
	 * 
	 * @see com.novartis.opensource.yada.io.FileHelper#setFileHeaderMap()
	 */
	@Override
	protected void setFileHeaderMap()
	{
		LOG.info("Setting VCF file header...");
		try(Scanner s    = new Scanner(getFileHeader()))
		{
  		String  line = "";
  		if (null == this.fileHeaderMap)
  		{
  			this.fileHeaderMap = new HashMap<>();
  		}
  		try
  		{
  			while(s.hasNextLine())
  			{
  				line      = s.nextLine();
  				Matcher m = HEADER_RX.matcher(line); 
  				if(m.matches())
  				{
  					// it's a file header line
  					String key = m.group(1);
  					String val = m.group(2);
  					Matcher m_xml = H_FIELDS_RX.matcher(line);
  					if (m_xml.matches())
  					{
  						// it's an INFO/FILTER/FORMAT/ALT line
  						String xmlVal = m_xml.group(2);
  						// ##INFO=<ID=ID,Number=number,Type=type,Description=”description”>
  						// Possible Types for INFO fields are: Integer, Float, Flag, Character, and String.
  						
  						// ##FILTER=<ID=ID,Description=”description”>
  						
  						// ##FORMAT=<ID=ID,Number=number,Type=type,Description=”description”>
  						
  						// handle the description first, because it's a quoted string which can contain commas
  						String[] attribsDesc = xmlVal.split(DESCRIPTION+"=");
  						this.fileHeaderMap.put(key+"_"+DESCRIPTION,attribsDesc[1]);
  						
  						// get the attribs in an array
  						String[] attribs = attribsDesc[0].split(",");
  						
  						for(String attr : attribs)
  						{
  							// get each pair
  							String[] pair = attr.split("=");
  							// put the pair in the file header map, i.e.,
  							//   INFO_ID, val;  FILTER_Description, val; etc
  							this.fileHeaderMap.put(key+"_"+pair[0],pair[1]);
  						}
  					}
  					else
  					{
  						this.fileHeaderMap.put(key, val);
  					}
  				}
  			}
  		}
  		catch(NoSuchElementException e)
  		{
  			e.printStackTrace();
  		}
		}
	}
}
