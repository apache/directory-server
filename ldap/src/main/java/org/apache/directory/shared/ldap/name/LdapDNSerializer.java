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
package org.apache.directory.shared.ldap.name;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class which serialize and deserialize a DN
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapDNSerializer
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( LdapDNSerializer.class );

    /**
     * Serialize a DN
     * 
     * We have to store a DN data efficiently. Here is the structure :
     * 
     * <li>upName</li> The User provided DN<p>
     * <li>normName</li> May be null if the normName is equivalent to 
     * the upName<p>
     * <li>rdns</li> The rdn's List.<p>
     * 
     * for each rdn :
     * <li>call the RDN write method</li>
     * 
     * @param dn The DN to serialize
     * @param out the stream in which the DN will be serialized
     * @throws IOException If we can't write in this stream
     */
    public static void serialize( LdapDN dn, ObjectOutput out ) throws IOException
    {
        if ( dn.getUpName() == null )
        {
            String message = "Cannot serialize a NULL DN";
            LOG.error( message );
            throw new IOException( message );
        }
        
        // Write the UPName
        out.writeUTF( dn.getUpName() );
        
        // Write the NormName if different
        if ( dn.isNormalized() )
        {
            if ( dn.getUpName().equals( dn.getNormName() ) )
            {
                out.writeUTF( "" );
            }
            else
            {
                out.writeUTF( dn.getNormName() );
            }
        }
        else
        {
            String message = "The DN should have been normalized before being serialized " + dn;
            LOG.error( message );
            throw new IOException( message );
        }
        
        // Should we store the byte[] ???
        
        // Write the RDNs.
        // First the number of RDNs
        out.writeInt( dn.size() );
        
        // Loop on the RDNs
        for ( Rdn rdn:dn.getRdns() )
        {
            RdnSerializer.serialize( rdn, out );
        }
    }


    /**
     * Deserialize a DN
     * 
     * We read back the data to create a new LdapDN. The structure 
     * read is exposed in the {@link LdapDNSerializer#serialize(LdapDN, ObjectOutput)} 
     * method<p>
     * 
     * @param in The input stream from which the DN is read
     * @return a deserialized DN
     * @throws IOException If the stream can't be read
     */
    public static LdapDN deserialize( ObjectInput in ) throws IOException
    {
        // Read the UPName
        String upName = in.readUTF();
        
        // Read the NormName
        String normName = in.readUTF();
        
        if ( normName.length() == 0 )
        {
            // As the normName is equal to the upName,
            // we didn't saved the nbnormName on disk.
            // restore it by copying the upName.
            normName = upName;
        }
        
        // Should we read the byte[] ???
        byte[] bytes = StringTools.getBytesUtf8( upName );
        
        // Read the RDNs. Is it's null, the number will be -1.
        int nbRdns = in.readInt();
        
        LdapDN dn = new LdapDN( upName, normName, bytes );
        
        for ( int i = 0; i < nbRdns; i++ )
        {
            Rdn rdn = RdnSerializer.deserialize( in );
            dn.add( 0, rdn );
        }
    
        return dn;
    }
}
