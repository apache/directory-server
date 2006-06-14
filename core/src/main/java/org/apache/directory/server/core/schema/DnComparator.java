/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.schema;


import java.util.Comparator;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DnComparator implements Comparator
{
    // @TODO you'll need this to fix the way normalization is done
    private final AttributeTypeRegistry attrRegistry;
    
    
    public DnComparator( AttributeTypeRegistry attrRegistry )
    {
        this.attrRegistry = attrRegistry;
    }
    
    
    public int compare( Object obj0, Object obj1 ) 
    {
        LdapDN dn0 = null;
        LdapDN dn1 = null;
        
        try 
        {
            dn0 = getDn( obj0 );
            dn1 = getDn( obj1 );
        }
        catch ( NamingException e )
        {
            // -- what do we do here ?
        }
        
        return dn0.compareTo( dn1 );
    }


    public LdapDN getDn( Object obj ) throws NamingException
    {
        LdapDN dn = null;
        if ( obj instanceof LdapDN )
        {
            dn = LdapDN.normalize( ( LdapDN ) obj );
        }
        else if ( obj instanceof Name )
        {
            dn = new LdapDN( ( Name ) obj );
            dn.normalize();
        }
        else if ( obj instanceof String )
        {
            dn = new LdapDN( ( String ) obj );
            dn.normalize();
        }
        else
        {
            throw new IllegalStateException( "I do not know how to handle dn comparisons with objects of class: " 
                + obj.getClass() );
        }
        return dn;
    }
}
