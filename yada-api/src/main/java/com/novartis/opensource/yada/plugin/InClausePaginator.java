package com.novartis.opensource.yada.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.novartis.opensource.yada.JSONParams;
import com.novartis.opensource.yada.Service;
import com.novartis.opensource.yada.YADAException;
import com.novartis.opensource.yada.YADAQuery;
import com.novartis.opensource.yada.YADARequest;
import com.novartis.opensource.yada.YADARequestException;

public class InClausePaginator implements Preprocess, Postprocess {
	
	
	private int pageOffset = 0;
	private int page       = 1;
	private ArrayList<Integer> inPages;
	private String[][] fullInClauses;
	private JSONArray inClauses;
	
	public InClausePaginator() {
		this.inPages = new ArrayList<Integer>();
		this.inClauses = new JSONArray();
	}

	// Postprocessor req level
	@Override
	public String engage(YADARequest yadaReq, String result) throws YADAPluginException {
		
		
		if(yadaReq.getFilters() != null && yadaReq.getPageSize() != -1) {
			
			//cut off all results before offset
			//offset equals 0
			//while num results < pz
			// execute another request
			// increment page, then in pages in order, resetting all earlier back to 0 when one is incremented
			// only resort to incrementing one further in list if exactly 0 results are returned.
			//finish when num results >= pz or all pages result in empty in clauses
			// if num results > pz, cut off last until num results=pz, set offset
			
			
			List<String> args = yadaReq.getArgs();
			int fullInArgIndex = 1;
			if(args == null) {
				args = yadaReq.getArgLists().get(1);
				fullInArgIndex = 2;
			}
			if(args.size() > 0) {
				if(args.size() < 3) {
					throw new YADAPluginException("Not enough page values");
				}
				else {
					this.pageOffset = Integer.parseInt(args.get(0));
					this.page       = Integer.parseInt(args.get(1));
					for(int i=2; i<args.size(); i++) {
						this.inPages.add(Integer.parseInt(args.get(i)));
					}
					JSONArray fullInJSON = new JSONArray(yadaReq.getArgLists().get(fullInArgIndex).get(0));
					this.fullInClauses = new String[fullInJSON.length()][];
					for(int i=0; i<fullInJSON.length(); i++) {
						this.fullInClauses[i] = new String[fullInJSON.getJSONArray(i).length()];
						for(int j=0; j<fullInJSON.getJSONArray(i).length(); j++) {
							this.fullInClauses[i][j] = fullInJSON.getJSONArray(i).getString(j);
						}
					}
				}
			}
			
			this.inClauses = new JSONArray();
			JSONObject filters = yadaReq.getFilters();
			ArrayList<JSONObject> ins = this.getIns(filters);
			for(int i=0; i<ins.size(); i++) {
				this.inClauses.put(ins.get(i));
			}
			
			//set "page" in result to [offset, page, [...in pages]]
			
			JSONObject resultObj = new JSONObject(result);
			
			JSONObject resultset = resultObj.getJSONObject("RESULTSET");
			
			JSONArray rows = resultset.getJSONArray("ROWS");
			
			ArrayList<JSONObject> results = new ArrayList<JSONObject>();
			for(int i=0; i<rows.length(); i++) {
				results.add(rows.getJSONObject(i));
			}
			
			results.subList(0, this.pageOffset).clear();
			int lastTotal = results.size();
			this.pageOffset = 0;
			
			boolean exhausted = false;
			yadaReq.setPlugin(new String[0]);
			while(!exhausted && results.size() < yadaReq.getPageSize()) {
				
				//increase pages
				exhausted = this.incrementPages(lastTotal, yadaReq.getPageSize());
				
				if(!exhausted) {
					this.updateFilters(yadaReq.getPageSize());
					String[] updatedFilters = {filters.toString()};
					
					
					try {
						yadaReq.setFilters(updatedFilters);
					} catch (YADARequestException e) {
						throw new YADAPluginException("Could not paginate filters");
					}
					
					YADARequest serviceReq = new YADARequest();
					
					String[] serviceFilters = {yadaReq.getFilters().toString()};
					String[] serviceCount   = {Boolean.toString(yadaReq.getCount())};
					String[] serviceCountOnly   = {Boolean.toString(yadaReq.getCountOnly())};
					JSONParams serviceJsonParams   = yadaReq.getJsonParams();
					String[] servicePageSize = {Integer.toString(yadaReq.getPageSize())};
					String[] servicePageStart = {Integer.toString(this.page)};
					String[] serviceQname = {yadaReq.getQname()};
					String[][] serviceParamList      = yadaReq.getParams();
					if(serviceParamList != null) {
						String[]  serviceParams = new String[serviceParamList.length];
						for(int i=0; i<serviceParams.length; i++) {
							serviceParams[i] = "["+String.join(",", serviceParamList[i])+"]";
						}
						serviceReq.setParams(serviceParams);
					}
					try {
						serviceReq.setFilters(serviceFilters);
						serviceReq.setCount(serviceCount);
						serviceReq.setCountOnly(serviceCountOnly);
						serviceReq.setJsonParams(serviceJsonParams);
						serviceReq.setPageSize(servicePageSize);
						serviceReq.setPageStart(servicePageStart);
						serviceReq.setQname(serviceQname);
					} catch (YADARequestException e) {
						throw new YADAPluginException("Could not create secondary query");
					}
					
					Service svc = new Service(serviceReq);
					JSONArray newRowsJSON = null;
          try
          {
            newRowsJSON = (new JSONObject(svc.execute())).getJSONObject("RESULTSET").getJSONArray("ROWS");
          }
          catch (JSONException | YADAException e)
          {
            throw new YADAPluginException(e);
          }
					
					ArrayList<JSONObject> newRows = new ArrayList<JSONObject>();
					for(int i=0; i<newRowsJSON.length(); i++) {
						newRows.add(newRowsJSON.getJSONObject(i));
					}
					
					
					//cut it down in the case that we get too many rows back
					int numOver = results.size() + newRows.size() - yadaReq.getPageSize();
					if(numOver > 0) {
						this.pageOffset = newRows.size() - numOver;
						newRows.subList(this.pageOffset, newRows.size()).clear();
					}
					lastTotal = newRows.size();
					
					results.addAll(newRows);
				}
			}
			
			if(this.pageOffset == 0) {
				this.page++;
			}
			
			resultset.put("ROWS", results);
			
			JSONArray pages = new JSONArray();
			pages.put(this.pageOffset);
			pages.put(this.page);
			for(int i=0; i<this.inPages.size(); i++) {
				pages.put(this.inPages.get(i));
			}
			
			resultset.put("page", pages);
			resultset.remove("total"); //total is <=num that comes back from elastic
			resultset.remove("records");//should be 50 except last page
			
			
			JSONObject finalResponse = new JSONObject();
			finalResponse.put("RESULTSET", resultset);
			
			return finalResponse.toString();
		}
		return result;
	}
	

	// Postprocessor query level
	@Override
	public void engage(YADAQuery yq) throws YADAPluginException {
		

	}

	// preprocessor req level
	@Override
	public YADARequest engage(YADARequest yadaReq) throws YADAPluginException {

		JSONObject filters = yadaReq.getFilters();
		int pagesize       = yadaReq.getPageSize();
		
		if(filters != null && pagesize != -1) {
			try {
				yadaReq.setFilters(new String[] {optimizeFilters(filters, new JSONObject(), new JSONObject()).toString()});
			} catch (YADARequestException e) {
				throw new YADAPluginException("Could not simplify filters");
			}
					
			
			ArrayList<JSONObject> inFilters = this.getIns(filters);
			this.fullInClauses = new String[inFilters.size()][];
			for(int i=0; i< inFilters.size(); i++) {
				this.fullInClauses[i] = inFilters.get(i).getString("data").split(",");
				this.inClauses.put(inFilters.get(i));
			}
			
			
			
			List<String> args = yadaReq.getArgs();
			if(args != null && args.size() > 0) {
				if(args.size() < 2) {
					throw new YADAPluginException("Not enough page values");
				}
				else {
					this.pageOffset = Integer.parseInt(args.get(0));
					this.page       = Integer.parseInt(args.get(1));
					for(int i=2; i<args.size(); i++) {
						this.inPages.add(Integer.parseInt(args.get(i)));
					}
				}
			}
			else {
				//if there are no args passed to pl, defaults to 0 offset, page 1, page 0 for all in clauses
				this.inPages = new ArrayList<Integer>();
				for(int i=0; i<inFilters.size(); i++) {
					this.inPages.add(0);
				}
				this.page = 1;
				this.pageOffset = 0;
				ArrayList<String> newPluginArgs = new ArrayList<String>();
				newPluginArgs.add("0");
				newPluginArgs.add("1");
				for(Integer x : this.inPages) {
					newPluginArgs.add(""+x);
				}
				yadaReq.addPluginArgs(newPluginArgs);
			}
			
			
			ArrayList<ArrayList<String>> serializedFullIns = new ArrayList<ArrayList<String>>();
			
			for(int i=0; i<this.fullInClauses.length; i++) {
				serializedFullIns.add(new ArrayList<String>());
				for(int j=0; j< this.fullInClauses[i].length; j++) {
					serializedFullIns.get(i).add(this.fullInClauses[i][j]);
				}
			}
			
			ArrayList<String> newArg = new ArrayList<String>();
			newArg.add(serializedFullIns.toString());
			yadaReq.addPluginArgs(newArg);
			
			if(this.inPages.size() > 0 && this.inPages.size() != this.fullInClauses.length) {
				throw new YADAPluginException("Pages arg must be two elements longer than the number of in clauses");
			}
			else {
				this.updateFilters(yadaReq.getPageSize());
			}
			
			String[] updatedFilters = {filters.toString()};
			try {
				yadaReq.setFilters(updatedFilters);
			} catch (YADARequestException e) {
				throw new YADAPluginException("Could not paginate filters");
			}
					
			String[] psa = {""+this.page};
			yadaReq.setPageStart(psa);
			
		}
		return yadaReq;
		
	}
	
	
	private boolean incrementPages(int total, int pz) {
		if(total == 0) {
			this.page = 1;
			boolean incremented = false;
			int index = 0;
			while(!incremented) {
				if(this.fullInClauses[index].length > (this.inPages.get(index)+1)*pz) {
					this.inPages.set(index, this.inPages.get(index) + 1);
					incremented = true;
				}
				else {
					this.inPages.set(index, 0);
					index++;
					if(index >= this.fullInClauses.length) {
						this.page = -1;
						return true;
					}
					incremented = true;
				}
				
			}
			return false;
			
		}
		else {
			this.page++;
			return false;
		}
	}
	
	
	private void updateFilters(int pz) {
		for(int i=0; i<this.inPages.size(); i++) {
			String[] subArray = Arrays.copyOfRange(this.fullInClauses[i], Math.min(this.fullInClauses[i].length, this.inPages.get(i)*pz), Math.min(this.fullInClauses[i].length, (1 + this.inPages.get(i))*pz));
			String paginated = String.join(",", subArray);
			this.inClauses.getJSONObject(i).put("data", paginated);
		}
	}
	
	
	
	private ArrayList<JSONObject> getIns(JSONObject filters) {
		ArrayList<JSONObject> inFilters = new ArrayList<JSONObject>();
		
		JSONArray rules = filters.getJSONArray("rules");
		for(int i=0; i<rules.length(); i++) {
			JSONObject rule = rules.getJSONObject(i);
			if(rule.getString("op").equals("in")) {
				inFilters.add(rule);
			}
		}
		if(filters.has("groups")) {
			JSONArray groups = filters.getJSONArray("groups");
			for(int i=0; i<groups.length(); i++) {
				inFilters.addAll(getIns(groups.getJSONObject(i)));
			}
		}
		return inFilters;
	}
	
	private JSONObject optimizeFilters(JSONObject filters, JSONObject include, JSONObject exclude) {
		
		JSONArray rules = filters.getJSONArray("rules");
		
		ArrayList<JSONObject> singleRules = new ArrayList<JSONObject>();
		
		for(int i=0; i<rules.length(); i++) {
			singleRules.add(rules.getJSONObject(i));
		}
				
		
		if(filters.has("groups")) {
			JSONArray groups = filters.getJSONArray("groups");
			
			for(int i=0; i<groups.length(); i++) {
				JSONObject group = groups.getJSONObject(i);
				ArrayList<JSONObject> groupRules = getRules(group);
				if(groupRules.size() == 1) {
					singleRules.add(groupRules.get(0));
					groups.remove(i);
					i--;
				}
			}
		}
		
		JSONObject singleIns = new JSONObject();
		for(int i=0; i< singleRules.size(); i++) {
			JSONObject rule = singleRules.get(i);
			if(rule.getString("op").equals("in")) {
				List<String> ruleData = Arrays.asList(rule.getString("data").split(","));
				if(!singleIns.has(rule.getString("field"))) {
					singleIns.put(rule.getString("field"), ruleData);
				}
				else if(filters.getString("groupOp").toLowerCase().equals("or")) {
					for(int j=0; j<ruleData.size(); j++) {
						singleIns.accumulate(rule.getString("field"), ruleData.get(j));
					}
				}
				else {
					JSONArray singleIn = singleIns.getJSONArray(rule.getString("field"));
					ArrayList<String> inMembers = new ArrayList<String>();
					for(int j=0; j<singleIn.length(); j++) {
						inMembers.add(singleIn.getString(j));
					}
					inMembers.retainAll(ruleData);
					singleIns.put(rule.getString("field"), inMembers);
				}

				singleRules.remove(i);
				i--;
			}
		}
		
		@SuppressWarnings("unchecked")
		Set<String> keys = singleIns.keySet();
		for(String key : keys) {
			JSONObject rule = new JSONObject();
			rule.put("op", "in");
			StringBuilder sb = new StringBuilder();
			JSONArray data = singleIns.getJSONArray(key);
			for(int i=0; i<data.length(); i++) {
				sb.append(data.getString(i));
				if(i != data.length()-1) {
					sb.append(",");
				}
			}
			if(data.length() != 0) {
				rule.put("data", sb.toString());
				rule.put("field", key);
				singleRules.add(rule);
			}
		}
		
		
		filters.put("rules", singleRules);
		
		
		
		
		
		for(int i=0; i<singleRules.size(); i++) {
			JSONObject rule = singleRules.get(i);
			if(rule.getString("op").equals("in")) {
				if(include.has(rule.getString("field")) || exclude.has(rule.getString("field"))) {
					
					ArrayList<String> inClause = new ArrayList<String>(Arrays.asList(rule.getString("data").split(",")));
					
					if(include.has(rule.getString("field"))) {
						JSONArray includedArray = (JSONArray)include.get(rule.getString("field"));
						ArrayList<String> includedVals = new ArrayList<String>();
						for(int j=0; j<includedArray.length(); j++) {
							includedVals.add(includedArray.getString(j));
						}
						inClause.retainAll(includedVals);
					}
					if(exclude.has(rule.getString("field"))) {
						JSONArray excludedArray = (JSONArray)exclude.get(rule.getString("field"));
						ArrayList<String> excludedVals = new ArrayList<String>();
						for(int j=0; j<excludedArray.length(); j++) {
							excludedVals.add(excludedArray.getString(j));
						}
						inClause.removeAll(excludedVals);
					}

					for(String value : inClause) {
						if(filters.getString("groupOp").toLowerCase().equals("and")) {
							include.accumulate(rule.getString("field"), value);
						}
						else {
							exclude.accumulate(rule.getString("field"), value);
						}
					}
					
					rule.put("data", String.join(",", inClause));
				}
				else {
					include.put(rule.getString("field"), new ArrayList<String>(Arrays.asList(rule.getString("data").split(","))));
				}
			}
		}
		
		if(filters.has("groups")) {
			JSONArray allGroups = filters.getJSONArray("groups");
			for(int i=0; i<allGroups.length(); i++) {
				if(getRules(allGroups.getJSONObject(i)).size() > 1) {
					String[] toExcludeNames = JSONObject.getNames(exclude);
					String[] toIncludeNames = JSONObject.getNames(include);
					if(toExcludeNames == null) {
						toExcludeNames = new String[0];
					}
					if(toIncludeNames == null) {
						toIncludeNames = new String[0];
					}
					allGroups.put(i, optimizeFilters(allGroups.getJSONObject(i), new JSONObject(include, toIncludeNames), new JSONObject(exclude, toExcludeNames)));
				}
			}
		}
		
		return filters;
	}
	
	
	private ArrayList<JSONObject> getRules(JSONObject group) {
		ArrayList<JSONObject> rules = new ArrayList<JSONObject>();
		
		for(int i=0; i<group.getJSONArray("rules").length(); i++) {
			rules.add(group.getJSONArray("rules").getJSONObject(i));
		}
		
		if(group.has("groups")) {
			JSONArray nestedGroups = group.getJSONArray("groups");
			for(int i=0; i<nestedGroups.length(); i++) {
				rules.addAll(getRules(nestedGroups.getJSONObject(i)));
			}
		}
		
		return rules;
	}
	
	// preprocessor query level
	@Override
	public void engage(YADARequest yadaReq, YADAQuery yq) throws YADAPluginException {
		// TODO Auto-generated method stub
		
		yadaReq.getFilters();

	}

}

