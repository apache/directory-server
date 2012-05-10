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
