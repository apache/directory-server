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
 * A comparator for UUID. We simply use the UUID compareTo method.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UUIDComparator implements Comparator<byte[]>
{
    /** A static instance of this comparator */
    public static final Comparator<byte[]> INSTANCE = new UUIDComparator();
    
    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( byte[] uuid1, byte[] uuid2 )
    {
        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------
        if ( uuid1 == null )
        {
            return ( uuid2 == null ) ? 0 : -1;
        }
        
        if ( uuid2 == null )
        {
            return 1;
        }
        
        if ( uuid1.length < uuid2.length )
        {
            return -1;
        }
        else if ( uuid1.length > uuid2.length )
        { 
            return 1;
        }
        
        for ( int pos = 0; pos < uuid1.length; pos++ )
        {
            if ( uuid1[pos] == uuid2[pos ] )
            {
                continue;
            }
            
            if ( uuid1[pos] < uuid2[pos] )
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
        
        return 0;
    }
}
