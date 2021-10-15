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
 * ADFHelper is for files in Adnan Derti format.  These are tab-delimited files with the following fileheader:
 * 
<pre>
__sample name: 22RV1
__species: human
__sample type: cell.line
__alignment program: TopHat v1.3.KornMod
__expression program: Cufflinks v2.0
__contents: FPKM
__no transcript: ---
__BEGINDATA
</pre>
 * 
 * The column header is on the next line, line 9.
 * 
 * @author David Varon
 *
 */
public class ADFHelper extends TabHelper {
	
	/**
	 * Local logger handle
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ADFHelper.class);
	
	//  header regex basic matches this:  __(header): (value)
	/**
	 * Constant equal to: {@code __(.+):\\s*(.+)}
	 */
	private final static Pattern HEADER_RX   = Pattern.compile("__(.+):\\s*(.+)");
	/**
	 * Constant equal to: {@value}
	 */
	private final static String  H_BEGINDATA = "__BEGINDATA";
	
	/**
	 * Sets file header values until encountering {@link #H_BEGINDATA}, then sets the column header line. 
	 * @see com.novartis.opensource.yada.io.TabHelper#setHeaders()
	 */
	@Override
	protected void setHeaders() throws YADAIOException 
	{
		LOG.debug("Setting headers dynamically...");
		String line       = "";
		StringBuffer fh   = new StringBuffer();
		
		boolean beginData     = false;
		boolean areHeadersSet = false;
		
		try
		{
			while(!areHeadersSet && (line = ((BufferedReader)this.reader).readLine()) != null)
			{
				if (!beginData)
				{
					if(!H_BEGINDATA.equals(line))
					{
						fh.append(line);
						fh.append(NEWLINE);
					}
					else
					{
						beginData = true;
					}
				}
				else
				{
					setColumnHeader(line);
					setFileHeader(fh.toString());
					areHeadersSet = true;
				}
			}
			setColHeaderArray();
			setFileHeaderMap();
		}
		catch (IOException e)
		{
			throw new YADAIOException(e.getMessage(),e);
		}
	}
	
	/**
	 * Builds a map from the values in the file header buffer.
	 * @see com.novartis.opensource.yada.io.FileHelper#setFileHeaderMap()
	 */
	@Override
	protected void setFileHeaderMap() 
	{
		LOG.debug("Setting ADF file header...");
		try(Scanner s = new Scanner(getFileHeader()))
		{
  		String  h = "";
  		if (null == this.fileHeaderMap)
  		{
  			this.fileHeaderMap = new HashMap<>();
  		}
  		try 
  		{
  			while(s.hasNextLine())
  			{
  				h = s.nextLine();
  				Matcher m = HEADER_RX.matcher(h);
  				if (m.matches())
  				{
  					this.fileHeaderMap.put(m.group(1),m.group(2));
  				}
  			}
  		} 
  		catch (NoSuchElementException e) 
  		{
  			e.printStackTrace();
  		}
		}
	}
}
