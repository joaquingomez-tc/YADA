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
package com.novartis.opensource.yada.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novartis.opensource.yada.Finder;
import com.novartis.opensource.yada.YADARequest;
import com.novartis.opensource.yada.YADAResourceException;

/**
 * A java API enabling execution of pre-existing scripts, in pre-defined locations on the YADA server, as Bypass plugins. 
 * For more information see the <a href="../../../../../../../guide.html">Users' Guide</a>
 * @author David Varon
 *
 */
public class ScriptBypass extends AbstractBypass {
	/**
	 * Local logger handle
	 */
	private static Logger l = LoggerFactory.getLogger(ScriptBypass.class);
	
	/**
	 * Enables the execution of a script stored in the {@code yada.bin} directory.
	 * To execute a script bypass plugin, pass {@code bypassargs}, or just {@code args}
	 * the first argument being the name of the script executable, and the rest of the arguments
	 * those, in order, to pass to it. If {@link YADARequest}{@code .getBypassArgs()} is not null
	 * and {@link YADARequest#getPlugin()} is null, then the plugin will be set to 
	 * {@link YADARequest#SCRIPT_BYPASS} automatically.
	 * <p>
	 * The script should return a String intended to be returned to the requesting client.
	 * </p>
	 * @see com.novartis.opensource.yada.plugin.Bypass#engage(com.novartis.opensource.yada.YADARequest)
	 */
	@Override
	public String engage(YADARequest yadaReq) throws YADAPluginException
	{
		List<String> cmds = new ArrayList<>();
		// add args
		List<String> args = yadaReq.getBypassArgs().size() == 0 ? yadaReq.getArgs() : yadaReq.getBypassArgs();
		// add plugin
		try 
		{
			cmds.add(Finder.getEnv("yada.bin")+args.remove(0));
		} 
		catch (YADAResourceException e)
		{
			String msg = "There was a problem locating the resource or variable identified by the supplied JNDI path (yada.bin) in the initial context.";
			throw new YADAPluginException(msg,e);
		}
		// add args
		cmds.addAll(args);
		for (String arg : args)
		{
			cmds.add(arg);
		}
		// add yadaReq json
		cmds.add(yadaReq.toString());
		l.debug("Executing script plugin: "+cmds);
		String scriptResult = "";
		String s            = null;
		try
		{
			ProcessBuilder builder = new ProcessBuilder(cmds);
			builder.redirectErrorStream(true);
			Process process = builder.start();
			try(BufferedReader si = new BufferedReader(new InputStreamReader(process.getInputStream())))
			{
  			while ((s = si.readLine()) != null)
  			{
  				l.debug("  LINE: "+s);
  				scriptResult += s;
  			}
			}
			process.waitFor();
		}
		catch(IOException e)
		{
			String msg = "Failed to get input from InputStream.";
			throw new YADAPluginException(msg,e);
		}
		catch(InterruptedException e)
		{
			String msg = "The external process executing the script was interrupted.";
			throw new YADAPluginException(msg,e);
		}
		return scriptResult;
	}

}
