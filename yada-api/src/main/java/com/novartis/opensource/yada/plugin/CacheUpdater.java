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
/**
 * 
 */
package com.novartis.opensource.yada.plugin;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novartis.opensource.yada.ConnectionFactory;
import com.novartis.opensource.yada.Finder;
import com.novartis.opensource.yada.YADAFinderException;
import com.novartis.opensource.yada.YADAQuery;
import com.novartis.opensource.yada.YADARequest;

/**
 * Updates all queries in the cache.  Useful when updating the YADA Index from the command line.
 * @author David Varon
 * @since 4.1.0
 */
public class CacheUpdater extends AbstractBypass
{

	/**
	 * Local logger handle
	 */
	private static final Logger LOG = LoggerFactory.getLogger(CacheUpdater.class);

	/** 
	 * @see com.novartis.opensource.yada.plugin.Bypass#engage(com.novartis.opensource.yada.YADARequest)
	 */	
  @Override
	public String engage(YADARequest yadaReq) throws YADAPluginException
	{    
		Map<String,YADAQuery> yadaIndex = ConnectionFactory.getConnectionFactory().getCache();
		for(String q : yadaIndex.keySet().toArray(new String[yadaIndex.keySet().size()]))
		{
			LOG.debug("Refreshing verson of [{}] in cache.",q);
      YADAQuery yq = null;
      try 
      {
        yq = new Finder().getQueryFromLib(q);
      }
      catch(YADAFinderException e)
      {
        LOG.warn("Attempted to update cached version non-existent query. This usually means someone changed the qname directly in the index. The old query was removed from the cache.");
        yadaIndex.remove(q);
      } 
      if(yq != null)
      {				  
        yadaIndex.put(q,yq); // automatically overwrites, or writes anew
      }
		}
		return "Cache successfully updated on " + new java.util.Date().toString();
	}

}
