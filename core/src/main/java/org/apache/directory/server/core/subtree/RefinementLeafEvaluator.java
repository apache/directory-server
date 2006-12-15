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


import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.shared.ldap.filter.AssertionEnum;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.util.StringTools;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.util.Iterator;


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
    public boolean evaluate( SimpleNode node, Attribute objectClasses ) throws NamingException
    {
        if ( node == null )
        {
            throw new IllegalArgumentException( "node cannot be null" );
        }
        if ( node.getAssertionType() != AssertionEnum.EQUALITY )
        {
            throw new NamingException( "Unrecognized assertion type for refinement node: " + node.getAssertionType() );
        }
        if ( !node.getAttribute().equalsIgnoreCase( "objectclass" ) )
        {
            throw new NamingException( "Refinement leaf node attribute was " + node.getAttribute() );
        }

        if ( null == objectClasses )
        {
            throw new IllegalArgumentException( "objectClasses argument cannot be null" );
        }
        if ( !objectClasses.getID().equalsIgnoreCase( "objectclass" ) )
        {
            throw new IllegalArgumentException( "objectClasses attribute must be for ID 'objectClass'" );
        }

        // check if AVA value exists in attribute
        if ( objectClasses.contains( node.getValue() ) )
        {
            return true;
        }

        // If the filter value for the objectClass is an OID we need to resolve a name
        String value = null;
        if ( node.getValue() instanceof String )
        {
            value = ( String ) node.getValue();
        }
        else if ( node.getValue() instanceof byte[] )
        {
            value = "#" + StringTools.toHexString( ( byte[] ) node.getValue() );
        }
        else
        {
            value = node.getValue().toString();
        }
        
        if ( Character.isDigit( value.charAt( 0 ) ) )
        {
            Iterator list = registry.getNameSet( value ).iterator();
            while ( list.hasNext() )
            {
                String objectClass = ( String ) list.next();
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
