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
package org.apache.directory.server.schema.bootstrap;


import java.io.Serializable;
import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A comparator that sorts OIDs based on their numeric id value.  Needs a 
 * OidRegistry to properly do it's job.  Public method to set the oid 
 * registry will be used by the server after instantiation in deserialization.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameOrNumericIdComparator implements Comparator, Serializable
{
    private static final long serialVersionUID = 1L;
    private transient OidRegistry registry;

    
    public NameOrNumericIdComparator( OidRegistry registry )
    {
        this.registry = registry;
    }
    
    
    public NameOrNumericIdComparator()
    {
    }
    
    
    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object o1, Object o2 )
    {
        String s1 = getNumericIdString( o1 );
        String s2 = getNumericIdString( o2 );

        if ( s1 == null && s2 == null )
        {
            return 0;
        }
        
        if ( s1 == null )
        {
            return -1;
        }
        
        if ( s2 == null )
        {
            return 1;
        }
        
        return s1.compareTo( s2 );
    }
    
    
    public void setRegistries( Registries registries )
    {
        registry = registries.getOidRegistry();
    }
    
    
    String getNumericIdString( Object obj )
    {
        String strValue;

        if ( obj == null )
        {
            return null;
        }
        
        if ( obj instanceof String )
        {
            strValue = ( String ) obj;
        }
        else if ( obj instanceof byte[] )
        {
            strValue = StringTools.utf8ToString( ( byte[] ) obj ); 
        }
        else
        {
            strValue = obj.toString();
        }
        
        if ( strValue.length() == 0 )
        {
            return "";
        }

        if ( registry.hasOid( strValue ) )
        {
            try
            {
                return registry.getOid( strValue );
            }
            catch ( NamingException e )
            {
                e.printStackTrace();
                throw new RuntimeException( "Failed to lookup OID for " + strValue, e );
            }
        }
        
        return strValue;
    }
}
