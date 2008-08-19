/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */


package org.apache.directory.server.core.sp;


/**
 * A utility class for working with Stored Procedures.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class StoredProcUtils
{
    
    /** The delimiter used to tokenize a full SP name into the unit and SP name */
    public static final String SPUnitDelimiter = ":";

    public static String extractStoredProcName( String fullSPName )
    {
        int delimiter = fullSPName.lastIndexOf( SPUnitDelimiter );
        String spName = fullSPName.substring( delimiter + SPUnitDelimiter.length() );
        return spName;
    }
    
    public static String extractStoredProcUnitName( String fullSPName )
    {
        int delimiter = fullSPName.lastIndexOf( SPUnitDelimiter );
        String className = fullSPName.substring( 0, delimiter );
        return className;
    }
    
}
