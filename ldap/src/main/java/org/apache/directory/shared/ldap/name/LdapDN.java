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


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.DefaultStringNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
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
 */
public class LdapDN implements Name
{
   /** The LoggerFactory used by this class */
   protected static final Logger log = LoggerFactory.getLogger( LdapDN.class );

   /**
    * Declares the Serial Version Uid.
    *
    * @see <a
    *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
    *      Declare Serial Version Uid</a>
    */
   private static final long serialVersionUID = 1L;

   /** Value returned by the compareTo method if values are not equals */
   public final static int NOT_EQUALS = -1;

   /** Value returned by the compareTo method if values are equals */
   public final static int EQUALS = 0;

   // ~ Static fields/initializers
   // -----------------------------------------------------------------
   /** The RDNs that are elements of the DN */
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
       super();
       upName = "";
       normName = "";
   }


   /**
    * Transduces, or copies a Name to an LdapDN.
    *
    * @param name composed of String name components.
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
   }


   /**
    * Creates an ldap name using a list of NameComponents. Each NameComponent
    * is a String
    *
    * @param list of String name components.
    */
   LdapDN( List list ) throws InvalidNameException
   {
       if ( ( list != null ) && ( list.size() != 0 ) )
       {
           Iterator nameComponents = list.iterator();

           while ( nameComponents.hasNext() )
           {
               String nameComponent = ( String ) nameComponents.next();
               add( 0, nameComponent );
           }
       }
   }


   /**
    * Creates an ldap name using a list of name components.
    *
    * @param nameComponents
    *            List of String name components.
    */
   LdapDN( Iterator nameComponents ) throws InvalidNameException
   {
       if ( nameComponents != null )
       {
           while ( nameComponents.hasNext() )
           {
               String nameComponent = ( String ) nameComponents.next();
               add( 0, nameComponent );
           }
       }
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
    * @param upName
    *            The String that contains the DN
    * @exception InvalidNameException is thrown if the buffer does not
    *                contains a valid DN.
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
       this.upName = upName;
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
    * @param bytes
    *            The byte buffer that contains the DN
    * @exception A
    *                InvalidNameException is thrown if the buffer does not
    *                contains a valid DN.
    */
   public LdapDN(byte[] bytes) throws InvalidNameException
   {
       try
       {
           upName = new String( bytes, "UTF-8" );
           LdapDnParser.parseInternal( upName, rdns );
           this.normName = toNormName();
       }
       catch ( UnsupportedEncodingException uee )
       {
           log.error( "The byte array is not an UTF-8 encoded Unicode String : " + uee.getMessage() );
           throw new InvalidNameException( "The byte array is not an UTF-8 encoded Unicode String : "
               + uee.getMessage() );
       }
   }


   /**
    * Normalize the DN by triming useless spaces and lowercasing names.
    *
    * @return a normalized form of the DN
    */
   private void normalizeInternal()
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
       if ( ( rdns == null ) || ( rdns.size() == 0 ) )
       {
           bytes = null;
           return "";
       }
       else
       {
           StringBuffer sb = new StringBuffer();
           boolean isFirst = true;

           for ( Rdn rdn:rdns )
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

           if ( ( normName ==  null ) || !normName.equals( newNormName ) )
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
       if ( ( rdns == null ) || ( rdns.size() == 0 ) )
       {
           upName = "";
       }
       else
       {
           StringBuffer sb = new StringBuffer();
           boolean isFirst = true;

           for ( Rdn rdn:rdns )
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
           log.error( message );
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
    * Gets the hashcode of this name.
    *
    * @see java.lang.Object#hashCode()
    */
   public int hashCode()
   {
       int result = 17;

       if ( ( rdns != null ) && ( rdns.size() == 0 ) )
       {
           for ( Rdn rdn:rdns )
           {
               result = result * 37 + rdn.hashCode();
           }
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
    * Get the initial DN (without normalization)
    *
    * @return The DN as a String
    */
    public String getNormName()
    {
        return ( normName == null ? "" : normName );
    }

   /**
    * Get the number of NameComponent conatained in this LdapDN
    *
    * @return The number of NameComponent conatained in this LdapDN
    */
   public int size()
   {
       return rdns.size();
   }


   /**
    * Get the number of bytes necessary to store this DN
    *
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
    * @return A byte[] representation of the DN
    */
   public static byte[] getBytes( LdapDN dn )
   {
       return dn == null ? null : dn.bytes;
   }


   /**
    * Determines whether this name starts with a specified prefix. A name
    * <tt>name</tt> is a prefix if it is equal to
    * <tt>getPrefix(name.size())</tt>.
    *
    * Be aware that for a specific DN like :
    * cn=xxx, ou=yyy
    * the startsWith method will return true with ou=yyy, and
    * false with cn=xxx
    *
    * @param name
    *            the name to check
    * @return true if <tt>name</tt> is a prefix of this name, false otherwise
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

           for ( int i = name.size() - 1; i >= 0; i-- )
           {
               Rdn ldapRdn = rdns.get( rdns.size() - i - 1 );
               Rdn nameRdn = null;
               
               try
               {
                   nameRdn = new Rdn( name.get( name.size() - i - 1 ) );
               }
               catch ( InvalidNameException e )
               {
                   e.printStackTrace();
                   log.error( "Failed to parse RDN for name " + name.toString(), e );
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


   /**
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
    * Determines whether this name is empty. An empty name is one with zero
    * components.
    *
    * @return true if this name is empty, false otherwise
    */
   public boolean isEmpty()
   {
       return ( rdns.size() == 0 );
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
    * Retrieves the last component of this name.
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
       for ( Rdn rdn:rdns )
       {
           newRdns.add( (Rdn)rdn.clone() );
       }

       return newRdns;
   }


   /**
    * Retrieves the components of this name as an enumeration of strings. The
    * effect on the enumeration of updates to this name is undefined. If the
    * name has zero components, an empty (non-null) enumeration is returned.
    *
    * @return an enumeration of the components of this name, each as string
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
                   log.error( "Exceeded number of elements in the current object" );
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
                   log.error( "Exceeded number of elements in the current object" );
                   throw new NoSuchElementException();
               }

               Rdn rdn = rdns.get( rdns.size() - pos - 1 );
               pos++;
               return rdn;
           }
       };
   }


   /**
    * Creates a name whose components consist of a prefix of the components of
    * this name. Subsequent changes to this name will not affect the name that
    * is returned and vice versa.
    *
    * @param posn
    *            the 0-based index of the component at which to stop. Must be
    *            in the range [0,size()].
    * @return a name consisting of the components at indexes in the range
    *         [0,posn].
    * @throws ArrayIndexOutOfBoundsException
    *             if posn is outside the specified range
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
           log.error( message );
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
    * Creates a name whose components consist of a suffix of the components in
    * this name. Subsequent changes to this name do not affect the name that is
    * returned and vice versa.
    *
    * @param posn
    *            the 0-based index of the component at which to start. Must be
    *            in the range [0,size()].
    * @return a name consisting of the components at indexes in the range
    *         [posn,size()]. If posn is equal to size(), an empty name is
    *         returned.
    * @throws ArrayIndexOutOfBoundsException
    *             if posn is outside the specified range
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
           log.error( message );
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
    * Adds the components of a name -- in order -- to the end of this name.
    *
    * @param suffix
    *            the components to add
    * @return the updated name (not a new one)
    * @throws InvalidNameException
    *             if <tt>suffix</tt> is not a valid name, or if the addition
    *             of the components would violate the syntax rules of this name
    */
   public Name addAll( Name suffix ) throws InvalidNameException
   {
       addAll( rdns.size(), suffix );
       normalizeInternal();
       toUpName();

       return this;
   }


   /**
    * Adds the components of a name -- in order -- at a specified position
    * within this name. Components of this name at or after the index of the
    * first new component are shifted up (away from 0) to accommodate the new
    * components.
    *
    * @param name
    *            the components to add
    * @param posn
    *            the index in this name at which to add the new components.
    *            Must be in the range [0,size()].
    * @return the updated name (not a new one)
    * @throws ArrayIndexOutOfBoundsException
    *             if posn is outside the specified range
    * @throws InvalidNameException
    *             if <tt>n</tt> is not a valid name, or if the addition of
    *             the components would violate the syntax rules of this name
    */
   public Name addAll( int posn, Name name ) throws InvalidNameException
   {
       if ( name instanceof LdapDN )
       {
           if ( ( name == null ) || ( name.size() == 0 ) )
           {
               return this;
           }

           // Concatenate the rdns
           rdns.addAll( size() - posn, ( ( LdapDN ) name ).rdns );

           // Regenerate the normalized name and the original string
           normalizeInternal();
           toUpName();
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
    * Adds a single component to the end of this name.
    *
    * @param comp
    *            the component to add
    * @return the updated name (not a new one)
    * @throws InvalidNameException
    *             if adding <tt>comp</tt> would violate the syntax rules of
    *             this name
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
    * Adds a single RDN to the end of this name.
    *
    * @param newRdn
    *            the RDN to add
    * @return the updated name (not a new one)
    * @throws InvalidNameException
    *             if adding <tt>RDN</tt> would violate the syntax rules of
    *             this name
    */
   public Name add( Rdn newRdn )
   {
       rdns.add( 0, newRdn );
       normalizeInternal();
       toUpName();

       return this;
   }

   /**
    * Adds a single component at a specified position within this name.
    * Components of this name at or after the index of the new component are
    * shifted up by one (away from index 0) to accommodate the new component.
    *
    * @param comp
    *            the component to add
    * @param posn
    *            the index at which to add the new component. Must be in the
    *            range [0,size()].
    * @return the updated name (not a new one)
    * @throws ArrayIndexOutOfBoundsException
    *             if posn is outside the specified range
    * @throws InvalidNameException
    *             if adding <tt>comp</tt> would violate the syntax rules of
    *             this name
    */
   public Name add( int posn, String comp ) throws InvalidNameException
   {
       if ( ( posn < 0 ) || ( posn > size() ) )
       {
           String message = "The posn(" + posn + ") should be in the range [0, " + rdns.size() + "]";
           log.error( message );
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
    * Removes a component from this name. The component of this name at the
    * specified position is removed. Components with indexes greater than this
    * position are shifted down (toward index 0) by one.
    *
    * @param posn
    *            the index of the component to remove. Must be in the range
    *            [0,size()).
    * @return the component removed (a String)
    * @throws ArrayIndexOutOfBoundsException
    *             if posn is outside the specified range
    * @throws InvalidNameException
    *             if deleting the component would violate the syntax rules of
    *             the name
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
           log.error( message );
           throw new ArrayIndexOutOfBoundsException( message );
       }

       int realPos = size() - posn - 1;
       Rdn rdn = rdns.remove( realPos );

       normalizeInternal();
       toUpName();

       return rdn;
   }


   /**
    * Generates a new copy of this name. Subsequent changes to the components
    * of this name will not affect the new copy, and vice versa.
    *
    * @return a copy of this name
    * @see Object#clone()
    */
   public Object clone()
   {
       try
       {
           LdapDN dn = ( LdapDN ) super.clone();
           dn.rdns = new ArrayList<Rdn>();

           for ( Rdn rdn:rdns )
           {
               dn.rdns.add( ( Rdn ) rdn.clone() );
           }

           return dn;
       }
       catch ( CloneNotSupportedException cnse )
       {
           log.error( "The clone operation has failed" );
           throw new Error( "Assertion failure : cannot clone the object" );
       }
   }


   /**
    * @see java.lang.Object#equals(java.lang.Object)
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
    * Compares this name with another name for order. Returns a negative
    * integer, zero, or a positive integer as this name is less than, equal to,
    * or greater than the given name.
    * <p>
    * As with <tt>Object.equals()</tt>, the notion of ordering for names
    * depends on the class that implements this interface. For example, the
    * ordering may be based on lexicographical ordering of the name components.
    * Specific attributes of the name, such as how it treats case, may affect
    * the ordering. In general, two names of different classes may not be
    * compared.
    *
    * @param obj
    *            the non-null object to compare against.
    * @return a negative integer, zero, or a positive integer as this name is
    *         less than, equal to, or greater than the given name
    * @throws ClassCastException
    *             if obj is not a <tt>Name</tt> of a type that may be
    *             compared with this name
    * @see Comparable#compareTo(Object)
    */
   public int compareTo( Object obj )
   {
       if ( obj instanceof LdapDN )
       {
           LdapDN ldapDN = ( LdapDN ) obj;

           if ( ldapDN.size() != size() )
           {
               return size() - ldapDN.size();
           }

           for ( int i = rdns.size(); i > 0; i-- )
           {
               Rdn rdn1 = rdns.get( i - 1 );
               Rdn rdn2 = ldapDN.rdns.get( i - 1 );
               int res = rdn1.compareTo( rdn2 );

               if ( res != 0 )
               {
                   return res;
               }
           }

           return EQUALS;
       }
       else
       {
           return 1;
       }
   }


   private static AttributeTypeAndValue atavOidToName( AttributeTypeAndValue atav, Map oidsMap )
       throws InvalidNameException, NamingException
   {
       String type = StringTools.trim( atav.getType() );

       if ( ( type.startsWith( "oid." ) ) || ( type.startsWith( "OID." ) ) )
       {
           type = type.substring( 4 );
       }

       if ( StringTools.isNotEmpty( StringTools.lowerCase( type ) ) )
       {
           if ( oidsMap == null )
           {
               return atav;
           }
           else
           {
               OidNormalizer oidNormalizer = ( OidNormalizer ) oidsMap.get( type );

               if ( oidNormalizer != null )
               {
                   return new AttributeTypeAndValue( oidNormalizer.getAttributeTypeOid(), oidNormalizer.getNormalizer()
                       .normalize( atav.getValue() ) );

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
           log.error( "Empty type not allowed in a DN" );
           throw new InvalidNameException( "Empty type not allowed in a DN" );
       }

   }


   /**
    * Transform a RDN by changing the value to its OID counterpart and
    * normalizing the value accordingly to its type.
    *
    * @param rdn
    *            The RDN to modify
    * @param oidsMap
    *            The map of all existing oids and normalizer
    * @throws InvalidNameException
    *             If
    * @throws NamingException
    */
   private static void rdnOidToName( Rdn rdn, Map oidsMap ) throws InvalidNameException, NamingException
   {
       if ( rdn.getNbAtavs() > 1 )
       {
           // We have more than one ATAV for this RDN. We will loop on all
           // ATAVs
           Rdn rdnCopy = ( Rdn ) rdn.clone();
           rdn.clear();

           Iterator atavs = rdnCopy.iterator();

           while ( atavs.hasNext() )
           {
               Object val = atavs.next();
               AttributeTypeAndValue newAtav = atavOidToName( ( AttributeTypeAndValue ) val, oidsMap );
               rdn.addAttributeTypeAndValue( newAtav.getType(), newAtav.getValue() );
           }

       }
       else
       {
           String type = StringTools.trim( rdn.getType() );

           if ( ( type.startsWith( "oid." ) ) || ( type.startsWith( "OID." ) ) )
           {
               type = type.substring( 4 );
           }

           if ( StringTools.isNotEmpty( StringTools.lowerCase( type ) ) )
           {
               if ( oidsMap == null )
               {
                   return;
               }
               else
               {
                   OidNormalizer oidNormalizer = ( OidNormalizer ) oidsMap.get( type );

                   if ( oidNormalizer != null )
                   {
                       // Alex asks: Why clone here when we do not use the cloned copy?
                       Rdn rdnCopy = ( Rdn ) rdn.clone();
                       rdn.clear();
                       Object value = rdnCopy.getValue();
                       value = DefaultStringNormalizer.normalizeString( ( String ) value );

                       rdn.addAttributeTypeAndValue( oidNormalizer.getAttributeTypeOid(),
                           oidNormalizer.getNormalizer()
                           .normalize( value ) );

                   }
                   else
                   {
                       // We don't have a normalizer for this OID : just do
                       // nothing.
                       return;
                   }
               }
           }
           else
           {
               // The type is empty : this is not possible...
               log.error( "We should not have an empty DN" );
               throw new InvalidNameException( "Empty type not allowed in a DN" );
           }
       }
   }


   /**
    * Change the internal DN, using the first alias instead of oids or other
    * aliases. As we still have the UP name of each RDN, we will be able to
    * provide both representation of the DN. example : dn: 2.5.4.3=People,
    * dc=example, domainComponent=com will be transformed to : cn=People,
    * dc=example, dc=com because 2.5.4.3 is the OID for cn and dc is the first
    * alias of the couple of aliases (dc, domaincomponent). This is really
    * important do have such a representation, as 'cn' and 'commonname' share
    * the same OID.
    *
    * @param dn
    *            The DN to transform
    * @param oidsMap
    *            The mapping between names and oids.
    * @return A normalized form of the DN
    * @throws InvalidNameException
    *             If the DN is invalid
    */
   public static LdapDN normalize( LdapDN dn, Map oidsMap ) throws InvalidNameException, NamingException
   {
       if ( ( dn == null ) || ( dn.size() == 0 ) || ( oidsMap == null ) || ( oidsMap.size() == 0 ) )
       {
           return dn;
       }

       LdapDN newDn = ( LdapDN ) dn.clone();

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

       return newDn;
   }

   /**
    * Change the internal DN, using the first alias instead of oids or other
    * aliases. As we still have the UP name of each RDN, we will be able to
    * provide both representation of the DN. example : dn: 2.5.4.3=People,
    * dc=example, domainComponent=com will be transformed to : cn=People,
    * dc=example, dc=com because 2.5.4.3 is the OID for cn and dc is the first
    * alias of the couple of aliases (dc, domaincomponent). This is really
    * important do have such a representation, as 'cn' and 'commonname' share
    * the same OID.
    *
    * @param oidsMap
    *            The mapping between names and oids.
    * @throws InvalidNameException
    *             If the DN is invalid
    */
   public void normalize( Map oidsMap ) throws InvalidNameException, NamingException
   {
       if ( ( oidsMap == null ) || ( oidsMap.size() == 0 ) )
       {
           return;
       }

       if ( size() == 0 )
       {
           return;
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
   }
   
   /**
    * Check if a DistinguishedName is syntaxically valid
    *
    * @param dn The DN to validate
    * @return <code>true></code> if the DN is valid, <code>false</code>
    * otherwise
    */
   public static boolean isValid( String dn )
   {
       return LdapDnParser.validateInternal( dn );
   }
}
