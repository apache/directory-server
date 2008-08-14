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
package org.apache.directory.server.core.entry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.directory.server.schema.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdbm.helper.Serializer;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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

    /** The registries reference */
    private transient Registries registries;


    /**
     * Creates a new instance of ServerEntrySerializer.
     *
     * @param registries The reference to the global registries
     */
    public ServerEntrySerializer( Registries registries )
    {
        this.registries = registries;
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
     * <li><b>[DN length]</b> : can be -1 if we don't have a DN, 0 if the 
     * DN is empty, otherwise contains the DN's length.<p> 
     * <b>NOTE :</b>This should be unnecessary, as the DN should always exists
     * <p>
     * </li>
     * <li>
     * <b>DN</b> : The entry's DN. Can be empty (rootDSE)<p>
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
        DefaultServerEntry entry = ( DefaultServerEntry ) object;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        entry.serialize( out );

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
     *  Deserialize a ServerEntry
     */
    public Object deserialize( byte[] bytes ) throws IOException
    {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bytes ) );

        DefaultServerEntry serverEntry = new DefaultServerEntry( registries );
        
        try
        {
            serverEntry.deserialize( in );
            
            return serverEntry;
        }
        catch ( ClassNotFoundException cnfe )
        {
            LOG.error( "Cannot deserialize the entry :" + cnfe.getMessage() );
            return null;
        }
    }
}
