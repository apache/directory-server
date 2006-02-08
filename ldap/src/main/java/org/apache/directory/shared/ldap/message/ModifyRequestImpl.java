/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.message;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Lockable ModifyRequest implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ModifyRequestImpl extends AbstractAbandonableRequest implements ModifyRequest
{
    static final long serialVersionUID = -505803669028990304L;

    /** The logger */
    private static final transient Logger log = LoggerFactory.getLogger( ModifyRequestImpl.class );

    /** Dn of the entry to modify or PDU's <b>object</b> field */
    private String name;

    /** Sequence of modifications or PDU's <b>modification</b> seqence field */
    private ArrayList mods = new ArrayList();

    private ModifyResponse response;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a Lockable ModifyRequest implementing object used to modify the
     * attributes of an entry.
     * 
     * @param id
     *            the sequential message identifier
     */
    public ModifyRequestImpl(final int id)
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // ModifyRequest Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets an immutable Collection of modification items representing the
     * atomic changes to perform on the candidate entry to modify.
     * 
     * @return an immutatble Collection of ModificationItem instances.
     * @see <{javax.naming.directory.ModificationItem}>
     */
    public Collection getModificationItems()
    {
        return Collections.unmodifiableCollection( mods );
    }


    /**
     * Gets the distinguished name of the entry to be modified by this request.
     * This property represents the PDU's <b>object</b> field.
     * 
     * @return the DN of the modified entry.
     */
    public String getName()
    {
        return name;
    }


    /**
     * Sets the distinguished name of the entry to be modified by this request.
     * This property represents the PDU's <b>object</b> field.
     * 
     * @param name
     *            the DN of the modified entry.
     */
    public void setName( String name )
    {
        this.name = name;
    }


    /**
     * Adds a ModificationItem to the set of modifications composing this modify
     * request.
     * 
     * @param mod
     *            a ModificationItem to add.
     */
    public void addModification( ModificationItem mod )
    {
        mods.add( mod );
    }


    /**
     * Removes a ModificationItem to the set of modifications composing this
     * modify request.
     * 
     * @param mod
     *            a ModificationItem to remove.
     */
    public void removeModification( ModificationItem mod )
    {
        mods.remove( mod );
    }


    // ------------------------------------------------------------------------
    // SingleReplyRequest Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the protocol response message type for this request which produces
     * at least one response.
     * 
     * @return the message type of the response.
     */
    public MessageTypeEnum getResponseType()
    {
        return RESP_TYPE;
    }


    /**
     * The result containing response for this request.
     * 
     * @return the result containing response for this request
     */
    public ResultResponse getResultResponse()
    {
        if ( response == null )
        {
            response = new ModifyResponseImpl( getMessageId() );
        }

        return response;
    }


    /**
     * Checks to see if ModifyRequest stub equals another by factoring in checks
     * for the name and modification items of the request.
     * 
     * @param obj
     *            the object to compare this ModifyRequest to
     * @return true if obj equals this ModifyRequest, false otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( !super.equals( obj ) )
        {
            return false;
        }

        ModifyRequest req = ( ModifyRequest ) obj;

        if ( name != null && req.getName() == null )
        {
            return false;
        }

        if ( name == null && req.getName() != null )
        {
            return false;
        }

        if ( name != null && req.getName() != null )
        {
            if ( !name.equals( req.getName() ) )
            {
                return false;
            }
        }

        if ( req.getModificationItems().size() != mods.size() )
        {
            return false;
        }

        Iterator list = req.getModificationItems().iterator();

        for ( int ii = 0; ii < mods.size(); ii++ )
        {
            ModificationItem item = ( ModificationItem ) list.next();

            if ( !equals( ( ModificationItem ) mods.get( ii ), item ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Checks to see if two ModificationItems are equal by factoring in the
     * modification operation as well as the attribute of each item.
     * 
     * @param item0
     *            the first ModificationItem to compare
     * @param item1
     *            the second ModificationItem to compare
     * @return true if the ModificationItems are equal, false otherwise
     */
    private boolean equals( ModificationItem item0, ModificationItem item1 )
    {
        if ( item0 == item1 )
        {
            return true;
        }

        if ( item0.getModificationOp() != item1.getModificationOp() )
        {
            return false;
        }

        // compare attribute id's at the very least
        if ( !item0.getAttribute().getID().equals( item1.getAttribute().getID() ) )
        {
            return false;
        }

        try
        {
            Attribute attr0 = item0.getAttribute();
            Attribute attr1 = item1.getAttribute();

            Set attrHash0 = new HashSet();

            NamingEnumeration iter0 = attr0.getAll();

            while ( iter0.hasMoreElements() )
            {
                attrHash0.add( iter0.next() );
            }

            NamingEnumeration iter1 = attr1.getAll();

            while ( iter1.hasMoreElements() )
            {
                Object value = iter1.next();

                if ( attrHash0.contains( value ) == false )
                {
                    if ( value instanceof byte[] )
                    {
                        String sValue = StringTools.utf8ToString( ( byte[] ) value );

                        if ( attrHash0.contains( sValue ) == false )
                        {
                            return false;
                        }
                        else
                        {
                            attrHash0.remove( sValue );
                            continue;
                        }
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    attrHash0.remove( value );
                }
            }

            return ( attrHash0.size() == 0 );
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }


    /**
     * Get a String representation of a ModifyRequest
     * 
     * @return A ModifyRequest String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Modify Request\n" );
        sb.append( "        Object : '" ).append( name ).append( "'\n" );

        if ( mods != null )
        {

            for ( int i = 0; i < mods.size(); i++ )
            {

                ModificationItem modification = ( ModificationItem ) mods.get( i );

                sb.append( "            Modification[" ).append( i ).append( "]\n" );
                sb.append( "                Operation : " );

                switch ( modification.getModificationOp() )
                {

                    case DirContext.ADD_ATTRIBUTE:
                        sb.append( " add\n" );
                        break;

                    case DirContext.REPLACE_ATTRIBUTE:
                        sb.append( " replace\n" );
                        break;

                    case DirContext.REMOVE_ATTRIBUTE:
                        sb.append( " delete\n" );
                        break;
                }

                sb.append( "                Modification\n" );

                Attribute attribute = modification.getAttribute();

                try
                {
                    sb.append( "                    Type : '" ).append( attribute.getID() ).append( "'\n" );
                    sb.append( "                    Vals\n" );

                    for ( int j = 0; j < attribute.size(); j++ )
                    {
                        sb.append( "                        Val[" ).append( j ).append( "] : '" );

                        Object attributeValue = attribute.get( j );

                        if ( attributeValue instanceof byte[] )
                        {
                            sb.append( StringTools.utf8ToString( ( byte[] ) attributeValue ) ).append( '/' ).append(
                                StringTools.dumpBytes( ( byte[] ) attributeValue ) );
                        }
                        else if ( attributeValue instanceof String )
                        {
                            sb.append( attributeValue );
                        }
                        else
                        {
                            sb.append( StringTools.dumpBytes( StringTools.getBytesUtf8( attributeValue.toString() ) ) );
                        }

                        sb.append( "' \n" );

                    }
                }
                catch ( NamingException ne )
                {
                    log.error( "Naming exception while printing the '" + attribute.getID() + "'" );
                }
            }
        }

        return sb.toString();
    }
}
