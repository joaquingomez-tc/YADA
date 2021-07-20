/**
 *
 */
package com.novartis.opensource.yada.security;

import com.novartis.opensource.yada.YADARequest;
import com.novartis.opensource.yada.server.YADAServer;

/**
 * @author Justin Finn
 * @since 8.7.6
 */

public interface Authorization {

  /**
   * Constant with value: {@value}
   */
  public final static String JWSKEY = "jws.key";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String JWTISS = "jwt.iss";

  // --------------------------------------------------------------------------------
  // TODO: Change these to system properties
  // --------------------------------------------------------------------------------

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_HDR_AUTH = "Authorization";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_HDR_SYNC_TKN = "X-CSRF-Token";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_HDR_AUTH_JWT_PREFIX = "Bearer";

  /**
   * Array of IAM headers we want to have access to
   */
  public final static String[] YADA_HDR_AUTH_NAMES = { YADA_HDR_AUTH, YADA_HDR_SYNC_TKN };

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_CK_TKN = "yadajwt";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_IDENTITY_SUB = "sub";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_IDENTITY_APP = "app";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_IDENTITY_ID = "identity";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_IDENTITY_KEY = "key";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_IDENTITY_KEYS = "keys";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_IDENTITY_TKN = "token";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_IDENTITY_GRANTS = "grants";

  /**
   * Constant with value: {@value}
   *
   */
  public final static String YADA_IDENTITY_IAT = "iat";

  /**
   * Constant equal to: {@value}
   */
  public final static String YADA_IDENTITY_CACHE = "identity";

  /**
   * Constant equal to: 14399
   */
  public final static Integer YADA_IDENTITY_TTL = 14399;

  /**
   * Constant equal to: {@value}
   */
  public final static String YADA_CREDENTIAL_CACHE = "credential";

  /**
   * Constant equal to: 14399
   */
  public final static Integer YADA_CREDENTIAL_TTL = 14399;

  /**
   * Constant equal to: {@value}
   */
  public final static String YADA_GRANT_CACHE = "grant";

  /**
   * Constant equal to: 1799
   */
  public final static Integer YADA_GRANT_TTL = 1799;

  /**
   * Constant equal to: {@value}
   */
  public final static String YADA_GROUP_CACHE = "groupList";

  /**
   * Constant equal to: 119
   */
  public final static Integer YADA_GROUP_TTL = 119;

  /**
   * Constant with value: {@value}
   *
   */
  public final static String AUTH_TYPE_WHITELIST = "whitelist";

  // --------------------------------------------------------------------------------

  /**
   * Constant equal to {@value}
   *
   */
  public final static String RX_HDR_AUTH_USR_PREFIX = "(Basic)(.+?)([A-Za-z0-9\\-\\._~\\+\\/]+=*)";

  /**
   * Constant equal to {@value}
   *
   */
  public final static String RX_HDR_AUTH_USR_CREDS = "(.+)[:=](.+)";

  /**
   * Constant equal to {@value} Formerly: (Bearer)(.+?)([a-zA-Z0-9-_.]{5,})
   *
   */
  public final static String RX_HDR_AUTH_TKN_PREFIX = "(Bearer)(.+?)([A-Za-z0-9\\-\\._~\\+\\/]+=*)";

  // --------------------------------------------------------------------------------
  // TODO: Make these YADA queries?
  // --------------------------------------------------------------------------------

  /**
   * Constant equal to {@value}. The query executed to evaluate authorization.
   */
  public final static String YADA_LOGIN_QUERY = "SELECT a.app \"APP\", a.userid \"USERID\", a.role \"ROLE\" "
      + "FROM yada_ug a JOIN yada_user b on a.userid = b.userid where b.userid = ? and b.pw = ? order by a.app";

  /**
   * Constant equal to {@value}. The query executed to evaluate authorization.
   */
  public final static String YADA_A11N_QUERY = "SELECT DISTINCT a.target, a.policy, a.type, a.qname "
      + "FROM YADA_A11N a " // join YADA_QUERY b on (a.target = b.qname OR
                            // a.target = b.app) "
      + "WHERE a.target = ?";

  // --------------------------------------------------------------------------------

  /**
   * Authorization of general use for given context
   *
   * @param payload a string to validate
   * @throws YADASecurityException when authorization fails for any reason, e.g., invalid credentials or token
   */
  public void authorize(String payload) throws YADASecurityException;

  /**
   * Authorization of query use for given context
   *
   * @throws YADASecurityException when authorization fails for any reason, e.g., invalid credentials or token
   */
  public void authorize() throws YADASecurityException;

  /**
   * Confirm token is valid and user possesses necessary grants. Intended for use in a postprocessor plugin.
   *
   * @see Authorizer
   * @param yadaReq the {@link YADARequest} containing the headers to validate
   * @param result the default auth query result, e.g., {@code 401 Unauthorized}
   * @throws YADASecurityException when validation fails for any reason, e.g., invalid credentials or token
   */
  public void authorizeYADARequest(YADARequest yadaReq, String result) throws YADASecurityException;

  /**
   * Write to the IAM cache. This default implementation of this method signature does nothing.
   * It could be overridden to support an external or third party cache.
   * Use the {@link #setCacheEntry(String, Object)} signature with native {@link IdentityCache} cache.
   *
   * @param cache the name of the cache
   * @param key the cache entry name
   * @param cacheValue the cache entry value
   * @param ttl the time-to-live of the entry
   */
  public default void setCacheEntry(String cache, String key, Object cacheValue, Integer ttl) {  }

  /**
   * Write to the native {@link IdentityCache} IAM cache.
   * @param key the cache entry name
   * @param cacheValue the cache entry value
   * @since 10.2.0
   */
  public default void setCacheEntry(String key, Object cacheValue) {
    YADAServer.getIdentityCache().put(key, cacheValue);
  }

  /**
   * Read the IAM cache.  This default implementation of this method signature does nothing
   * and returns null.  It could be overridden to support an external or third party cache.
   * Use the {@link #getCacheEntry(String)} signature with native {@link IdentityCache} cache.
   *
   * @param cache the name of the cache
   * @param key the name of the entry to retrieve
   * @return the stored string
   */
  public default Object getCacheEntry(String cache, String key) {
    return null;
  }

  /**
   * Read the native {@link IdentityCache} IAM cache
   * @param key the name of the entry to retrieve
   * @return the stored object
   * @since 10.2.0
   */
  public default Object getCacheEntry(String key) {
    return YADAServer.getIdentityCache().get(key);
  }

}
