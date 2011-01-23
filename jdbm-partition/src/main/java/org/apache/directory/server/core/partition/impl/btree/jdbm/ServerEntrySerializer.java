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
package org.apache.directory.server.core.partition.impl.btree.jdbm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jdbm.helper.Serializer;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServerEntrySerializer implements Serializer
{
    private static final long serialVersionUID = 1L;

    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ServerEntrySerializer.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The schemaManager reference */
    private transient SchemaManager schemaManager;


    /**
     * Creates a new instance of ServerEntrySerializer.
     *
     * @param schemaManager The reference to the global schemaManager
     */
    public ServerEntrySerializer( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /**
     * <p>
     * 
     * This is the place where we serialize entries, and all theirs
     * elements. the reason why we don't call the underlying methods
     * (<code>ServerAttribute.write(), Value.write()</code>) is that we need
     * access to the registries to read back the values.
     * <p>
     * The structure used to store the entry is the following :
     * <li><b>[Dn length]</b> : can be -1 if we don't have a Dn, 0 if the
     * Dn is empty, otherwise contains the Dn's length.<p>
     * <b>NOTE :</b>This should be unnecessary, as the Dn should always exists
     * <p>
     * </li>
     * <li>
     * <b>Dn</b> : The entry's Dn. Can be empty (rootDSE)<p>
     * </li>
     * <li>
     * <b>[nb attributes]</b> The number of attributes
     * </li>
     * <br>
     * For each attribute :
     * <li>
     * <b>[upId]</b> The attribute user provided ID (it can't be null)
     * </li>
     * <li>
     * <b>[nb values]</b> The number of values
     * </li>
     * <br>
     * For each value :
     * <li>
     *  <b>[is valid]</b> if the value is valid
     * </li>
     * <li>
     *  <b>[HR flag]</b> if the value is a String
     * </li>
     * <li>
     *  <b>[Streamed flag]</b> if the value is streamed
     * </li>
     * <li>
     *  <b>[UP value]</b> the user provided value
     * </li>
     * <li>
     *  <b>[Norm value]</b> (will be null if normValue == upValue)
     * </li>
     */
    public byte[] serialize( Object object ) throws IOException
    {
        Entry entry = ( DefaultEntry ) object;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        ((DefaultEntry)entry).serialize( out );

        // Note : we don't store the ObjectClassAttribute. I has already
        // been stored as an attribute.

        out.flush();

        if ( IS_DEBUG )
        {
            LOG.debug( ">------------------------------------------------" );
            LOG.debug( "Serialize " + entry );
        }

        return baos.toByteArray();
    }

    
    /**
     *  Deserialize a Entry.
     *  
     *  @param bytes the byte array containing the serialized entry
     *  @return An instance of a Entry object 
     *  @throws IOException if we can't deserialize the Entry
     */
    public Object deserialize( byte[] bytes ) throws IOException
    {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bytes ) );

        Entry serverEntry = new DefaultEntry( schemaManager );
        
        try
        {
            ((DefaultEntry)serverEntry).deserialize( in );
            
            return serverEntry;
        }
        catch ( ClassNotFoundException cnfe )
        {
            LOG.error( I18n.err( I18n.ERR_134, cnfe.getLocalizedMessage() ) );
            return null;
        }
    }
}
