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
package org.apache.directory.server.core.subtree;


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;


/**
 * A refinement leaf node evaluator.  This evaluator checks to see if the
 * objectClass attribute of a candidate entry is matched by a leaf node in
 * a refinement filter expression tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RefinementLeafEvaluator
{
    /** an OID to name and vice versa registry */
    private final OidRegistry registry;


    /**
     * Creates a refinement filter's leaf node evaluator.
     *
     * @param registry the OID registry used to lookup names for objectClass OIDs
     */
    public RefinementLeafEvaluator(OidRegistry registry)
    {
        this.registry = registry;
    }


    /**
     * Evaluates whether or not a simple leaf node of a refinement filter selects an
     * entry based on the entry's objectClass attribute values.
     *
     * @param node the leaf node of the refinement filter
     * @param objectClasses the objectClass attribute's values
     * @return true if the leaf node selects the entry based on objectClass values, false
     * if it rejects the entry
     * @throws NamingException
     */
    public boolean evaluate( SimpleNode node, EntryAttribute objectClasses ) throws NamingException
    {
        if ( node == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_295 ) );
        }
        
        if ( !( node instanceof EqualityNode ) )
        {
            throw new NamingException( I18n.err( I18n.ERR_301, node ) );
        }
        
        if ( !node.getAttribute().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT ) )
        {
            throw new NamingException( I18n.err( I18n.ERR_302, node.getAttribute() ) );
        }

        if ( null == objectClasses )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_303 ) );
        }
        
        if ( !((ServerAttribute)objectClasses).instanceOf( SchemaConstants.OBJECT_CLASS_AT ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_304 ) );
        }

        // check if AVA value exists in attribute
        // If the filter value for the objectClass is an OID we need to resolve a name
        String value = node.getValue().getString();

        if ( objectClasses.contains( value ) )
        {
            return true;
        }
        
        if ( Character.isDigit( value.charAt( 0 ) ) )
        {
            Iterator<String> list = registry.getNameSet( value ).iterator();
            
            while ( list.hasNext() )
            {
                String objectClass = list.next();
                
                if ( objectClasses.contains( objectClass ) )
                {
                    return true;
                }
            }
        }

        // no match so return false
        return false;
    }
}
