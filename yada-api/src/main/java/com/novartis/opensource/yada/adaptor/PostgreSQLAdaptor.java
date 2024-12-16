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
package com.novartis.opensource.yada.adaptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novartis.opensource.yada.YADARequest;

/**
 * For connecting to PostgreSQL databases via JDBC.
 * 
 * @author David Varon
 * @since 4.1.0
 */
public class PostgreSQLAdaptor extends JDBCAdaptor {
	
	/**
   * Local logger handle
   */
	private static final Logger LOG = LoggerFactory.getLogger(PostgreSQLAdaptor.class);
	
	/**
	 * Default subclass constructor (calls {@code super()}
	 */
	public PostgreSQLAdaptor() {
		super();
		LOG.debug("Initializing");
	}
	
	/**
	 * Subclass constructor, calls {@code super(yadaReq)}
	 * @param yadaReq YADA request configuration
	 */
	public PostgreSQLAdaptor(YADARequest yadaReq)
	{
		super(yadaReq);
	}
    
	/**
	 * Enables checking for {@link JDBCAdaptor#ORACLE_DATE_FMT} if {@code val} does not conform to {@link JDBCAdaptor#STANDARD_DATE_FMT}
	 * @since 5.1.1
	 */
	@Override
	protected void setDateParameter(PreparedStatement pstmt, int index, char type, String val) throws SQLException 
  {
    if (EMPTY.equals(val) || val == null)
    {
      pstmt.setNull(index, java.sql.Types.DATE);
    }
    else
    {
      SimpleDateFormat sdf     = new SimpleDateFormat(STANDARD_DATE_FMT);
      ParsePosition    pp      = new ParsePosition(0);
      Date             dateVal = sdf.parse(val,pp);
      if(dateVal == null)
      {
        sdf     = new SimpleDateFormat(ORACLE_DATE_FMT);
        pp      = new ParsePosition(0);
        dateVal = sdf.parse(val,pp);
      }
      if (dateVal != null)
      {
        long t = dateVal.getTime();
        java.sql.Date sqlDateVal = new java.sql.Date(t);
        pstmt.setDate(index, sqlDateVal);
      }
    }
  }
}
