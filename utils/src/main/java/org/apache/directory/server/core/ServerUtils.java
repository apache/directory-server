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
package org.apache.directory.server.core;


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * A central place to aggregate functionality used throughout the server
 * in various subsystems yet the code here is server specific hence why 
 * it's not placed in shared-ldap.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerUtils
{
    /**
     * Efficient server side means to perform a lookup of an attribute
     * within an entry.  Simple lookups by name may fail if the user provided 
     * the attribute identifier in any way other than the common alias for 
     * the attribute.  We need to check for all aliases and the OID.
     * 
     * @param type the attributeType containing the OID and aliases we need
     * @param entry the entry with user provided attribute IDENTIFIERS 
     * @return the attribute if it is present
     * @throws NamingException if there are failures while accessing the 
     * attributes object
     */
    public static Attribute getAttribute( AttributeType type, Attributes entry ) throws NamingException
    {
        Attribute attr = entry.get( type.getOid() );
        if ( attr == null )
        {
            String[] aliases = type.getNames();
            for ( int ii = 0; ii < aliases.length; ii++ )
            {
                attr = entry.get( aliases[ii] );
                if ( attr != null )
                {
                    return attr;
                }
            }
        }
        return attr;
    }


    public static Attribute removeAttribute( AttributeType type, Attributes entry ) throws NamingException
    {
        Attribute attr = entry.get( type.getOid() );
        if ( attr == null )
        {
            String[] aliases = type.getNames();
            for ( int ii = 0; ii < aliases.length; ii++ )
            {
                attr = entry.get( aliases[ii] );
                if ( attr != null )
                {
                    return entry.remove( attr.getID() );
                }
            }
        }
        
        if ( attr == null )
        {
            return null;
        }
        
        return entry.remove( attr.getID() );
    }
}
