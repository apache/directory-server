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
package org.apache.directory.shared.ldap.schema.comparator;


import java.util.Comparator;

/**
 * A class for the BooleanComparator matchingRule (RFC 4517, par. 4.2.2)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class BooleanComparator implements Comparator<Object>
{
    /**
     * Implementation of the Compare method
     */
    public int compare( Object backendValue, Object assertValue ) 
    {
        // First, shortcut the process by comparing
        // references. If they are equals, then o1 and o2
        // reference the same object
        if ( backendValue == assertValue )
        {
            return 0;
        }
        
        // Then, deal with one of o1 or o2 being null
        // Both can't be null, because then they would 
        // have been catched by the previous test
        if ( ( backendValue == null ) || ( assertValue == null ) )
        {
            return ( backendValue == null ? -1 : 1 );
        }

        // Both object must be stored as String for boolean
        // values. If this is not the case, we have a pb...
        // However, the method will then throw a ClassCastException
        String b1 = (String)backendValue;

        // The boolean should have been stored as 'TRUE' or 'FALSE'
        // into the server, and the compare method will be called
        // with normalized booleans, so no need to uppercase them.
        // We don't need to check the assertion value, because we
        // are dealing with booleans.
        return ( b1.equals( "TRUE" ) ? 1 : -1 );
    }
}
