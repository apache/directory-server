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
package org.apache.ldap.server.schema.bootstrap;


import java.util.HashMap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.server.jndi.ServerDirObjectFactory;
import org.apache.ldap.server.schema.ObjectFactoryRegistry;
import org.apache.ldap.server.schema.OidRegistry;


/**
 * A boostrap service implementation for an ObjectFactoryRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapObjectFactoryRegistry implements ObjectFactoryRegistry
{
    /** Used to lookup a state factory by objectClass id */
    private final HashMap byOid = new HashMap();

    /** The oid registry used to get numeric ids */
    private final OidRegistry oidRegistry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an ObjectFactoryRegistry that looks up an object factory to use.
     *
     * @param oidRegistry an object identifier registry
     */
    public BootstrapObjectFactoryRegistry( OidRegistry oidRegistry )
    {
        this.oidRegistry = oidRegistry;
    }


    public ServerDirObjectFactory getObjectFactories( LdapContext ctx ) throws NamingException
    {
        Attribute objectClass = ctx.getAttributes( "" ).get( "objectClass" );

        if ( objectClass == null )
        {
            return null;
        }

        if ( ctx.getEnvironment().containsKey( "factory.hint" ) )
        {
            String oid = ( String ) ctx.getEnvironment().get( "factory.hint" );

            String noid = oidRegistry.getOid( oid );

            if ( byOid.containsKey( noid ) )
            {
                return ( ServerDirObjectFactory ) byOid.get( noid );
            }
        }

        // hint did not work or was not provided so we return what we find first

        for ( int ii = 0; ii < objectClass.size(); ii++ )
        {
            String noid = oidRegistry.getOid( ( String ) objectClass.get( ii ) );
            if ( byOid.containsKey( noid ) )
            {
                return ( ServerDirObjectFactory ) byOid.get( noid );
            }
        }

        return null;
    }


    public void register( ServerDirObjectFactory factory ) throws NamingException
    {
        byOid.put( oidRegistry.getOid( factory.getObjectClassId() ), factory );
    }
}
