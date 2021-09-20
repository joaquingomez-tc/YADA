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

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novartis.opensource.yada.ConnectionFactory;
import com.novartis.opensource.yada.Finder;
import com.novartis.opensource.yada.YADAConnectionException;
import com.novartis.opensource.yada.YADAFinderException;
import com.novartis.opensource.yada.YADAQuery;
import com.novartis.opensource.yada.YADAQueryConfigurationException;
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
	private static Logger l = LoggerFactory.getLogger(CacheUpdater.class);

	/** 
	 * @see com.novartis.opensource.yada.plugin.Bypass#engage(com.novartis.opensource.yada.YADARequest)
	 */
	@SuppressWarnings("unchecked")
  @Override
	public String engage(YADARequest yadaReq) throws YADAPluginException
	{
		Cache yadaIndex = ConnectionFactory.getConnectionFactory().getCacheConnection(Finder.YADA_CACHE_MGR,Finder.YADA_CACHE);
		for(Object q : yadaIndex.getKeys().toArray(new Object[yadaIndex.getKeys().size()]))
		{
			try
			{
				l.debug("Refreshing verson of [" + q + "] in cache.");
				YADAQuery yq = null;
				try 
				{
				  yq = new Finder().getQueryFromIndex((String)q);
				}
				catch(YADAFinderException e)
				{
				  l.warn("Attempted to update cached version non-existent query. This usually means someone changed the qname directly in the index. The old query was removed from the cache.");
				  yadaIndex.remove(q);
				} 
				if(yq != null)
				{
				  Element element = new Element(q, yq);
				  yadaIndex.put(element); // automatically overwrites, or writes anew
				}
			} 
			catch (YADAConnectionException e)
			{
				throw new YADAPluginException(e.getMessage(), e);
			} 
			catch (YADAQueryConfigurationException e) 
      {
			  throw new YADAPluginException(e.getMessage(), e);
			}
		}
		return "Cache successfully updated on " + new java.util.Date().toString();
	}

}
