/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.trigger;


/**
 * An enumeration that represents all standard LDAP operations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class LdapOperation
{
    public static final LdapOperation BIND = new LdapOperation( "bind" );

    public static final LdapOperation UNBIND = new LdapOperation( "unbind" );

    public static final LdapOperation SEARCH = new LdapOperation( "search" );

    public static final LdapOperation MODIFY = new LdapOperation( "modify" );

    public static final LdapOperation ADD = new LdapOperation( "add" );

    public static final LdapOperation DEL = new LdapOperation( "del" );

    public static final LdapOperation MODDN = new LdapOperation( "moddn" );

    public static final LdapOperation COMPARE = new LdapOperation( "compare" );
    
    public static final LdapOperation ABANDON = new LdapOperation( "abandon" );
    
    public static final LdapOperation EXTENDED = new LdapOperation( "extended" );

    
    private final String name;


    private LdapOperation( String name )
    {
        this.name = name;
    }


    /**
     * Returns the name of this LDAP operation.
     */
    public String getName()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }
    
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        
        if ( o instanceof LdapOperation )
        {
            LdapOperation operation = ( LdapOperation ) o;
            return operation.getName().equals( getName() );
            
        }
        else
        {
            return false;
        }
    }
}
