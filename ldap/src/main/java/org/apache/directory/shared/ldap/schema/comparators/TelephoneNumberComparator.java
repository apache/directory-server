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
package org.apache.directory.shared.ldap.schema.comparators;


import java.util.Comparator;


/**
 * A comparator for TelephoneNumber.
 * 
 * The rules for matching are identical to those for caseIgnoreMatch, except that 
 * all space and "-" characters are skipped during the comparison. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class TelephoneNumberComparator implements Comparator<String>
{
    /** A static instance of this comparator */
    public static final Comparator<String> INSTANCE = new TelephoneNumberComparator();
    
    
    /**
     * Remove all spaces and '-' from the telephone number
     */
    private String strip( String telephoneNumber )
    {
        char[] telephoneNumberArray = telephoneNumber.toCharArray();
        int pos = 0;
        
        for ( char c:telephoneNumberArray )
        {
            if ( ( c == ' ' ) || ( c == '-' ) )
            { 
                continue;
            }
            
            telephoneNumberArray[pos++] = c;
        }
        
        return new String( telephoneNumberArray, 0, pos );
    }

    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( String telephoneNumber1, String telephoneNumber2 )
    {
        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------

        if ( telephoneNumber1 == null )
        {
            return ( telephoneNumber2 == null ) ? 0 : -1;
        }
        
        if ( telephoneNumber2 == null )
        {
            return 1;
        }
        
        // -------------------------------------------------------------------
        // Remove all spaces and '-'
        // -------------------------------------------------------------------
        telephoneNumber1 = strip( telephoneNumber1 );
        telephoneNumber2 = strip( telephoneNumber2 );
        
        return ( telephoneNumber1.compareToIgnoreCase( telephoneNumber2 ) );
    }
}
