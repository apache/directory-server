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

package org.apache.directory.server.kerberos.shared.store.operations;


import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.protocol.shared.store.ContextOperation;


/**
 * Command for deleting a principal from a JNDI context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DeletePrincipal implements ContextOperation
{
    private static final long serialVersionUID = -6970986279811261983L;

    /** The Kerberos principal who is to be deleted. */
    protected KerberosPrincipal principal;


    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     */
    public DeletePrincipal(KerberosPrincipal principal)
    {
        this.principal = principal;
    }


    public Object execute( DirContext ctx, Name searchBaseDn )
    {
        if ( principal == null )
        {
            return null;
        }

        String dn = null;

        try
        {
            dn = search( ctx, searchBaseDn, principal.getName() );
            Name rdn = getRelativeName( ctx, dn );
            ctx.destroySubcontext( rdn );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            return null;
        }

        return dn;
    }


    private String search( DirContext ctx, Name searchBaseDn, String principal ) throws NamingException
    {
        String[] attrIDs =
            { KerberosAttribute.PRINCIPAL, KerberosAttribute.VERSION, KerberosAttribute.TYPE, KerberosAttribute.KEY };

        Attributes matchAttrs = new BasicAttributes( true );
        matchAttrs.put( new BasicAttribute( KerberosAttribute.PRINCIPAL, principal ) );

        // Search for objects that have those matching attributes
        NamingEnumeration answer = ctx.search( searchBaseDn, matchAttrs, attrIDs );

        if ( answer.hasMore() )
        {
            SearchResult sr = ( SearchResult ) answer.next();
            if ( sr != null )
            {
                return sr.getName();
            }
        }

        return null;
    }


    private Name getRelativeName( DirContext ctx, String baseDn ) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty( "jndi.syntax.direction", "right_to_left" );
        props.setProperty( "jndi.syntax.separator", "," );
        props.setProperty( "jndi.syntax.ignorecase", "true" );
        props.setProperty( "jndi.syntax.trimblanks", "true" );

        Name searchBaseDn;

        try
        {
            Name ctxRoot = new CompoundName( ctx.getNameInNamespace(), props );
            searchBaseDn = new CompoundName( baseDn, props );

            if ( !searchBaseDn.startsWith( ctxRoot ) )
            {
                throw new NamingException( "Invalid search base " + baseDn );
            }

            for ( int ii = 0; ii < ctxRoot.size(); ii++ )
            {
                searchBaseDn.remove( 0 );
            }
        }
        catch ( NamingException e )
        {
            throw new NamingException( "Failed to initialize search base " + baseDn );
        }

        return searchBaseDn;
    }
}
