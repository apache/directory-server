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

package org.apache.directory.shared.ldap.name;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The LdapDN class contains a DN (Distinguished Name).
 *
 * Its specification can be found in RFC 2253,
 * "UTF-8 String Representation of Distinguished Names".
 *
 * We will store two representation of a DN :
 * - a user Provider represeentation, which is the parsed String given by a user
 * - an internal representation.
 *
 * A DN is formed of RDNs, in a specific order :
 *  RDN[n], RDN[n-1], ... RDN[1], RDN[0]
 *
 * It represents a tree, in which the root is the last RDN (RDN[0]) and the leaf
 * is the first RDN (RDN[n]).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapDN implements Name, Externalizable
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( LdapDN.class );

    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** Value returned by the compareTo method if values are not equals */
    public static final int NOT_EQUAL = -1;

    /** Value returned by the compareTo method if values are equals */
    public static final int EQUAL = 0;

    /** A flag used to tell if the DN has been normalized */
    private boolean normalized;

    // ~ Static fields/initializers
    // -----------------------------------------------------------------
    /**
     *  The RDNs that are elements of the DN
     * NOTE THAT THESE ARE IN THE OPPOSITE ORDER FROM THAT IMPLIED BY THE JAVADOC!
     * Rdn[0] is rdns.get(n) and Rdn[n] is rdns.get(0)
     */
    protected List<Rdn> rdns = new ArrayList<Rdn>( 5 );

    /** The user provided name */
    private String upName;

    /** The normalized name */
    private String normName;

    /** The bytes representation of the normName */
    private byte[] bytes;

    /** A null LdapDN */
    public static final LdapDN EMPTY_LDAPDN = new LdapDN();


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Construct an empty LdapDN object
     */
    public LdapDN()
    {
        upName = "";
        normName = "";
        normalized = true;
    }


    /**
     * Transduces, or copies a Name to an LdapDN.
     *
     * @param name composed of String name components.
     * @throws InvalidNameException If the Name is invalid.
     */
    public LdapDN( Name name ) throws InvalidNameException
    {
        if ( ( name != null ) && ( name.size() != 0 ) )
        {
            for ( int ii = 0; ii < name.size(); ii++ )
            {
                String nameComponent = name.get( ii );
                add( nameComponent );
            }
        }

        normalized = false;

    }


    /**
     * Creates an ldap name using a list of NameComponents. Each NameComponent
     * is a String
     *
     * @param list of String name components.
     * @throws InvalidNameException If the nameComponent is incorrect
     */
    public LdapDN( List<String> list ) throws InvalidNameException
    {
        if ( ( list != null ) && ( list.size() != 0 ) )
        {
            for ( String nameComponent : list )
            {
                add( 0, nameComponent );
            }
        }

        normalized = false;

    }


    /**
     * Creates an ldap name using a list of name components.
     *
     * @param nameComponents List of String name components.
     * @throws InvalidNameException If the nameComponent is incorrect
     */
    public LdapDN( Iterator<String> nameComponents ) throws InvalidNameException
    {
        if ( nameComponents != null )
        {
            while ( nameComponents.hasNext() )
            {
                String nameComponent = nameComponents.next();
                add( 0, nameComponent );
            }
        }

        normalized = false;
    }


    /**
     * Parse a String and checks that it is a valid DN <br>
     * <p>
     * &lt;distinguishedName&gt; ::= &lt;name&gt; | e <br>
     * &lt;name&gt; ::= &lt;name-component&gt; &lt;name-components&gt; <br>
     * &lt;name-components&gt; ::= &lt;spaces&gt; &lt;separator&gt;
     * &lt;spaces&gt; &lt;name-component&gt; &lt;name-components&gt; | e <br>
     * </p>
     *
     * @param upName The String that contains the DN.
     * @throws InvalidNameException if the String does not contain a valid DN.
     */
    public LdapDN( String upName ) throws InvalidNameException
    {
        if ( upName != null )
        {
            LdapDnParser.parseInternal( upName, rdns );
        }

        // Stores the representations of a DN : internal (as a string and as a
        // byte[]) and external.
        normalizeInternal();
        normalized = false;

        this.upName = upName;
    }


    /**
     * Create a DN when deserializing it.
     * 
     * Note : this constructor is used only by the deserialization method.
     * @param upName The user provided name
     * @param normName the normalized name
     * @param bytes the name as a byte[]
     */
    /* No protection */ LdapDN( String upName, String normName, byte[] bytes )
    {
        normalized = true;
        this.upName = upName;
        this.normName = normName;
        this.bytes = bytes;
    }


    /**
     * Static factory which creates a normalized DN from a String and a Map of OIDs.
     *
     * @param name The DN as a String
     * @param oidsMap The OID mapping
     * @return A valid DN
     * @throws InvalidNameException If the DN is invalid.
     * @throws NamingException If something went wrong.
     */
    public static Name normalize( String name, Map<String, OidNormalizer> oidsMap ) throws InvalidNameException,
        NamingException
    {
        if ( ( name == null ) || ( name.length() == 0 ) || ( oidsMap == null ) || ( oidsMap.size() == 0 ) )
        {
            return LdapDN.EMPTY_LDAPDN;
        }

        try
        {
            LdapDN newDn = new LdapDN( name );

            Enumeration<Rdn> rdns = newDn.getAllRdn();

            // Loop on all RDNs
            while ( rdns.hasMoreElements() )
            {
                Rdn rdn = rdns.nextElement();
                String upName = rdn.getUpName();
                rdnOidToName( rdn, oidsMap );
                rdn.normalize();
                rdn.setUpName( upName );
            }

            newDn.normalizeInternal();
            newDn.normalized = true;

            return newDn;
        }
        catch ( NamingException ne )
        {
            throw new InvalidNameException( ne.getMessage() );
        }
    }


    /**
     * Parse a buffer and checks that it is a valid DN <br>
     * <p>
     * &lt;distinguishedName&gt; ::= &lt;name&gt; | e <br>
     * &lt;name&gt; ::= &lt;name-component&gt; &lt;name-components&gt; <br>
     * &lt;name-components&gt; ::= &lt;spaces&gt; &lt;separator&gt;
     * &lt;spaces&gt; &lt;name-component&gt; &lt;name-components&gt; | e <br>
     * </p>
     *
     * @param bytes The byte buffer that contains the DN.
     * @throws InvalidNameException if the buffer does not contains a valid DN.
     */
    public LdapDN( byte[] bytes ) throws InvalidNameException
    {
        upName = StringTools.utf8ToString( bytes );
        LdapDnParser.parseInternal( bytes, rdns );
        this.normName = toNormName();
        normalized = false;
    }


    /**
     * Normalize the DN by triming useless spaces and lowercasing names.
     */
    void normalizeInternal()
    {
        normName = toNormName();
    }


    /**
     * Build the normalized DN as a String,
     *
     * @return A String representing the normalized DN
     */
    public String toNormName()
    {
        if ( rdns.size() == 0 )
        {
            bytes = null;
            return "";
        }
        else
        {
            StringBuffer sb = new StringBuffer();
            boolean isFirst = true;

            for ( Rdn rdn : rdns )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ',' );
                }

                sb.append( rdn );
            }

            String newNormName = sb.toString();

            if ( ( normName == null ) || !normName.equals( newNormName ) )
            {
                bytes = StringTools.getBytesUtf8( newNormName );
                normName = newNormName;
            }

            return normName;
        }
    }


    /**
     * Return the normalized DN as a String. It returns the same value as the
     * getNormName method
     *
     * @return A String representing the normalized DN
     */
    public String toString()
    {
        return normName == null ? "" : normName;
    }


    /**
     * Return the User Provided DN as a String,
     *
     * @return A String representing the User Provided DN
     */
    private String toUpName()
    {
        if ( rdns.size() == 0 )
        {
            upName = "";
        }
        else
        {
            StringBuffer sb = new StringBuffer();
            boolean isFirst = true;

            for ( Rdn rdn : rdns )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ',' );
                }

                sb.append( rdn.getUpName() );
            }

            upName = sb.toString();
        }

        return upName;
    }


    /**
     * Return the User Provided prefix representation of the DN starting at the
     * posn position.
     *
     * If posn = 0, return an empty string.
     *
     * for DN : sn=smith, dc=apache, dc=org
     * getUpname(0) -> ""
     * getUpName(1) -> "dc=org"
     * getUpname(3) -> "sn=smith, dc=apache, dc=org"
     * getUpName(4) -> ArrayOutOfBoundException
     *
     * Warning ! The returned String is not exactly the
     * user provided DN, as spaces before and after each RDNs have been trimmed.
     *
     * @param posn
     *            The starting position
     * @return The truncated DN
     */
    private String getUpNamePrefix( int posn )
    {
        if ( posn == 0 )
        {
            return "";
        }

        if ( posn > rdns.size() )
        {
            String message = "Impossible to get the position " + posn + ", the DN only has " + rdns.size() + " RDNs";
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        int start = rdns.size() - posn;
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;

        for ( int i = start; i < rdns.size(); i++ )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ',' );
            }

            sb.append( rdns.get( i ).getUpName() );
        }

        return sb.toString();
    }


    /**
     * Return the User Provided suffix representation of the DN starting at the
     * posn position.
     * If posn = 0, return an empty string.
     *
     * for DN : sn=smith, dc=apache, dc=org
     * getUpname(0) -> "sn=smith, dc=apache, dc=org"
     * getUpName(1) -> "sn=smith, dc=apache"
     * getUpname(3) -> "sn=smith"
     * getUpName(4) -> ""
     *
     * Warning ! The returned String is not exactly the user
     * provided DN, as spaces before and after each RDNs have been trimmed.
     *
     * @param posn The starting position
     * @return The truncated DN
     */
    private String getUpNameSuffix( int posn )
    {
        if ( posn > rdns.size() )
        {
            return "";
        }

        int end = rdns.size() - posn;
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;

        for ( int i = 0; i < end; i++ )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ',' );
            }

            sb.append( rdns.get( i ).getUpName() );
        }

        return sb.toString();
    }


    /**
     * Gets the hash code of this name.
     *
     * @see java.lang.Object#hashCode()
     * @return the instance hash code
     */
    public int hashCode()
    {
        int result = 37;

        for ( Rdn rdn : rdns )
        {
            result = result * 17 + rdn.hashCode();
        }

        return result;
    }


    /**
     * Get the initial DN (without normalization)
     *
     * @return The DN as a String
     */
    public String getUpName()
    {
        return ( upName == null ? "" : upName );
    }


    /**
     * Sets the up name.
     * 
     * @param upName the new up name
     */
    void setUpName( String upName )
    {
        this.upName = upName;
    }


    /**
     * Get the initial DN (without normalization)
     *
     * @return The DN as a String
     */
    public String getNormName()
    {
        return ( normName == null ? "" : normName );
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return rdns.size();
    }


    /**
     * Get the number of bytes necessary to store this DN

     * @param dn The DN.
     * @return A integer, which is the size of the UTF-8 byte array
     */
    public static int getNbBytes( Name dn )
    {
        LdapDN ldapDn = ( LdapDN ) dn;
        return ldapDn.bytes == null ? 0 : ldapDn.bytes.length;
    }


    /**
     * Get an UTF-8 representation of the normalized form of the DN
     * 
     * @param dn The DN.
     * @return A byte[] representation of the DN
     */
    public static byte[] getBytes( LdapDN dn )
    {
        return dn == null ? null : dn.bytes;
    }


    /**
     * {@inheritDoc}
     */
    public boolean startsWith( Name name )
    {
        if ( name == null )
        {
            return true;
        }
        else if ( name instanceof LdapDN )
        {
            LdapDN nameDN = ( LdapDN ) name;

            if ( nameDN.size() == 0 )
            {
                return true;
            }

            if ( nameDN.size() > size() )
            {
                // The name is longer than the current LdapDN.
                return false;
            }

            // Ok, iterate through all the RDN of the name,
            // starting a the end of the current list.

            for ( int i = nameDN.size() - 1; i >= 0; i-- )
            {
                Rdn nameRdn = nameDN.rdns.get( nameDN.rdns.size() - i - 1 );
                Rdn ldapRdn = rdns.get( rdns.size() - i - 1 );

                if ( nameRdn.compareTo( ldapRdn ) != 0 )
                {
                    return false;
                }
            }

            return true;
        }
        else
        {
            if ( name.size() == 0 )
            {
                return true;
            }

            if ( name.size() > size() )
            {
                // The name is longer than the current LdapDN.
                return false;
            }

            // Ok, iterate through all the RDN of the name,
            // starting a the end of the current list.
            int starting = size() - name.size();

            for ( int i = name.size() - 1; i >= 0; i-- )
            {
                Rdn ldapRdn = rdns.get( i + starting );
                Rdn nameRdn = null;

                try
                {
                    nameRdn = new Rdn( name.get( name.size() - i - 1 ) );
                }
                catch ( InvalidNameException e )
                {
                    LOG.error( "Failed to parse RDN for name " + name.toString(), e );
                    return false;
                }

                if ( nameRdn.compareTo( ldapRdn ) != 0 )
                {
                    return false;
                }
            }

            return true;
        }
    }


    /*
     * Determines whether this name ends with a specified suffix. A name
     * <tt>name</tt> is a suffix if it is equal to
     * <tt>getSuffix(size()-name.size())</tt>.
     *
     * Be aware that for a specific
     * DN like : cn=xxx, ou=yyy the endsWith method will return true with
     * cn=xxx, and false with ou=yyy
     *
     * @param name
     *            the name to check
     * @return true if <tt>name</tt> is a suffix of this name, false otherwise
     */
    /**
     * {@inheritDoc}
     */
    public boolean endsWith( Name name )
    {
        if ( name instanceof LdapDN )
        {
            LdapDN nameDN = ( LdapDN ) name;

            if ( nameDN.size() == 0 )
            {
                return true;
            }

            if ( nameDN.size() > size() )
            {
                // The name is longer than the current LdapDN.
                return false;
            }

            // Ok, iterate through all the RDN of the name
            for ( int i = 0; i < nameDN.size(); i++ )
            {
                Rdn nameRdn = nameDN.rdns.get( i );
                Rdn ldapRdn = rdns.get( i );

                if ( nameRdn.compareTo( ldapRdn ) != 0 )
                {
                    return false;
                }
            }

            return true;
        }
        else
        {
            // We don't accept a Name which is not a LdapName
            return name == null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
    {
        return ( rdns.size() == 0 );
    }


    /**
     * {@inheritDoc}
     */
    public String get( int posn )
    {
        if ( rdns.size() == 0 )
        {
            return "";
        }
        else
        {
            Rdn rdn = rdns.get( rdns.size() - posn - 1 );

            return rdn.toString();
        }
    }


    /**
     * Retrieves a component of this name.
     *
     * @param posn
     *            the 0-based index of the component to retrieve. Must be in the
     *            range [0,size()).
     * @return the component at index posn
     * @throws ArrayIndexOutOfBoundsException
     *             if posn is outside the specified range
     */
    public Rdn getRdn( int posn )
    {
        if ( rdns.size() == 0 )
        {
            return null;
        }
        else
        {
            Rdn rdn = rdns.get( rdns.size() - posn - 1 );

            return rdn;
        }
    }


    /**
     * Retrieves the last (leaf) component of this name.
     *
     * @return the last component of this DN
     */
    public Rdn getRdn()
    {
        if ( rdns.size() == 0 )
        {
            return null;
        }
        else
        {
            return rdns.get( 0 );
        }
    }


    /**
     * Retrieves all the components of this name.
     *
     * @return All the components
     */
    public List<Rdn> getRdns()
    {
        List<Rdn> newRdns = new ArrayList<Rdn>();

        // We will clone the list, to avoid user modifications
        for ( Rdn rdn : rdns )
        {
            newRdns.add( ( Rdn ) rdn.clone() );
        }

        return newRdns;
    }


    /**
     * {@inheritDoc}
     */
    public Enumeration<String> getAll()
    {
        /*
         * Note that by accessing the name component using the get() method on
         * the name rather than get() on the list we are reading components from
         * right to left with increasing index values. LdapName.get() does the
         * index translation on m_list for us.
         */
        return new Enumeration<String>()
        {
            private int pos;


            public boolean hasMoreElements()
            {
                return pos < rdns.size();
            }


            public String nextElement()
            {
                if ( pos >= rdns.size() )
                {
                    LOG.error( "Exceeded number of elements in the current object" );
                    throw new NoSuchElementException();
                }

                Rdn rdn = rdns.get( rdns.size() - pos - 1 );
                pos++;
                return rdn.toString();
            }
        };
    }


    /**
     * Retrieves the components of this name as an enumeration of strings. The
     * effect on the enumeration of updates to this name is undefined. If the
     * name has zero components, an empty (non-null) enumeration is returned.
     * This starts at the root (rightmost) rdn.
     *
     * @return an enumeration of the components of this name, as Rdn
     */
    public Enumeration<Rdn> getAllRdn()
    {
        /*
         * Note that by accessing the name component using the get() method on
         * the name rather than get() on the list we are reading components from
         * right to left with increasing index values. LdapName.get() does the
         * index translation on m_list for us.
         */
        return new Enumeration<Rdn>()
        {
            private int pos;


            public boolean hasMoreElements()
            {
                return pos < rdns.size();
            }


            public Rdn nextElement()
            {
                if ( pos >= rdns.size() )
                {
                    LOG.error( "Exceeded number of elements in the current object" );
                    throw new NoSuchElementException();
                }

                Rdn rdn = rdns.get( rdns.size() - pos - 1 );
                pos++;
                return rdn;
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    public Name getPrefix( int posn )
    {
        if ( rdns.size() == 0 )
        {
            return EMPTY_LDAPDN;
        }

        if ( ( posn < 0 ) || ( posn > rdns.size() ) )
        {
            String message = "The posn(" + posn + ") should be in the range [0, " + rdns.size() + "]";
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        LdapDN newLdapDN = new LdapDN();

        for ( int i = rdns.size() - posn; i < rdns.size(); i++ )
        {
            // Don't forget to clone the rdns !
            newLdapDN.rdns.add( ( Rdn ) rdns.get( i ).clone() );
        }

        newLdapDN.normName = newLdapDN.toNormName();
        newLdapDN.upName = getUpNamePrefix( posn );

        return newLdapDN;
    }


    /**
     * {@inheritDoc}
     */
    public Name getSuffix( int posn )
    {
        if ( rdns.size() == 0 )
        {
            return EMPTY_LDAPDN;
        }

        if ( ( posn < 0 ) || ( posn > rdns.size() ) )
        {
            String message = "The posn(" + posn + ") should be in the range [0, " + rdns.size() + "]";
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        LdapDN newLdapDN = new LdapDN();

        for ( int i = 0; i < size() - posn; i++ )
        {
            // Don't forget to clone the rdns !
            newLdapDN.rdns.add( ( Rdn ) rdns.get( i ).clone() );
        }

        newLdapDN.normName = newLdapDN.toNormName();
        newLdapDN.upName = getUpNameSuffix( posn );

        return newLdapDN;
    }


    /**
     * Adds the components of a name -- in order -- at a specified position
     * within this name. Components of this name at or after the index of the
     * first new component are shifted up (away from 0) to accommodate the new
     * components. Compoenents are supposed to be normalized.
     *
     * @param posn the index in this name at which to add the new components.
     *            Must be in the range [0,size()]. Note this is from the opposite end as rnds.get(posn)
     * @param name the components to add
     * @return the updated name (not a new one)
     * @throws ArrayIndexOutOfBoundsException
     *             if posn is outside the specified range
     * @throws InvalidNameException
     *             if <tt>n</tt> is not a valid name, or if the addition of
     *             the components would violate the syntax rules of this name
     */
    public Name addAllNormalized( int posn, Name name ) throws InvalidNameException
    {
        if ( name instanceof LdapDN )
        {
            LdapDN dn = (LdapDN)name;
            
            if ( ( dn == null ) || ( dn.size() == 0 ) )
            {
                return this;
            }

            // Concatenate the rdns
            rdns.addAll( size() - posn, dn.rdns );

            if ( StringTools.isEmpty( normName ) )
            {
                normName = dn.normName;
                bytes = dn.bytes;
                upName = dn.upName;
            }
            else
            {
                normName = dn.normName + "," + normName;
                bytes = StringTools.getBytesUtf8( normName );
                upName = dn.upName + "," + upName;
            }
        }
        else
        {
            if ( ( name == null ) || ( name.size() == 0 ) )
            {
                return this;
            }

            for ( int i = name.size() - 1; i >= 0; i-- )
            {
                Rdn rdn = new Rdn( name.get( i ) );
                rdns.add( size() - posn, rdn );
            }

            normalizeInternal();
            toUpName();
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Name addAll( Name suffix ) throws InvalidNameException
    {
        addAll( rdns.size(), suffix );
        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public Name addAll( int posn, Name name ) throws InvalidNameException
    {
        if ( name instanceof LdapDN )
        {
            LdapDN dn = (LdapDN)name;
            
            if ( ( dn == null ) || ( dn.size() == 0 ) )
            {
                return this;
            }

            // Concatenate the rdns
            rdns.addAll( size() - posn, dn.rdns );

            // Regenerate the normalized name and the original string
            if ( this.isNormalized() && dn.isNormalized() )
            {
                if ( this.size() != 0 )
                {
                    normName = dn.getNormName() + "," + normName;
                    bytes = StringTools.getBytesUtf8( normName );
                    upName = dn.getUpName() + "," + upName;
                }
            }
            else
            {
                normalizeInternal();
                toUpName();
            }
        }
        else
        {
            if ( ( name == null ) || ( name.size() == 0 ) )
            {
                return this;
            }

            for ( int i = name.size() - 1; i >= 0; i-- )
            {
                Rdn rdn = new Rdn( name.get( i ) );
                rdns.add( size() - posn, rdn );
            }

            normalizeInternal();
            toUpName();
        }

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public Name add( String comp ) throws InvalidNameException
    {
        if ( comp.length() == 0 )
        {
            return this;
        }

        // We have to parse the nameComponent which is given as an argument
        Rdn newRdn = new Rdn( comp );

        rdns.add( 0, newRdn );
        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * Adds a single RDN to the (leaf) end of this name.
     *
     * @param newRdn the RDN to add
     * @return the updated name (not a new one)
     */
    public Name add( Rdn newRdn )
    {
        rdns.add( 0, newRdn );
        
        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * Adds a single RDN to a specific position.
     *
     * @param newRdn the RDN to add
     * @param pos The position where we want to add the Rdn
     * @return the updated name (not a new one)
     */
    public Name add( int pos, Rdn newRdn )
    {
        rdns.add( newRdn );
        
        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * Adds a single normalized RDN to the (leaf) end of this name.
     *
     * @param newRdn the RDN to add
     * @return the updated name (not a new one)
     */
    public Name addNormalized( Rdn newRdn )
    {
        rdns.add( 0, newRdn );
        
        // Avoid a call to the toNormName() method which
        // will iterate through all the rdns, when we only
        // have to build a new normName by using the current
        // RDN normalized name. The very same for upName.
        if (rdns.size() == 1 )
        {
            normName = newRdn.toString();
            upName = newRdn.getUpName();
        }
        else
        {
            normName = newRdn + "," + normName;
            upName = newRdn.getUpName() + "," + upName;
        }
        
        bytes = StringTools.getBytesUtf8( normName );

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public Name add( int posn, String comp ) throws InvalidNameException
    {
        if ( ( posn < 0 ) || ( posn > size() ) )
        {
            String message = "The posn(" + posn + ") should be in the range [0, " + rdns.size() + "]";
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        // We have to parse the nameComponent which is given as an argument
        Rdn newRdn = new Rdn( comp );

        int realPos = size() - posn;
        rdns.add( realPos, newRdn );

        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public Object remove( int posn ) throws InvalidNameException
    {
        if ( rdns.size() == 0 )
        {
            return EMPTY_LDAPDN;
        }

        if ( ( posn < 0 ) || ( posn >= rdns.size() ) )
        {
            String message = "The posn(" + posn + ") should be in the range [0, " + rdns.size() + "]";
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        int realPos = size() - posn - 1;
        Rdn rdn = rdns.remove( realPos );

        normalizeInternal();
        toUpName();

        return rdn;
    }


    /**
     * {@inheritDoc}
     */
    public Object clone()
    {
        try
        {
            LdapDN dn = ( LdapDN ) super.clone();
            dn.rdns = new ArrayList<Rdn>();

            for ( Rdn rdn : rdns )
            {
                dn.rdns.add( ( Rdn ) rdn.clone() );
            }

            return dn;
        }
        catch ( CloneNotSupportedException cnse )
        {
            LOG.error( "The clone operation has failed" );
            throw new Error( "Assertion failure : cannot clone the object" );
        }
    }


    /**
     * @see java.lang.Object#equals(java.lang.Object)
     * @return <code>true</code> if the two instances are equals
     */
    public boolean equals( Object obj )
    {
        if ( obj instanceof String )
        {
            return normName.equals( obj );
        }
        else if ( obj instanceof LdapDN )
        {
            LdapDN name = ( LdapDN ) obj;

            if ( name.size() != this.size() )
            {
                return false;
            }

            for ( int i = 0; i < this.size(); i++ )
            {
                if ( name.rdns.get( i ).compareTo( rdns.get( i ) ) != 0 )
                {
                    return false;
                }
            }

            // All components matched so we return true
            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo( Object obj )
    {
        if ( obj instanceof LdapDN )
        {
            LdapDN dn = ( LdapDN ) obj;

            if ( dn.size() != size() )
            {
                return size() - dn.size();
            }

            for ( int i = rdns.size(); i > 0; i-- )
            {
                Rdn rdn1 = rdns.get( i - 1 );
                Rdn rdn2 = dn.rdns.get( i - 1 );
                int res = rdn1.compareTo( rdn2 );

                if ( res != 0 )
                {
                    return res;
                }
            }

            return EQUAL;
        }
        else
        {
            return 1;
        }
    }


    private static AttributeTypeAndValue atavOidToName( AttributeTypeAndValue atav, Map<String, OidNormalizer> oidsMap )
        throws InvalidNameException, NamingException
    {
        String type = StringTools.trim( atav.getNormType() );

        if ( ( type.startsWith( "oid." ) ) || ( type.startsWith( "OID." ) ) )
        {
            type = type.substring( 4 );
        }

        if ( StringTools.isNotEmpty( type ) )
        {
            if ( oidsMap == null )
            {
                return atav;
            }
            else
            {
                OidNormalizer oidNormalizer = oidsMap.get( type );

                if ( oidNormalizer != null )
                {
                    return new AttributeTypeAndValue( atav.getUpType(), oidNormalizer.getAttributeTypeOid(), 
                            atav.getUpValue(),
                            oidNormalizer.getNormalizer().normalize( atav.getNormValue() ) );

                }
                else
                {
                    // We don't have a normalizer for this OID : just do nothing.
                    return atav;
                }
            }
        }
        else
        {
            // The type is empty : this is not possible...
            LOG.error( "Empty type not allowed in a DN" );
            throw new InvalidNameException( "Empty type not allowed in a DN" );
        }

    }

    /**
     * This private method is used to normalize the value, when we have found a normalizer.
     * This method deals with RDN having one single ATAV.
     * 
     * @param rdn the RDN we want to normalize. It will contain the resulting normalized RDN
     * @param oidNormalizer the normalizer to use for the RDN
     * @throws NamingException If something went wrong.
     */
    private static void oidNormalize( Rdn rdn, OidNormalizer oidNormalizer ) throws NamingException
    {
        Object upValue = rdn.getUpValue();
        String upType = rdn.getUpType();
        rdn.clear();
        Object normStringValue = DefaultStringNormalizer.normalizeString( ( String ) upValue );
        Object normValue = oidNormalizer.getNormalizer().normalize( normStringValue );

        rdn.addAttributeTypeAndValue( upType, oidNormalizer.getAttributeTypeOid(), upValue, normValue );
    }

    /**
     * Transform a RDN by changing the value to its OID counterpart and
     * normalizing the value accordingly to its type.
     *
     * @param rdn The RDN to modify.
     * @param oidsMap The map of all existing oids and normalizer.
     * @throws InvalidNameException If the RDN is invalid.
     * @throws NamingException If something went wrong.
     */
    private static void rdnOidToName( Rdn rdn, Map<String, OidNormalizer> oidsMap ) throws InvalidNameException,
        NamingException
    {
        if ( rdn.getNbAtavs() > 1 )
        {
            // We have more than one ATAV for this RDN. We will loop on all
            // ATAVs
            Rdn rdnCopy = ( Rdn ) rdn.clone();
            rdn.clear();

            for ( AttributeTypeAndValue val:rdnCopy )
            {
                AttributeTypeAndValue newAtav = atavOidToName( val, oidsMap );
                rdn.addAttributeTypeAndValue( val.getUpType(), newAtav.getNormType(), val.getUpValue(), newAtav.getNormValue() );
            }

        }
        else
        {
            String type = rdn.getNormType();

            if ( StringTools.isNotEmpty( type ) )
            {
                if ( oidsMap == null )
                {
                    return;
                }
                else
                {
                    OidNormalizer oidNormalizer = oidsMap.get( type );

                    if ( oidNormalizer != null )
                    {
                        oidNormalize( rdn, oidNormalizer );
                    }
                    else
                    {
                        // May be the oidNormalizer was null because the type starts with OID
                        if ( ( type.startsWith( "oid." ) ) || ( type.startsWith( "OID." ) ) )
                        {
                            type = type.substring( 4 );
                            oidNormalizer = oidsMap.get( type );
                            
                            if ( oidNormalizer != null )
                            {
                                // Ok, just normalize after having removed the 4 first chars
                                oidNormalize( rdn, oidNormalizer );
                            }
                            else
                            {
                                // We don't have a normalizer for this OID : just do
                                // nothing.
                                return;
                            }
                        }
                        else
                        {
                            // We don't have a normalizer for this OID : just do
                            // nothing.
                            return;
                        }
                    }
                }
            }
            else
            {
                // The type is empty : this is not possible...
                LOG.error( "We should not have an empty DN" );
                throw new InvalidNameException( "Empty type not allowed in a DN" );
            }
        }
    }


    /**
     * Change the internal DN, using the OID instead of the first name or other
     * aliases. As we still have the UP name of each RDN, we will be able to
     * provide both representation of the DN. example : dn: 2.5.4.3=People,
     * dc=example, domainComponent=com will be transformed to : 2.5.4.3=People,
     * 0.9.2342.19200300.100.1.25=example, 0.9.2342.19200300.100.1.25=com 
     * because 2.5.4.3 is the OID for cn and dc is the first
     * alias of the couple of aliases (dc, domaincomponent), which OID is 
     * 0.9.2342.19200300.100.1.25. 
     * This is really important do have such a representation, as 'cn' and 
     * 'commonname' share the same OID.
     * 
     * @param dn The DN to transform.
     * @param oidsMap The mapping between names and oids.
     * @return A normalized form of the DN.
     * @throws NamingException If something went wrong.
     */
    public static LdapDN normalize( LdapDN dn, Map<String, OidNormalizer> oidsMap ) throws NamingException
    {
        if ( ( dn == null ) || ( dn.size() == 0 ) || ( oidsMap == null ) || ( oidsMap.size() == 0 ) )
        {
            return dn;
        }

        Enumeration<Rdn> rdns = dn.getAllRdn();

        // Loop on all RDNs
        while ( rdns.hasMoreElements() )
        {
            Rdn rdn = rdns.nextElement();
            String upName = rdn.getUpName();
            rdnOidToName( rdn, oidsMap );
            rdn.normalize();
            rdn.setUpName( upName );
        }

        dn.normalizeInternal();

        dn.normalized = true;
        return dn;
    }


    /**
     * Change the internal DN, using the OID instead of the first name or other
     * aliases. As we still have the UP name of each RDN, we will be able to
     * provide both representation of the DN. example : dn: 2.5.4.3=People,
     * dc=example, domainComponent=com will be transformed to : 2.5.4.3=People,
     * 0.9.2342.19200300.100.1.25=example, 0.9.2342.19200300.100.1.25=com 
     * because 2.5.4.3 is the OID for cn and dc is the first
     * alias of the couple of aliases (dc, domaincomponent), which OID is 
     * 0.9.2342.19200300.100.1.25. 
     * This is really important do have such a representation, as 'cn' and 
     * 'commonname' share the same OID.
     *
     * @param oidsMap The mapping between names and oids.
     * @throws NamingException If something went wrong.
     * @return The normalized DN
     */
    public LdapDN normalize( Map<String, OidNormalizer> oidsMap ) throws NamingException
    {
        if ( ( oidsMap == null ) || ( oidsMap.size() == 0 ) )
        {
            return this;
        }

        if ( size() == 0 )
        {
            normalized = true;
            return this;
        }

        Enumeration<Rdn> localRdns = getAllRdn();

        // Loop on all RDNs
        while ( localRdns.hasMoreElements() )
        {
            Rdn rdn = localRdns.nextElement();
            String localUpName = rdn.getUpName();
            rdnOidToName( rdn, oidsMap );
            rdn.normalize();
            rdn.setUpName( localUpName );
        }

        normalizeInternal();
        normalized = true;
        return this;
    }


    /**
     * Check if a DistinguishedName is syntactically valid.
     *
     * @param dn The DN to validate
     * @return <code>true></code> if the DN is valid, <code>false</code>
     * otherwise
     */
    public static boolean isValid( String dn )
    {
        return LdapDnParser.validateInternal( dn );
    }

    /**
     * Tells if the DN has already been normalized or not
     *
     * @return <code>true</code> if the DN is already normalized.
     */
    public boolean isNormalized()
    {
        return normalized;
    }


    /**
     * @see Externalizable#readExternal(ObjectInput)<p>
     * 
     * We have to store a DN data efficiently. Here is the structure :
     * 
     * <li>upName</li> The User provided DN<p>
     * <li>normName</li> May be null if the normName is equaivalent to 
     * the upName<p>
     * <li>rdns</li> The rdn's List.<p>
     * 
     * for each rdn :
     * <li>call the RDN write method</li>
     *
     *@param out The stream in which the DN will be serialized
     *@throws IOException If the serialization fail
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        if ( upName == null )
        {
            String message = "Cannot serialize a NULL DN";
            LOG.error( message );
            throw new IOException( message );
        }
        
        // Write the UPName
        out.writeUTF( upName );
        
        // Write the NormName if different
        if ( isNormalized() )
        {
            if ( upName.equals( normName ) )
            {
                out.writeUTF( "" );
            }
            else
            {
                out.writeUTF( normName );
            }
        }
        else
        {
            String message = "The DN should have been normalized before being serialized";
            LOG.error( message );
            throw new IOException( message );
        }
        
        // Should we store the byte[] ???
        
        // Write the RDNs. Is it's null, the number will be -1. 
        out.writeInt( rdns.size() );

        // Loop on the RDNs
        for ( Rdn rdn:rdns )
        {
            out.writeObject( rdn );
        }
    }


    /**
     * @see Externalizable#readExternal(ObjectInput)
     * 
     * We read back the data to create a new LdapDN. The structure 
     * read is exposed in the {@link LdapDN#writeExternal(ObjectOutput)} 
     * method<p>
     * 
     * @param in The stream from which the DN is read
     * @throws IOException If the stream can't be read
     * @throws ClassNotFoundException If the RDN can't be created 
     */
    public void readExternal( ObjectInput in ) throws IOException , ClassNotFoundException
    {
        // Read the UPName
        upName = in.readUTF();
        
        // Read the NormName
        normName = in.readUTF();
        
        if ( normName.length() == 0 )
        {
            // As the normName is equal to the upName,
            // we didn't saved the nbnormName on disk.
            // restore it by copying the upName.
            normName = upName;
        }
        
        // A serialized DN is always normalized.
        normalized = true;
            
        // Should we read the byte[] ???
        bytes = StringTools.getBytesUtf8( upName );
        
        // Read the RDNs. Is it's null, the number will be -1.
        int nbRdns = in.readInt();
        rdns = new ArrayList<Rdn>( nbRdns );
        
        for ( int i = 0; i < nbRdns; i++ )
        {
            Rdn rdn = (Rdn)in.readObject();
            rdns.add( rdn );
        }
    }
}
