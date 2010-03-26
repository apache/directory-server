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
package org.apache.directory.server.xdbm.search.impl;


import java.util.Iterator;

import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * An Evaluator which determines if candidates are matched by GreaterEqNode
 * assertions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PresenceEvaluator<ID> implements Evaluator<PresenceNode, ServerEntry, ID>
{
    private final PresenceNode node;
    private final Store<ServerEntry, ID> db;
    private final AttributeType type;
    private final SchemaManager schemaManager;
    private final Index<String, ServerEntry, ID> idx;


    public PresenceEvaluator( PresenceNode node, Store<ServerEntry, ID> db, SchemaManager schemaManager )
        throws Exception
    {
        this.db = db;
        this.node = node;
        this.schemaManager = schemaManager;
        this.type = schemaManager.lookupAttributeTypeRegistry( node.getAttribute() );

        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            idx = db.getPresenceIndex();
        }
        else
        {
            idx = null;
        }
    }


    public PresenceNode getExpression()
    {
        return node;
    }


    public AttributeType getAttributeType()
    {
        return type;
    }


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    public boolean evaluate( IndexEntry<?, ServerEntry, ID> indexEntry ) throws Exception
    {
        if ( idx != null )
        {
            return idx.forward( type.getOid(), indexEntry.getId() );
        }

        ServerEntry entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        return evaluateEntry( entry );
    }


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    public boolean evaluateId( ID id ) throws Exception
    {
        if ( idx != null )
        {
            return idx.forward( type.getOid(), id );
        }

        return evaluateEntry( db.lookup( id ) );
    }


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    public boolean evaluateEntry( ServerEntry entry ) throws Exception
    {
        if ( db.hasSystemIndexOn( node.getAttribute() ) )
        {
            // we don't maintain a presence index for objectClass, entryUUID, and entryCSN
            // however as every entry has such an attribute this evaluator always evaluates to true
            return true;
        }

        // get the attribute
        EntryAttribute attr = entry.get( type );

        // if the attribute exists just return true
        if ( attr != null )
        {
            return true;
        }

        // If we do not have the attribute, loop through the sub classes of
        // the attributeType.  Perhaps the entry has an attribute value of a
        // subtype (descendant) that will produce a match
        if ( schemaManager.getAttributeTypeRegistry().hasDescendants( node.getAttribute() ) )
        {
            // TODO check to see if descendant handling is necessary for the
            // index so we can match properly even when for example a name
            // attribute is used instead of more specific commonName
            Iterator<AttributeType> descendants = schemaManager.getAttributeTypeRegistry().descendants(
                node.getAttribute() );

            do
            {
                AttributeType descendant = descendants.next();

                attr = entry.get( descendant );

                if ( attr != null )
                {
                    return true;
                }
            }
            while ( descendants.hasNext() );
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }
}