package com.novartis.opensource.yada.security;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.novartis.opensource.yada.server.YADAServer;

/**
 * @author dvaron
 * @since 10.2.0
 *
 */
public class IdentityCache extends ConcurrentHashMap<String, Object> {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * 
   */
  private static final long defaultTtl = 14400L;
  
  /**
   * 
   */
  private static final String YADA_IDENTITY_TTL = "YADA.identity.ttl";
  
  /**
   * 
   */
  private Map<String,Long> expiry = new ConcurrentHashMap<>();
    
  /**
   * Returns the identity object corresponding to key <em>only</em> 
   * if {@link #isActive(String)} is {@code true}.  If the identity is not active
   * it will be removed from the internal map and {@code null} will be returned.
   * @param key the token corresponding to the sought identity
   * @return the active identity or {@code null}
   */
  @Override
  public Object get(Object key) {
    if(key instanceof String && this.isActive((String)key))
    {
      return super.get(key);
    }
    else
    {
      super.remove(key);
      this.expiry.remove(key);
    }
    return null;
  }

  /**
   * Adds {@code value} mapped to {@code key} and additionally adds 
   * {@code current time + ttl} to {@link #expiry} 
   */
  @Override
  public Object put(String key, Object value) {
    Date   now     = new Date();
    String ttlProp = YADAServer.getProperties().getProperty(YADA_IDENTITY_TTL);
    long   ttl     = ttlProp != null ? Long.valueOf(ttlProp) : defaultTtl; 
    expiry.put(key, now.getTime() + ttl);
    return super.put(key, value);
  }
  
  /**
   * @param key
   * @return
   */
  private boolean isActive(String key) {
    Date now = new Date();
    return now.getTime() < this.getExpiry(key);
  }
  
  /**
   * @param key
   * @return
   */
  private long getExpiry(String key) {
    return expiry.get(key);
  }
  

}
