/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.kerberos.store.operations;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.store.KerberosAttribute;
import org.apache.protocol.common.store.ContextOperation;

/**
 * Command for changing a principal's password in a JNDI context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePassword implements ContextOperation
{
    private static final long serialVersionUID = -7147685183641418353L;

    /** The Kerberos principal who's password is to be changed. */
    protected KerberosPrincipal principal;
    /** The new key for the update. */
    protected KerberosKey newKey;

    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     */
    public ChangePassword( KerberosPrincipal principal, KerberosKey newKey )
    {
        this.principal = principal;
        this.newKey = newKey;
    }

    public Object execute( DirContext ctx, Name searchBaseDn )
    {
        if ( principal == null )
        {
            return null;
        }

        ModificationItem[] mods = new ModificationItem[ 1 ];
        Attribute newKeyAttribute = new BasicAttribute( "krb5key", newKey.getEncoded() );
        mods[ 0 ] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, newKeyAttribute );

        String dn = null;

        try
        {
            dn = search( ctx, principal.getName() );
            Name rdn = getRelativeName( ctx.getNameInNamespace(), dn );
            ctx.modifyAttributes( rdn, mods );
        }
        catch ( NamingException e )
        {
            return null;
        }

        return dn;
    }

    private String search( DirContext ctx, String principal ) throws NamingException
    {
        String[] attrIDs = { KerberosAttribute.PRINCIPAL, KerberosAttribute.VERSION, KerberosAttribute.TYPE,
                KerberosAttribute.KEY };

        Attributes matchAttrs = new BasicAttributes( false ); // case-sensitive
        matchAttrs.put( new BasicAttribute( KerberosAttribute.PRINCIPAL, principal ) );

        NamingEnumeration answer = ctx.search( "", matchAttrs, attrIDs );

        if ( answer.hasMore() )
        {
            SearchResult sr = (SearchResult) answer.next();
            if ( sr != null )
            {
                return sr.getName();
            }
        }

        return null;
    }

    private Name getRelativeName( String nameInNamespace, String baseDn ) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty( "jndi.syntax.direction", "right_to_left" );
        props.setProperty( "jndi.syntax.separator", "," );
        props.setProperty( "jndi.syntax.ignorecase", "true" );
        props.setProperty( "jndi.syntax.trimblanks", "true" );

        Name searchBaseDn = null;

        Name ctxRoot = new CompoundName( nameInNamespace, props );
        searchBaseDn = new CompoundName( baseDn, props );

        if ( !searchBaseDn.startsWith( ctxRoot ) )
        {
            throw new NamingException( "Invalid search base " + baseDn );
        }

        for ( int ii = 0; ii < ctxRoot.size(); ii++ )
        {
            searchBaseDn.remove( 0 );
        }

        return searchBaseDn;
    }
}
