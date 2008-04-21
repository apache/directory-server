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


import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.schema.registries.Registries;

import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import java.util.Iterator;


/**
 * An Evaluator which determines if candidates are matched by GreaterEqNode
 * assertions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PresenceEvaluator implements Evaluator<PresenceNode, Attributes>
{
    private final PresenceNode node;
    private final Store<Attributes> db;
    private final Registries registries;
    private final AttributeType type;
    private final Index<String,Attributes> idx;


    public PresenceEvaluator( PresenceNode node, Store<Attributes> db, Registries registries )
        throws Exception
    {
        this.db = db;
        this.node = node;
        this.registries = registries;
        this.type = registries.getAttributeTypeRegistry().lookup( node.getAttribute() );

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


    public boolean evaluate( IndexEntry<?,Attributes> indexEntry ) throws Exception
    {
        if ( idx != null )
        {
            return idx.forward( type.getOid(), indexEntry.getId() );
        }

        Attributes entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        // get the attribute
        Attribute attr = AttributeUtils.getAttribute( entry, type );

        // if the attribute exists just return true
        if ( attr != null )
        {
            return true;
        }

        // If we do not have the attribute, loop through the sub classes of
        // the attributeType.  Perhaps the entry has an attribute value of a
        // subtype (descendant) that will produce a match
        if ( registries.getAttributeTypeRegistry().hasDescendants( node.getAttribute() ) )
        {
            // TODO check to see if descendant handling is necessary for the
            // index so we can match properly even when for example a name
            // attribute is used instead of more specific commonName
            Iterator<AttributeType> descendants =
                registries.getAttributeTypeRegistry().descendants( node.getAttribute() );

            do
            {
                AttributeType descendant = descendants.next();

                attr = AttributeUtils.getAttribute( entry, descendant );

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