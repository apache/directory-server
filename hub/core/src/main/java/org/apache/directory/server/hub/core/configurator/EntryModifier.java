/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.hub.core.configurator;


import java.util.List;

import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.schema.AttributeType;


public class EntryModifier
{
    public static Entry generateEntryWithMods( Entry currentEntry, List<Modification> mods )
        throws LdapException
    {
        // The first step is to check that the modifications are valid :
        // - the ATs are present in the schema
        // - The value is syntaxically correct
        //
        // While doing that, we will apply the modification to a copy of the current entry
        Entry tempEntry = currentEntry.clone();

        // Now, apply each mod one by one
        for ( Modification mod : mods )
        {
            Attribute attribute = mod.getAttribute();
            AttributeType attributeType = attribute.getAttributeType();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    Attribute currentAttribute = tempEntry.get( attributeType );

                    if ( currentAttribute != null )
                    {
                        for ( Value<?> value : attribute )
                        {
                            currentAttribute.add( value );
                        }
                    }
                    else
                    {
                        Attribute newAttribute = attribute.clone();
                        tempEntry.put( newAttribute );
                    }

                    break;

                case REMOVE_ATTRIBUTE:

                    if ( attribute.size() == 0 )
                    {
                        tempEntry.removeAttributes( attributeType );
                    }
                    else
                    {
                        currentAttribute = tempEntry.get( attributeType );

                        for ( Value<?> value : attribute )
                        {
                            currentAttribute.remove( value );
                        }

                        if ( currentAttribute.size() == 0 )
                        {
                            tempEntry.removeAttributes( attributeType );
                        }
                    }

                    break;

                case REPLACE_ATTRIBUTE:
                    if ( !tempEntry.containsAttribute( attributeType ) )
                    {
                        if ( attribute.size() == 0 )
                        {
                            break;
                        }
                        else
                        {
                            Attribute newAttribute = createNewAttribute( attribute );

                            tempEntry.put( newAttribute );
                        }
                    }
                    else
                    {
                        if ( attribute.size() == 0 )
                        {
                            tempEntry.removeAttributes( attributeType );
                        }
                        else
                        {
                            tempEntry.removeAttributes( attributeType );

                            Attribute newAttribute = createNewAttribute( attribute );

                            tempEntry.put( newAttribute );
                        }
                    }

                    break;
            }
        }

        return tempEntry;
    }


    private static Attribute createNewAttribute( Attribute attribute ) throws LdapException
    {
        AttributeType attributeType = attribute.getAttributeType();

        Attribute newAttribute = new DefaultAttribute( attribute.getUpId(), attributeType );

        for ( Value<?> value : attribute )
        {
            newAttribute.add( value );
        }

        return newAttribute;
    }
}
