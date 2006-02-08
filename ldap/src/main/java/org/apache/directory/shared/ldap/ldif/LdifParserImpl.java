/*
 *   Copyright 2001-2004 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.ldif;


import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;

import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.Base64;


/**
 * An LDAP Data Interchange Format (LDIF) parser.  All LDIF attributes including
 * control attributes within the LDIF that are not part of the entry proper are
 * added to the Attributes or MultiMap instance supplied for population.  These
 * attributes where applicable need to be removed from the populated MultiMap or
 * Attributes instance.  Until they are the populated container cannot be deemed
 * representative of an entry.
 *
 * @todo Get the RFC for LDIF syntax in this javadoc.
 * @see <a href="http://www.faqs.org/rfcs/rfc2849.html"> RFC 2849 </a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdifParserImpl implements LdifParser
{
    /**
     * Decodes an encoded string in base64 into a byte array.
     *
     * @param attrValue the value of a encoded binary attribute.
     * @return the decoded binary data as a byte array.
     */
    public byte [] base64decode( String attrValue )
    {
        return ( Base64.decode( attrValue.toCharArray() ) );
    }


    /**
     * Parses an String representing an entry in LDAP Data Interchange Format
     * (LDIF) storing its attributes in the supplied Attributes instance.
     *
     * @param attributes the attributes from the LDIF
     * including the DN of the entry represented by the LDIF.
     * @param ldif the entry in LDAP Data Interchange Format
     * @throws NamingException if there any failures while parsing the LDIF and
     * populating the attirubutes
     */
    public synchronized void parse( Attributes attributes, String ldif ) throws NamingException
    {
        boolean isBase64Encoded = false;
        int lineCount = 0;
        int index;
        String line;
        String attrName;
        String attrValue;
        StringReader strIn = new StringReader( ldif );
        BufferedReader in = new BufferedReader( strIn );

        try 
        {
            while ( ( line = in.readLine() ) != null )
            {
                // check and see if we have a comment on this line and if so
                // we need to forgo the entire line or chop off the comment
                int asteriskIndex = line.indexOf( '#' );
                if ( asteriskIndex != -1 )
                {
                    if ( asteriskIndex < 3 )
                    {
                        continue;
                    }

                    line = line.substring( 0, asteriskIndex ).trim();

                    if ( line.equals( "" ) )
                    {
                        continue;
                    }
                }

                // Try to advance to ':' if one exists.
                if ( ( index = line.indexOf( ':' ) ) == -1 )
                {
                    throw new LdapNamingException( "Line " + lineCount + " ["
                        + line + "] does not correspond to an LDIF entry "
                        + "attribute value pair.\n{" + ldif + "}",
                        ResultCodeEnum.OTHER );
                }

                // Capture data while at first colon.
                attrName = line.substring( 0, index ).trim();
    
                if ( line.length() <= index + 1 )
                {
                    continue;
                }

                // Consume next char and check if it's a colon for binary attr.
                if ( line.charAt( ++index ) == ':' )
                {
                    isBase64Encoded = true;
                }

                // Advance index past whitespace to the first char of the value.
                try
                {
                    while ( ( line.length() <= index + 1 ) && line.charAt( ++index ) == ' ' )
                    {; // Does nothing!
                    }

                    // Capture attribute value from first char till end of line.
                    attrValue = line.substring( index + 1 );
                }
                catch ( StringIndexOutOfBoundsException e )
                {
                    attrValue = "";
                }
    
                /*
                 * We need to construct an attribute yet we may not know if it
                 * is single valued or multi valued.  Our best bet is to just
                 * cover all possibilities using a basic attribute instance that
                 * the basic attrubutes instance creates automatically.
                 */
                if ( isBase64Encoded && ( attrValue != null ) )
                {
                    byte[] value = base64decode( attrValue );

                    if ( attributes.get( attrName ) == null )
                    {
                        attributes.put( attrName, value );
                    }
                    else
                    {
                        Attribute attribute = attributes.get( attrName );
                        attribute.add( value );
                    }
                    isBase64Encoded = false;
                }
                else
                {
                    if ( attributes.get( attrName ) == null )
                    {
                        attributes.put( attrName, attrValue );
                    }
                    else
                    {
                        Attribute attribute = attributes.get( attrName );
                        attribute.add( attrValue );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            // Does not really occur: we follow form by transforming w/ rethrow
            throw new LdapNamingException( ResultCodeEnum.OTHER );
        }
    }


    /**
     * Parses an String representing an entry in LDAP Data Interchange Format
     * (LDIF) storing its attributes in the supplied ldap entry instance.
     *
     * @param an_ldif the entry in LDAP Data Interchange Format
     * @return the LdifEntry parsed from the LDIF string
     * @throws NamingException if there any failures while parsing the LDIF and
     * populating the attirubutes
     */
    public LdifEntry parse( String an_ldif )
        throws NamingException
    {
        boolean l_isBase64Encoded = false;
        int l_lineCount = 0;
        int l_index;
        String l_line;
        String l_attrName = new String ();
        String l_attrValue = new String ();
        String l_prevAttrValue = null;
        StringReader l_strIn = new StringReader( an_ldif );
        BufferedReader l_in = new BufferedReader( l_strIn );
        LdifEntry l_entry = new LdifEntry();
        int l_currentModOp = -1;

        try 
        {
            while ( ( l_line = l_in.readLine() ) != null )
            {
                if ( l_line.equalsIgnoreCase( "-" ) )
                {                   
                    if ( l_prevAttrValue != null )
                    {
                        if ( l_entry.getModType().equalsIgnoreCase( "modify" ) )
                        {
                            if ( l_currentModOp == -1 )
                            {
                                throw new LdapNamingException( "A modification"
                                    + " type must be supplied for a change "
                                    + "type of modify",
                                        ResultCodeEnum.OTHER );
                            }
                            l_entry.addModificationItem( l_currentModOp, 
                                l_attrName, l_attrValue );
                        }
                        else
                        {
                            if ( l_isBase64Encoded && ( l_attrValue != null ) )
                            {
                                l_entry.addAttribute( l_attrName,
                                    base64decode( l_attrValue ) );
                                l_isBase64Encoded = false;
                            }
                            else
                            {
                                l_entry.addAttribute( l_attrName, l_attrValue );
                            }
                        }                        
                    }
                    l_currentModOp = -1;
                    l_prevAttrValue = null;
                    continue;
                }
                // Try to advance to ':' if one exists.
                if ( ( l_index = l_line.indexOf( ':' ) ) == -1 )
                {
                    throw new LdapNamingException( "Line " + l_lineCount + " ["
                        + l_line + "] does not correspond to an LDIF entry "
                        + "attribute value pair.\n{" + an_ldif + "}",
                            ResultCodeEnum.OTHER );
                }
    
                // Capture data while at first colon.
                l_attrName = l_line.substring( 0, l_index ).trim();

                // Consume next char and check if it's a colon for binary attr.
                if ( l_line.charAt( ++l_index ) == ':' )
                {
                    l_isBase64Encoded = true;
                }
    
                // Advance index past whitespace to the first char of the value.
                try
                {
                    while ( l_line.charAt( ++l_index ) == ' ' ) 
                    {
                       ; // Does nothing!
                    }

                    // Capture attribute value from first char till end of line.
                    l_attrValue = l_line.substring( l_index );
                }
                catch ( StringIndexOutOfBoundsException e )
                {
                    l_attrValue = "";
                }

                /*
                 *  We need to check to see if the current line is an
                 *  attribute, of if it a conrtol function
                 */
                if ( l_attrName.equalsIgnoreCase( "dn" ) )
                {
                    l_entry.setDn( l_attrValue );
                }
                else if ( l_attrName.equalsIgnoreCase( "version" ) )
                {
                    l_entry.setVersion( Integer.parseInt( l_attrValue ) );
                }
                else if ( l_attrName.equalsIgnoreCase( "control" ) )
                {
                   ; // Not implemented
                }
                else if ( l_attrName.equalsIgnoreCase( "changetype" ) )
                {
                    l_entry.setModType( l_attrValue );
                }
                else if ( l_attrName.equalsIgnoreCase( "add" ) )
                {
                    if ( !l_entry.getModType().equalsIgnoreCase( "modify" ) )
                    {
                        throw new LdapNamingException( "Cannot use modification "
                            + l_attrName + " identifier on " 
                            + l_entry.getModType()
                            + " change type",
                                ResultCodeEnum.OTHER );
                    }
                    l_currentModOp = DirContext.ADD_ATTRIBUTE ;
                }
                else if ( l_attrName.equalsIgnoreCase( "replace" ) )
                {
                if ( !l_entry.getModType().equalsIgnoreCase( "modify" ) )
                    {
                        throw new LdapNamingException( "Cannot use modification "
                            + l_attrName + " identifier on " 
                            + l_entry.getModType()
                            + " change type",
                                ResultCodeEnum.OTHER );
                    }
                    l_currentModOp = DirContext.REPLACE_ATTRIBUTE;
                }
                else if ( l_attrName.equalsIgnoreCase( "delete" ) )
                {
                    if ( !l_entry.getModType().equalsIgnoreCase( "modify" ) )
                    {
                        throw new LdapNamingException( "Cannot use modification "
                            + l_attrName + " identifier on " 
                            + l_entry.getModType()
                            + " change type",
                                ResultCodeEnum.OTHER );
                    }
                    l_currentModOp = DirContext.REMOVE_ATTRIBUTE;
                    if ( l_attrValue != null )
                    {
                        l_prevAttrValue = l_attrValue;
                    } 
                }
                else
                {
                    if ( l_entry.getModType().equalsIgnoreCase( "modify" ) )
                    {
                        if ( l_currentModOp == -1 )
                        {
                            throw new LdapNamingException( "A modification type must"
                                + " be supplied for a change type of modify",
                                    ResultCodeEnum.OTHER );
                        }
                        l_entry.addModificationItem( l_currentModOp, l_attrName,
                            l_attrValue );
                    }
                    else
                    {
                        if ( l_isBase64Encoded && ( l_attrValue != null ) )
                        {
                            l_entry.addAttribute( l_attrName,
                                base64decode( l_attrValue ) );
                            l_isBase64Encoded = false;
                        }
                        else
                        {
                            l_entry.addAttribute( l_attrName, l_attrValue );
                        }
                    }
                }
            }
            return l_entry;
        }
        catch ( IOException e )
        {
            // Does not really occur: we follow form by transforming w/ rethrow
            throw new LdapNamingException( e.getMessage(), ResultCodeEnum.OTHER );
        }
    }
}

