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

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.store.KerberosAttribute;
import org.apache.kerberos.store.PrincipalStoreEntryModifier;

/**
 * An ObjectFactory that resusitates objects from directory attributes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PrincipalObjectFactory implements DirObjectFactory
{
    public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment, Attributes attrs ) throws Exception
    {
        if ( attrs == null || attrs.get( "objectClass" ) == null || ! attrs.get( "objectClass" ).contains( "krb5KDCEntry" ) )
        {
            return null;
        }
        
        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();
        
        modifier.setUserId( ( String ) attrs.get( "uid" ).get() );
        modifier.setCommonName( ( String ) attrs.get( "cn" ).get() );
        
        KerberosPrincipal principal = new KerberosPrincipal( ( String ) attrs.get( KerberosAttribute.PRINCIPAL ).get() );
        modifier.setPrincipal( principal );
        
        modifier.setKey( ( byte[] ) attrs.get( KerberosAttribute.KEY ).get() );
        modifier.setEncryptionType( Integer.parseInt( ( String ) attrs.get( KerberosAttribute.TYPE ).get() ) );
        modifier.setKeyVersionNumber( Integer.parseInt( ( String ) attrs.get( KerberosAttribute.VERSION ).get() ) );
        
        return modifier.getEntry();
    }
    
    public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment ) throws Exception
    {
        throw new UnsupportedOperationException( "Attributes are required to add an entry." );
    }
}

