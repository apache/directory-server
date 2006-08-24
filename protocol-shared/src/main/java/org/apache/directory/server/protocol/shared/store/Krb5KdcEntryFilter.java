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
package org.apache.directory.server.protocol.shared.store;


import java.io.File;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Filter which generates kerberos keys from userPassword attributes of kerberos users being
 * loaded into the server from an LDIF file.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Krb5KdcEntryFilter implements LdifLoadFilter
{
    private static final Logger log = LoggerFactory.getLogger( Krb5KdcEntryFilter.class );
    private static final String KEY_TYPE = "DES";
    private static final String OBJECTCLASS_ATTR = "objectClass";
    private static final String KRB5KDCENTRY_OC = "krb5KDCEntry";
    private static final String PASSWORD_ATTR = "userPassword";


    /**
     * Always accepts entries whether or not it can sucessfully generate a key for the entry.
     *
     * @see  LdifLoadFilter#filter(File, String, Attributes, DirContext)
     */
    public boolean filter( File file, String dn, Attributes entry, DirContext ctx ) throws NamingException
    {
        if ( entry.get( OBJECTCLASS_ATTR ).contains( KRB5KDCENTRY_OC ) )
        {
            String krbPrincipal = null;
            try
            {
                String pw = ( String ) entry.get( PASSWORD_ATTR ).get();
                krbPrincipal = ( String ) entry.get( KerberosAttribute.PRINCIPAL ).get();
                KerberosPrincipal principal = new KerberosPrincipal( krbPrincipal );
                KerberosKey key = new KerberosKey( principal, pw.toCharArray(), KEY_TYPE );

                byte[] encodedKey = key.getEncoded();
                entry.put( KerberosAttribute.KEY, encodedKey );
                entry.put( KerberosAttribute.VERSION, Integer.toString( key.getVersionNumber() ) );
                entry.put( KerberosAttribute.TYPE, Integer.toString( key.getKeyType() ) );
            }
            catch ( Exception e )
            {
                log.warn( "failed to generate kerberos key\n\tkrbPrincipal=" + krbPrincipal + "\n\tdn=" + dn
                    + "\n\tentry=\n" + entry );
            }
        }

        return true;
    }
}
