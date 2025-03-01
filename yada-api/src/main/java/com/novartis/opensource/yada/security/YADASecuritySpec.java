/**
 * 
 */
package com.novartis.opensource.yada.security;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.novartis.opensource.yada.YADAQueryConfigurationException;

/**
 * @author dvaron
 * @since 9.0.0
 */
public class YADASecuritySpec extends HashMap<String, Object> {
  
  /**
   * 
   */
  private static Logger l = LoggerFactory.getLogger(YADASecuritySpec.class); 
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * 
   */
  public final static String KEY_AUTH_PATH_RX = "auth.path.rx";
  /**
   * 
   */
  public final static String KEY_POLICY = "policy";
  /**
   * 
   */
  public final static String KEY_TYPE = "type";
  /**
   * 
   */
  public final static String KEY_QUALIFIER = "qualifier";
  /**
   * 
   */
  public final static String KEY_PREDICATE = "predicate";
  /**
   * 
   */
  public final static String KEY_PROTECTOR = "protector";
  /**
   * 
   */
  public final static String KEY_COLUMNS = "columns";
  /**
   * 
   */
  public final static String KEY_INDEXES = "indexes";
  /**
   * 
   */
  public final static String KEY_INDICES = "indices";
  /**
   * 
   */
  public final static String KEY_TOKEN_VALIDATOR = "token.validator";
  /**
   * 
   */
  public final static String POLICY_EXECUTION = "E";
  /**
   * 
   */
  public final static String POLICY_CONTENT = "C";
  /**
   * 
   */
  public final static String POLICY_AUTHORIZATION  = "A";
  /**
   * 
   */
  public final static String TYPE_ALLOWLIST = "whitelist";
  /**
   * 
   */
  public final static String TYPE_DENYLIST = "blacklist";
  /**
   * Constant equal to {@value}
   * 
   * @since 9.0.0 (moved from {@link com.novartis.opensource.yada.security.Gatekeeperer}
   */
  public static final String RX_COL_INJECTION = "(?:([a-zA-Z0-9_]+):)?(get[A-Z][a-zA-Z0-9_]+\\(([A-Za-z0-9_]*)\\))";

  
  /**
   * Constant equal to {@value}
   * 
   * @since 9.0.0 (moved from {@lincom.novartis.opensource.yada.security.Gatekeepereper}
   */
  public static final String RX_IDX_INJECTION = "(?:([0-9]+):)?(get[A-Z][a-zA-Z0-9_]+\\(([A-Za-z0-9_]*)\\))";
  
  
  /**
   * Constant equal to {@value}
   * @since 9.0.0
   */
  public final static String RX_COL = "^"+RX_COL_INJECTION+"|[A-Za-z0-9_]+$";
  
  /**
   * Constant equal to {@value}
   * @since 9.0.0
   */
  public final static String RX_IDX = "^"+RX_IDX_INJECTION+"|[\\d]+$";
  
  /**
   * 
   */
  public final static List<String> keyFields = getKeyFields(); 
  

  
  /**
   * 
   */
  public YADASecuritySpec() {}
  
  /**
   * @param spec a {@link JSONObject} containing the specification to be instantiated
   * @throws YADAQueryConfigurationException when the spec is invalid
   */
  public YADASecuritySpec(JSONObject spec) throws YADAQueryConfigurationException
  {    
    for(String key : JSONObject.getNames(spec))
    {      
      // only add key to spec if it matches a private "KEY" field
      if(keyFields.contains(key))
      {
        try
        {
          this.put(key, spec.get(key));
        }
        catch(JSONException e)
        {
          String msg = String.format("[%s] key caused an error", key);
          throw new YADAQueryConfigurationException(msg);
        }        
      }
      else
      {
        String msg = String.format("[%s] is not permitted in YADASecuritySpec", key);
        throw new YADAQueryConfigurationException(msg);
      }
    }
  }
  
  /**
   * @return all the keys in this spec which correspond to constants having the {@code KEY} prefix 
   */
  public final static List<String> getKeyFields() 
  {
    List<String> names = new ArrayList<String>();
    for(Field field : YADASecuritySpec.class.getDeclaredFields())
    {
      if(field.getName().startsWith("KEY"))
      {
        try
        {
          names.add(field.get(null).toString());
          l.debug("YADASecuritySpec KEY field: "+field.get(null).toString());
        }
        catch (IllegalArgumentException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
      }
    }
    return names;
  }
  
  /**
   * @param value the classname of the {@lcom.novartis.opensource.yada.security.TokenValidatoridator}
   * @throws YADAQueryConfigurationException when {@code value} is {@code null}
   */
  public void setTokenValidator(String value) throws YADAQueryConfigurationException 
  {
    if(null == value)
      throw new YADAQueryConfigurationException("Token validator value must be non-null");
    this.put(KEY_TOKEN_VALIDATOR, value);
  }
  
  /**
   * @return the value of the {@link #KEY_TOKEN_VALIDATOR} entry
   */ 
  public String getTokenValidator()
  {
    return (String) this.get(KEY_TOKEN_VALIDATOR);
  }
  
  /**
   * @param value a regular expression for validating request urls
   * @throws YADAQueryConfigurationException when {@code value} is {@code null}
   */
  public void setURLSpec(String value) throws YADAQueryConfigurationException 
  {
    if(null == value)
      throw new YADAQueryConfigurationException("Authorized URL path expression must be non-null");
    this.put(KEY_AUTH_PATH_RX, value);
  }
  
  /**
   * @return the value of the {@link #KEY_AUTH_PATH_RX} entry
   */
  public String getURLSpec() 
  {
    return (String) this.get(KEY_AUTH_PATH_RX);
  }
  
  /**
   * @param type {@link #TYPE_ALLOWLIST} or {@link #TYPE_DENYLIST}
   * @param protector the name of the protector query
   * @param columnsOrIndexes a {@link List} of columns or indices from the query config
   * @throws YADAQueryConfigurationException if any parameter is {@code null}
   */
  @SuppressWarnings("unchecked")
  public void setExecutionPolicy(String type, String protector, List<?> columnsOrIndexes) throws YADAQueryConfigurationException 
  {
    List<String> list = (List<String>) columnsOrIndexes;
    boolean isColumns = (null != list && list.stream().filter(c -> c.matches(RX_COL)).collect(Collectors.toList()).size() > 0);
    if(null == type || null == protector 
        || (!isColumns && list.stream().filter(c -> c.matches(RX_IDX)).collect(Collectors.toList()).size() == 0))
      throw new YADAQueryConfigurationException("policy, type, protector, and columns, indexes, or indices must be non-null");
//    this.put(KEY_POLICY, POLICY_EXECUTION);
    this.put(KEY_TYPE, type);
    this.put(KEY_PROTECTOR, protector);
    if(isColumns)
    {
      this.put(KEY_COLUMNS, columnsOrIndexes);
    }
    else
    {
      this.put(KEY_INDEXES, columnsOrIndexes);
    }
  }
  /**
   * @return {@code true} if {@link #KEY_PROTECTOR} is present in the specification
   */
  public boolean hasExecutionPolicy()
  {
    return (this.get(KEY_PROTECTOR) != null);
  }
  
  /**
   * @return a {@link Map} containing the {@link #POLICY_EXECUTION} specs
   */
  public Map<String, Object> getExecutionPolicy()  
  {
    Map<String,Object> policy = null;
//    if(this.get(KEY_POLICY) == POLICY_EXECUTION)
    if(hasExecutionPolicy())
    {
      policy = new HashMap<String,Object>();
      policy.put(KEY_TYPE, (String) this.get(KEY_TYPE));
      policy.put(KEY_PROTECTOR, (String) this.get(KEY_PROTECTOR));
      if(this.get(KEY_COLUMNS) != null)
        policy.put(KEY_COLUMNS, getValueAsList(this.get(KEY_COLUMNS)));
      else if(this.get(KEY_INDEXES) != null)
        policy.put(KEY_INDEXES, getValueAsList(this.get(KEY_INDEXES)));
      else
        policy.put(KEY_INDEXES, getValueAsList(this.get(KEY_INDICES)));
      
    }
    return policy;
  }
  
  /**
   * @param type {@link #TYPE_ALLOWLIST} or {@link #TYPE_DENYLIST}
   * @param qualifier the grants associated to the query
   * @throws YADAQueryConfigurationException if any parameter is {@code null}
   */
  @SuppressWarnings("unchecked")
  public void setAuthorizationPolicy(String type, List<?> qualifier) throws YADAQueryConfigurationException 
  {
    List<String> list = (List<String>) qualifier;
    if(null == type || null == qualifier || null == list || list.size() == 0)
      throw new YADAQueryConfigurationException("type and qualifier must be non-null");
//    this.put(KEY_POLICY, POLICY_AUTHORIZATION);
    this.put(KEY_TYPE, type);
    this.put(KEY_QUALIFIER, qualifier);    
  }
  
  /**
   * @return {@code true} if {@link #KEY_QUALIFIER} is present in the specification
   */
  public boolean hasAuthorizationPolicy()
  {
    return (this.get(KEY_QUALIFIER) != null);
  }
  
  /**
   * @return a {@link Map} containing the {@link #POLICY_AUTHORIZATION} specs
   */
  public Map<String,Object> getAuthorizationPolicy()  
  {
    Map<String,Object> policy = null;
    if(hasAuthorizationPolicy())
    {
      policy = new HashMap<String,Object>();
      policy.put(KEY_TYPE, (String) this.get(KEY_TYPE));
      policy.put(KEY_QUALIFIER, getValueAsList(this.get(KEY_QUALIFIER)));
    }
    return policy;
  }
  
  /**
   * @param type {@link #TYPE_ALLOWLIST} or {@link #TYPE_DENYLIST}
   * @param predicate the dynamic sql config associated to the query
   * @throws YADAQueryConfigurationException if any parameter is {@code null}
   */
  public void setContentPolicy(String type, String predicate) throws YADAQueryConfigurationException 
  {
    if(null == predicate)
      throw new YADAQueryConfigurationException("policy, type, protector, and columns, indexes, or indices must be non-null");
//    this.put(KEY_POLICY, POLICY_CONTENT);
    this.put(KEY_TYPE, type);
    this.put(KEY_PREDICATE, predicate);    
  }
  
  /**
   * @return {@code true} if {@link #KEY_PREDICATE} is present in the specification
   */
  public boolean hasContentPolicy()
  {
    return (this.get(KEY_PREDICATE) != null);
  }
  
  /**
   * @return a {@link Map} containing the {@link #POLICY_CONTENT} specs
   */
  public Map<String,String> getContentPolicy()  
  {
    Map<String,String> policy = null;
//    if(this.get(KEY_POLICY) == POLICY_CONTENT)
    if(hasContentPolicy())
    {
      policy = new HashMap<String,String>();
      policy.put(KEY_TYPE, (String) this.get(KEY_TYPE));
      policy.put(KEY_PREDICATE, (String) this.get(KEY_PREDICATE));
    }
    return policy;
  }
  
  /**
   * @param value a {@link JSONArray} mapped to {@link #KEY_QUALIFIER}, {@link #KEY_COLUMNS}, {@link #KEY_INDEXES}, or {@link #KEY_INDICES}
   * @return a {@link List} containing the entries associated to {@code value}
   */
  private List<?> getValueAsList(Object value) {
    if(null == value)
      return null;
    return Arrays.asList(((JSONArray)value).toString().split(","));
  }
}
