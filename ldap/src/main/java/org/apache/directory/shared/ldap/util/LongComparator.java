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
package org.apache.directory.shared.ldap.util;

import java.io.Serializable;
import java.util.Comparator;


/**
 * Compares Long keys and values within a table.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 437007 $
 */
public class LongComparator implements Comparator<Object>, Serializable
{
    /** A instance of this comparator */
    public static final LongComparator INSTANCE = new LongComparator();

    /**
     * Version id for serialization.
     */
    static final long serialVersionUID = 1L;


    /**
     * Compare two objects.
     * 
     * @param obj1 First object
     * @param obj2 Second object
     * @return 1 if obj1 > obj2, 0 if obj1 == obj2, -1 if obj1 < obj2
     */
    public int compare( Object obj1, Object obj2 )
    {
    	try
    	{
            Long long1 = (Long)obj1;
            Long long2 = (Long)obj2;
            return long1 < long2 ? -1 : long1 == long2 ? 0 : -1 ;
    	}
    	catch ( NullPointerException npe )
    	{
	        if ( obj1 == null )
	        {
	            throw new IllegalArgumentException( "Argument 'obj1' is null" );
	        }
	        else
	        {
	            throw new IllegalArgumentException( "Argument 'obj2' is null" );
	        }
    	}
    }
}
