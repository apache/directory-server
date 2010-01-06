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
package org.apache.directory.shared.ldap.schema.normalizers;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * Normalizer a DN
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DnNormalizer extends Normalizer
{
    // The serial UID
    private static final long serialVersionUID = 1L;
    
    /** A reference to the schema manager used to normalize the DN */
    private SchemaManager schemaManager;
    
    /**
     * Empty constructor
     */
    public DnNormalizer()
    {
        super( SchemaConstants.DISTINGUISHED_NAME_MATCH_MR_OID );
    }


    /**
     * {@inheritDoc}
     */
    public Value<?> normalize( Value<?> value ) throws NamingException
    {
        LdapDN dn = null;
        
        String dnStr = value.getString();
        
        dn = new LdapDN( dnStr );
        
        dn.normalize( schemaManager.getNormalizerMapping() );
        return new ClientStringValue( dn.getNormName() );
    }


    /**
     * {@inheritDoc}
     */
    public String normalize( String value ) throws NamingException
    {
        LdapDN dn = null;
        
        dn = new LdapDN( value );
        
        dn.normalize( schemaManager.getNormalizerMapping() );
        return dn.getNormName();
    }


    /**
     * Normalize a DN
     * @param value The DN to normalize
     * @return A normalized DN
     * @throws NamingException
     */
    public String normalize( LdapDN value ) throws NamingException
    {
        LdapDN dn = null;
        
        dn = new LdapDN( value );
        
        dn.normalize( schemaManager.getNormalizerMapping() );
        return dn.getNormName();
    }


    /**
     * {@inheritDoc}
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }
}
