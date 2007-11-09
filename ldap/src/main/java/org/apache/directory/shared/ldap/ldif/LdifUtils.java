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
package org.apache.directory.shared.ldap.ldif;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyRequest;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Some LDIF useful methods
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifUtils
{
	/** The array that will be used to match the first char.*/
    private static boolean[] LDIF_SAFE_STARTING_CHAR_ALPHABET = new boolean[128];
    
    /** The array that will be used to match the other chars.*/
    private static boolean[] LDIF_SAFE_OTHER_CHARS_ALPHABET = new boolean[128];
    
    /** The default length for a line in a ldif file */
    private static final int DEFAULT_LINE_LENGTH = 80;
    
    static
    {
    	// Initialization of the array that will be used to match the first char.
    	for (int i = 0; i < 128; i++) 
        {
    		LDIF_SAFE_STARTING_CHAR_ALPHABET[i] = true;
		}
    	
        LDIF_SAFE_STARTING_CHAR_ALPHABET[0] = false; // 0 (NUL)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[10] = false; // 10 (LF)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[13] = false; // 13 (CR)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[32] = false; // 32 (SPACE)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[58] = false; // 58 (:)
    	LDIF_SAFE_STARTING_CHAR_ALPHABET[60] = false; // 60 (>)
    	
    	// Initialization of the array that will be used to match the other chars.
    	for (int i = 0; i < 128; i++) 
        {
    		LDIF_SAFE_OTHER_CHARS_ALPHABET[i] = true;
		}
    	
        LDIF_SAFE_OTHER_CHARS_ALPHABET[0] = false; // 0 (NUL)
    	LDIF_SAFE_OTHER_CHARS_ALPHABET[10] = false; // 10 (LF)
    	LDIF_SAFE_OTHER_CHARS_ALPHABET[13] = false; // 13 (CR)
    }

    /**
     * Checks if the input String contains only safe values, that is, the data
     * does not need to be encoded for use with LDIF. The rules for checking safety
     * are based on the rules for LDIF (LDAP Data Interchange Format) per RFC 2849.
     * The data does not need to be encoded if all the following are true:
     * 
     * The data cannot start with the following char values:
     * 		00 (NUL)
     * 		10 (LF)
     * 		13 (CR)
     * 		32 (SPACE)
     * 		58 (:)
     * 		60 (<)
     * 		Any character with value greater than 127
     * 
     * The data cannot contain any of the following char values:
     * 		00 (NUL)
     * 		10 (LF)
     * 		13 (CR)
     * 		Any character with value greater than 127
     * 
     * The data cannot end with a space.
     * 
     * @param str the String to be checked
     * @return true if encoding not required for LDIF
     */
    public static boolean isLDIFSafe( String str )
    {
    	// Checking the first char
    	char currentChar = str.charAt(0);
        
    	if ( ( currentChar > 127 ) || !LDIF_SAFE_STARTING_CHAR_ALPHABET[currentChar] )
    	{
    		return false;
    	}
    	
    	// Checking the other chars
    	for (int i = 1; i < str.length(); i++)
    	{
        	currentChar = str.charAt(i);
        	
        	if ( ( currentChar > 127 ) || !LDIF_SAFE_OTHER_CHARS_ALPHABET[currentChar] )
        	{
        		return false;
        	}
		}
    	
    	// The String cannot end with a space
    	return ( currentChar != ' ' );
    }
    
    /**
     * Convert an Attributes as LDIF
     * @param attrs the Attributes to convert
     * @param length the expectend line length
     * @return the corresponding LDIF code as a String
     * @throws NamingException If a naming exception is encountered.
     */
    public static String convertToLdif( Attributes attrs ) throws NamingException
    {
        return convertToLdif( attrs, DEFAULT_LINE_LENGTH );
    }
    
    
    /**
     * Convert an Attributes as LDIF
     * @param attrs the Attributes to convert
     * @param length the expected line length
     * @return the corresponding LDIF code as a String
     * @throws NamingException If a naming exception is encountered.
     */
    public static String convertToLdif( Attributes attrs, int length ) throws NamingException
    {
		StringBuilder sb = new StringBuilder();
		
		NamingEnumeration<? extends Attribute> ne = attrs.getAll();
		
		while ( ne.hasMore() )
		{
			Object attribute = ne.next();
            
			if ( attribute instanceof Attribute ) 
            {
				sb.append( convertToLdif( (Attribute) attribute, length ) );
			}			
		}
		
		return sb.toString();
	}
    
    /**
     * Convert an Entry to LDIF
     * @param entry the entry to convert
     * @return the corresponding LDIF as a String
     * @throws NamingException If a naming exception is encountered.
     */
    public static String convertToLdif( Entry entry ) throws NamingException
    {
        return convertToLdif( entry, DEFAULT_LINE_LENGTH );
    }
    
    /**
     * Convert an Entry to LDIF
     * @param entry the entry to convert
     * @return the corresponding LDIF as a String
     * @throws NamingException If a naming exception is encountered.
     */
    public static String convertToLdif( Entry entry, int length ) throws NamingException
    {
        StringBuilder sb = new StringBuilder();
        
        // First, dump the DN
        if ( isLDIFSafe( entry.getDn() ) )
        {
            sb.append( stripLineToNChars( "dn: " + entry.getDn(), length ) );
        }
        else
        {
            sb.append( stripLineToNChars( "dn:: " + encodeBase64( entry.getDn() ), length ) );
        }
        
        sb.append( '\n' );
        
        // Dump the ChangeType
        sb.append( stripLineToNChars( "changeType: " + entry.getChangeType(), length ) );
        
        sb.append( '\n' );

        switch ( entry.getChangeType() )
        {
            case Delete :
                if ( entry.getAttributes() != null )
                {
                    throw new NamingException( "Invalid Entry : a deleted entry should not contain attributes" );
                }
                
                break;
                
            case Add :
                if ( ( entry.getAttributes() == null ) )
                {
                    throw new NamingException( "Invalid Entry : a added or modified entry should contain attributes" );
                }

                // Now, iterate through all the attributes
                NamingEnumeration<? extends Attribute> ne = entry.getAttributes().getAll();
                
                while ( ne.hasMore() )
                {
                    Attribute attribute = ne.next();
                    
                    sb.append( convertToLdif( attribute, length ) );
                }
                
                break;
                
            case ModDn :
            case ModRdn :
                if ( entry.getAttributes() != null )
                {
                    throw new NamingException( "Invalid Entry : a modifyDN operation entry should not contain attributes" );
                }
                
                // Stores the deleteoldrdn flag
                sb.append( "deleteoldrdn: " );
                
                if ( entry.isDeleteOldRdn() )
                {
                    sb.append( "1" );
                }
                else
                {
                    sb.append( "0" );
                }
                
                sb.append( '\n' );
                
                // Stores the optional newSuperior
                if ( ! StringTools.isEmpty( entry.getNewSuperior() ) )
                {
                    Attribute newSuperior = new AttributeImpl( "newsuperior", entry.getNewSuperior() );
                    sb.append( convertToLdif( newSuperior, length ) );
                }
                
                // Stores the new RDN
                Attribute newRdn = new AttributeImpl( "newrdn", entry.getNewRdn() );
                sb.append( convertToLdif( newRdn, length ) );
                
                break;
                
            case Modify :
                for ( ModificationItem modification:entry.getModificationItems() )
                {
                    switch ( modification.getModificationOp() )
                    {
                        case DirContext.ADD_ATTRIBUTE :
                            sb.append( "add: " );
                            break;
                            
                        case DirContext.REMOVE_ATTRIBUTE :
                            sb.append( "delete: " );
                            break;
                            
                        case DirContext.REPLACE_ATTRIBUTE :
                            sb.append( "replace: " );
                            break;
                            
                    }
                    
                    sb.append( modification.getAttribute().getID() );
                    sb.append( '\n' );
                    
                    sb.append( convertToLdif( modification.getAttribute() ) );
                    sb.append( "-\n" );
                }
                break;
                
        }
        
        sb.append( '\n' );
        
        return sb.toString();
    }
    
    /**
     * Base64 encode a String  
     */
    private static String encodeBase64( String str )
    {
        char[] encoded;
        
        try
        {
            // force encoding using UTF-8 charset, as required in RFC2849 note 7
            encoded = Base64.encode( ( ( String ) str ).getBytes( "UTF-8" ) );
        }
        catch ( UnsupportedEncodingException e )
        {
            encoded = Base64.encode( ( ( String ) str ).getBytes() );
        }
        
        return new String( encoded );
    }
    

    /**
     * Converts an Attribute as LDIF
     * @param attr the Attribute to convert
     * @return the corresponding LDIF code as a String
     * @throws NamingException If a naming exception is encountered.
     */
    public static String convertToLdif( Attribute attr ) throws NamingException
    {
        return convertToLdif( attr, DEFAULT_LINE_LENGTH );
    }
    
    
    /**
     * Converts an Attribute as LDIF
     * @param attr the Attribute to convert
     * @param length the expected line length
     * @return the corresponding LDIF code as a String
     * @throws NamingException If a naming exception is encountered.
     */
	public static String convertToLdif( Attribute attr, int length ) throws NamingException
	{
		StringBuilder sb = new StringBuilder();
		
		// iterating on the attribute's values
		for ( int i = 0; i < attr.size(); i++ )
        {
			StringBuilder lineBuffer = new StringBuilder();
			
            lineBuffer.append( attr.getID() );
			
			Object value = attr.get( i );
            
			// First, deal with null value (which is valid)
			if ( value == null )
			{
                lineBuffer.append( ':' );
			}
			else if ( value instanceof byte[] )
            {
            	// It is binary, so we have to encode it using Base64 before adding it
            	char[] encoded = Base64.encode( ( byte[] ) value );
            	
            	lineBuffer.append( ":: " + new String( encoded ) );                        	
            }
            else if ( value instanceof String )
            {
            	// It's a String but, we have to check if encoding isn't required
            	String str = (String) value;
                
            	if ( !LdifUtils.isLDIFSafe( str ) )
            	{
                    lineBuffer.append( ":: " + encodeBase64( (String)value ) );
            	}
            	else
            	{
            		lineBuffer.append( ": " + value );
            	}
            }
            
            lineBuffer.append( "\n" );
            sb.append( stripLineToNChars( lineBuffer.toString(), length ) );
        }
		
		return sb.toString();
	}
	
	
	/**
	 * Strips the String every n specified characters
	 * @param str the string to strip
	 * @param nbChars the number of characters
	 * @return the stripped String
	 */
	public static String stripLineToNChars( String str, int nbChars)
	{
        int strLength = str.length();

        if ( strLength <= nbChars )
		{
			return str;
		}
        
        if ( nbChars < 2 )
        {
            throw new IllegalArgumentException( "The length of each line must be at least 2 chars long" );
        }
		
        // We will first compute the new size of the LDIF result
        // It's at least nbChars chars plus one for \n
        int charsPerLine = nbChars - 1;

        int remaining = ( strLength - nbChars ) % charsPerLine;

        int nbLines = 1 + ( ( strLength - nbChars ) / charsPerLine ) +
                        ( remaining == 0 ? 0 : 1 );

        int nbCharsTotal = strLength + nbLines + nbLines - 2;

        char[] buffer = new char[ nbCharsTotal ];
        char[] orig = str.toCharArray();
        
        int posSrc = 0;
        int posDst = 0;
        
        System.arraycopy( orig, posSrc, buffer, posDst, nbChars );
        posSrc += nbChars;
        posDst += nbChars;
        
        for ( int i = 0; i < nbLines - 2; i ++ )
        {
            buffer[posDst++] = '\n';
            buffer[posDst++] = ' ';
            
            System.arraycopy( orig, posSrc, buffer, posDst, charsPerLine );
            posSrc += charsPerLine;
            posDst += charsPerLine;
        }

        buffer[posDst++] = '\n';
        buffer[posDst++] = ' ';
        System.arraycopy( orig, posSrc, buffer, posDst, remaining == 0 ? charsPerLine : remaining );
        
        return new String( buffer );
	}
	
	
    /**
     * Compute a reverse LDIF of an AddRequest. It's simply a delete request
     * of the added entry
     *
     * @param addRequest The added entry
     * @return A reverse LDIF
     * @throws NamingException If something went wrong
     */
    public static Entry reverseAdd( AddRequest addRequest) throws NamingException
    {
        Entry entry = new Entry();
        entry.setChangeType( ChangeType.Delete );
        entry.setDn( addRequest.getEntry().getUpName() );

        return entry;
    }

    
    /**
     * Compute a reverse LDIF of a DeleteRequest. We have to get the previous
     * entry in order to restore it.
     *
     * @param dn The deleted entry DN
     * @param deletedEntry The entry which has been deleted
     * @return A reverse LDIF
     * @throws NamingException If something went wrong
     */
    public static Entry reverseDel( LdapDN dn, Attributes deletedEntry ) throws NamingException
    {
        Entry entry = new Entry();
        
        entry.setDn( dn.getUpName() );
        entry.setChangeType( ChangeType.Add );
        NamingEnumeration<? extends Attribute> attributes = deletedEntry.getAll();
        
        while ( attributes.hasMoreElements() )
        {
            entry.addAttribute( attributes.nextElement() );
        }       

        return entry;
    }
    
    
    /**
     * Compute a reverse LDIF for a ModifyDNRequest. This is more complex than 
     * the Add and Delete operation, as we have to handle four different cases :
     *  - no new superior, no deleteOldRdn 
     *  - no new superior, deleteOldRdn set to true
     *  - a new superior, no deleteOldRdn 
     *  - a new superior, deleteOldRdn set to true
     *
     * @param modifyDn The modifyRequest
     * @return A reverse LDIF
     * @throws NamingException If something went wrong
     */
    public static Entry reverseModifyDN( ModifyDnRequest modifyDn ) throws NamingException
    {
        Entry entry = new Entry();
        
        LdapDN newDN = null;
        
        if ( modifyDn.getNewSuperior() != null )
        {
            newDN = modifyDn.getNewSuperior();
            String newSuperior = ( (LdapDN)modifyDn.getName().getPrefix( modifyDn.getName().size() - 1 ) ).getUpName();
            String trimmedSuperior = StringTools.trim( newSuperior );
            entry.setNewSuperior( trimmedSuperior );
        }
        else
        {
            newDN = (LdapDN)modifyDn.getName().getPrefix( modifyDn.getName().size() - 1 );
        }
        
        newDN.add( modifyDn.getNewRdn() );
        
        entry.setDn( newDN.getUpName() );
        entry.setChangeType( ChangeType.ModDn );
        entry.setDeleteOldRdn( true );
        
        
        entry.setNewRdn( modifyDn.getName().getRdn().getUpName() );
        
        return entry;
    }
    
    
    /**
     * 
     * Compute the reversed LDIF for a modify request. We will deal with the 
     * three kind of modifications :
     * - add
     * - remove
     * - replace
     * 
     * As the modifications should be issued in a reversed order ( ie, for 
     * the initials modifications {A, B, C}, the reversed modifications will
     * be ordered like {C, B, A}), we will change the modifications order. 
     *
     * @param modifyRequest The modify request
     * @param modifiedEntry The modified entry. Necessary for the destructive modifications
     * @return A reversed LDIF 
     * @throws NamingException If something went wrong
     */
    public static Entry reverseModify( ModifyRequest modifyRequest, Attributes modifiedEntry ) throws NamingException
    {
        Entry entry = new Entry();
        entry.setChangeType( ChangeType.Modify );
        
        entry.setDn( modifyRequest.getName().getUpName() );
        
        // As the reversed modifications should be pushed in reversed order,
        // we create a list to temporarily store the modifications.
        List<ModificationItemImpl> reverseModifications = new ArrayList<ModificationItemImpl>();
        
        // Loop through all the modifications
        for ( ModificationItem modification:modifyRequest.getModificationItems() )
        {
            switch ( modification.getModificationOp() )
            {
                case DirContext.ADD_ATTRIBUTE :
                    Attribute mod = modification.getAttribute();
                    
                    Attribute previous = modifiedEntry.get( mod.getID() );
                    
                    if ( mod.equals( previous ) )
                    {
                        continue;
                    }
                    
                    ModificationItemImpl reverseModification = new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, mod );
                    reverseModifications.add( 0, reverseModification );
                    break;
                    
                case DirContext.REMOVE_ATTRIBUTE :
                    mod = modification.getAttribute();
                    
                    previous = modifiedEntry.get( mod.getID() );
                    
                    if ( mod.get() == null )
                    {
                        reverseModification = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, previous );
                        reverseModifications.add( 0, reverseModification );
                        continue;
                    }
                    
                    reverseModification = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, mod );
                    reverseModifications.add( 0, reverseModification );
                    break;
                    
                case DirContext.REPLACE_ATTRIBUTE :
                    mod = modification.getAttribute();
                    
                    previous = modifiedEntry.get( mod.getID() );
                    
                    if ( mod.get() == null )
                    {
                        reverseModification = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, previous );
                        reverseModifications.add( 0, reverseModification );
                        continue;
                    }
                    
                    if ( previous == null )
                    {
                        Attribute emptyAttribute = new AttributeImpl( mod.getID() );
                        reverseModification = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, emptyAttribute );
                        reverseModifications.add( 0, reverseModification );
                        continue;
                    }
                    
                    reverseModification = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, previous );
                    reverseModifications.add( 0, reverseModification );
                    break;
            }
        }
        
        // Special case if we don't have any reverse modifications
        if ( reverseModifications.size() == 0 )
        {
            return null;
        }
        
        // Now, push the reversed list into the entry
        for ( ModificationItemImpl modification:reverseModifications )
        {
            entry.addModificationItem( modification );
        }
        
        // Return the reverted entry
        return entry;
    }
}

