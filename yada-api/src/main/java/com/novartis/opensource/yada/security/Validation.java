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
package com.novartis.opensource.yada.security;

/**
 * @author Dave Varon
 * @since 7.0.0
 */
public interface Validation
{
  
  /**
   * @throws YADASecurityException when the user is unauthorized or there is an error in policy processing
   */
  public void validateYADARequest() throws YADASecurityException;
  
  /**
   * @throws YADASecurityException when the user is unauthorized or there is an error in regex syntax
   */
  public void validateURL() throws YADASecurityException;
  
  /**
   * @throws YADASecurityException when the user is unauthorized or there is an error in token processing
   */
  public void validateToken() throws YADASecurityException;
 
  
  /**
   * @param securityPolicy the policy object
   * @throws YADASecurityException when the user is unauthorized or there is an error in policy processing
   */
  public void applyPolicy(SecurityPolicy securityPolicy) throws YADASecurityException;
}