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

/*
 * $Id: LdapName.java,v 1.11 2003/09/23 07:13:26 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.ldap.common.name ;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.ldap.common.util.NamespaceTools;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;


/**
 * A distinguished name implementation for LDAPv3.
 *
 * @todo Get the RFC for DN syntax into this javadoc
 *
 * @todo Think about adding a printSuffix(int) method to avoid Name creation
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class LdapName implements Name
{
    static final long serialVersionUID = -2504400451346647793L;
    /** A name parser use by LdapName to parse strings into valid names */
    private static DnParser s_parser = null ;
    /** List of name components composing this Name. */
    private List m_list ;
    /** The cached string representation of this Name. */
    private String m_name ;
    /** Dirty bit used to determine whether the cached m_name is up to date */
    private boolean m_isClean = false ;
    
    public static LdapName EMPTY_LDAP_NAME = null;
    
    static
    {
    	try
    	{
    		EMPTY_LDAP_NAME = new LdapName( "" );
    	}
    	catch ( NamingException ne )
    	{
    		// Nothing to do...
    	}
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates the default empty name. ToString returns the empty string ""
     */
    public LdapName()
    {
        m_list = new ArrayList() ;
    }


    /**
     * Creates the name by parsing a distinguished name String.
     * 
     * @param dn the distinguished name as a String.
     * @throws NamingException if a parser cannot be initialized or the name is
     *    invalid
     */
    public LdapName( String dn ) throws NamingException
    {
        // Protect against the use of the empty string as a special case.
        if ( null == dn || dn.trim().equals( "" ) )
        {
            m_list = new ArrayList() ;
            return ;
        }
        
        // Past this point we are dealing with a non-empty DN and must 
        getNameParser().parse( dn, this ) ;
    }


    /**
     * Creates an ldap name using a list of name components.
     *
     * @param a_list of String name components.
     */
    LdapName( List a_list )
    {
        m_list = a_list ;
    }


    /**
     * Creates an ldap name using a list of name components.
     *
     * @param a_list of String name components.
     */
    LdapName( Iterator a_list )
    {
        m_list = new ArrayList() ;
        while ( a_list.hasNext() ) 
        {
            m_list.add( a_list.next() ) ;
        }
    }


    /**
     * Sets the internal list used to store name components in this LdapName.  
     * This method is called by the DnParser when it's parse method is invoked
     * to populate an existing LdapName instance.
     *
     * @param a_list a list of components
     */
    void setList( ArrayList a_list )
    {
        m_list = a_list ;
    }
    

    /**
     * Generates a new copy of this name.
     * Subsequent changes to the components of this name will not
     * affect the new copy, and vice versa.
     *
     * @return  a copy of this name
     *
     * @see Object#clone()
     */
    public Object clone()
    {
        return new LdapName( m_list.iterator() ) ;
    }


    /**
     * Compares this name with another name for order.  Returns a negative 
     * integer, zero, or a positive integer as this name is less than, equal to,
     * or greater than the given name.
     * 
     * <p> LDAP names contain the most significant component on the right and 
     * the least significant component on the left.  This fact must be 
     * considered while comparing name components lexographically to determine 
     * the correct ordering of names. </p>  
     * 
     * <p> Case is NOT ignored while matching names.  Matching rules in an LDAP 
     * schema are needed to correctly determine whether case matters or not.  
     * Every attribute in the name is presumed to take on the IA5String syntax
     * which does not ignore case while matching.  Note that rather than ignore
     * case here we let normalization occur before hand which will make case
     * irrelavent to begin with before this comarison is made.  So matching for 
     * case is a good idea here.
     *
     * @param an_obj the non-null object to compare against.
     * @return a negative integer, zero, or a positive integer as this name
     *      is less than, equal to, or greater than the given name
     * @throws ClassCastException if obj is not a <tt>Name</tt> of a
     *      type that may be compared with this name
     *
     * @see Comparable#compareTo(Object)
     */
    public int compareTo( Object an_obj )
    {
        /*
         * We only compare names that are of the same class.
         */
        if ( an_obj instanceof LdapName )
        {
            LdapName l_dn = ( LdapName ) an_obj ;
            int l_min = Math.min( size(), l_dn.size() ) ;

            /*
             * Compare the minimum number of name components that are 
             * potentially in common between the two names.  Read components
             * using the get() method on both names to make sure the correct
             * index translation occurs to take name component significance 
             * into acount.
             */
            for ( int ii = 0; ii < l_min; ii++ ) 
            {
                int compareTo = componentsCompareTo( get( ii ), l_dn.get( ii ) );
                if ( compareTo != 0 )
                {
                    return compareTo;
                }
            }
            
            /*
             * Up until this point, all the most significant name components in 
             * common are equal.  So if both names are equal in size we return 
             * 0, otherwise we return 1 or -1 depending on which name is longer.
             */
            if ( m_list.size() == l_dn.size() ) 
            {
                return 0 ;
            } 
            else if ( m_list.size() > l_dn.size() ) 
            {
                return 1 ;
            } 
            else 
            {
                return -1 ;
            }
        } 
        else 
        {
            throw new ClassCastException( "The object to compare this LdapName "
                + "to is not a valid LDAP distinguished name." ) ;
        }
    }

    /**
     * Returns the number of components in this name.
     *
     * @return the number of components in this name
     */
    public int size()
    {
        return m_list.size() ;
    }


    /**
     * Determines whether this name is empty.  An empty name is one with zero 
     * components.
     *
     * @return true if this name is empty, false otherwise
     */
    public boolean isEmpty()
    {
        return m_list.isEmpty() ;
    }


    /**
     * Retrieves the components of this name as an enumeration of Strings 
     * starting in order from the most significant name component on the right
     * to the least significant name component on the left.  The effect on the 
     * enumeration of updates to this name is undefined.  If the name has zero 
     * components, an empty (non-null) enumeration is returned.
     * 
     * @return  an enumeration of the components of this name, each a string
     */
    public Enumeration getAll()
    {
        /*
         * Note that by accessing the name component using the get() method on
         * the name rather than get() on the list we are reading components from
         * right to left with increasing index values.  LdapName.get() does the
         * index translation on m_list for us. 
         */
        return new Enumeration() 
        {
            private int l_pos ;

            public boolean hasMoreElements()
            {
                return l_pos < size() ;
            }

            public Object nextElement()
            {
                if ( l_pos >= size() ) 
                {
                    throw new NoSuchElementException() ;
                }

                Object l_obj = get( l_pos ) ;
                l_pos++ ;
                return l_obj ;
            }
        } ;
    }


    /**
     * Retrieves a component of this name using 0 based indexing from the right
     * most significant name component to the left least significant name 
     * component.  So the right most name component would be at index 0, and the
     * left least significant name component would be at index size() - 1. 
     *
     * @param a_posn
     *      the 0-based index of the component to retrieve.
     *      Must be in the range [0,size()).
     * @return the component at index posn
     * @throws ArrayIndexOutOfBoundsException
     *      if posn is outside the specified range
     */
    public String get( int a_posn )
    {
        return ( String ) m_list.get( size() - a_posn - 1 ) ;
    }


    /**
     * Creates a name whose components consist of a suffix of the components of
     * this name.  Subsequent changes to this name will not affect the name that
     * is returned and vice versa.
     *
     * @param a_posn
     *      the 0-based index of the component at which to stop.
     *      Must be in the range [0,size()].
     * @return  a name consisting of the components at indexes in
     *      the range [0,posn).
     * @throws  ArrayIndexOutOfBoundsException
     *      if posn is outside the specified range
     */
    public Name getSuffix( int a_posn )
    {
        ArrayList list = new ArrayList();
        list.addAll( m_list.subList( size() - a_posn, size() ) );
        return new LdapName( list ) ;
    }


    /**
     * Creates a name whose components consist of a prefix of the
     * components in this name.  Subsequent changes to
     * this name do not affect the name that is returned and vice versa.
     *
     * @param a_posn
     *      the 0-based index of the component at which to start.
     *      Must be in the range [0,size()].
     * @return  a name consisting of the components at indexes in
     *      the range [posn,size()).  If posn is equal to 
     *      size(), an empty name is returned.
     * @throws  IndexOutOfBoundsException
     *      if posn is outside the specified range
     */
    public Name getPrefix( int a_posn )
    {
        ArrayList list = new ArrayList();
        list.addAll( m_list.subList( a_posn, m_list.size() ) );
        return new LdapName( list ) ;
    }


    /**
     * Determines whether this name starts with a specified prefix.
     * A name <tt>n</tt> is a prefix if it is equal to
     * <tt>getPrefix(n.size())</tt>.
     *
     * @param a_name
     *      the name to check
     * @return  true if <tt>n</tt> is a prefix of this name, false otherwise
     */
    public boolean startsWith( Name a_name )
    {
        if ( a_name instanceof LdapName ) 
        {
            LdapName l_dn = ( LdapName ) a_name ;

            // Can't be if a_name is larger than this name
            if ( l_dn.size() > size() ) 
            {
                return false ;
            }

            
            for ( int ii = 0; ii < l_dn.size(); ii++ ) 
            {
                if ( ! l_dn.get( ii ).equals( get( ii ) ) ) 
                {
                    return false ;
                }
            }

            return true ;
        } 
        else 
        {
            return false ;
        }
    }


    /**
     * Determines whether this name ends with a specified suffix.
     * A name <tt>n</tt> is a suffix if it is equal to
     * <tt>getSuffix(size()-n.size())</tt>.
     *
     * @param a_name
     *      the name to check
     * @return  true if <tt>n</tt> is a suffix of this name, false otherwise
     */
    public boolean endsWith( Name a_name )
    {
        if ( a_name instanceof LdapName ) 
        {
            LdapName l_dn = ( LdapName ) a_name ;

            // Can't be if a_name is larger than this name
            if ( l_dn.size() > size() )
            {
                return false ;
            }

            final int l_difference = size() - l_dn.size() ;
            for ( int ii = 0; ii < l_dn.size(); ii++ ) 
            {
                if ( ! l_dn.get( ii ).equals( get( l_difference + ii ) ) ) 
                {
                    return false ;
                }
            }

            return true ;
        }
        else 
        {
            return false ;
        }
    }


    /**
     * Used by methods to test if a name component is a syntactically valid name
     * within the namespace.
     *
     * @param a_component the name component to test
     * @throws InvalidNameException if the name component is not valid according
     * to the namespace syntax
     * TODO must write a smaller name component parser to use here!
     */
    private void syntaxCheck( String a_component )
        throws InvalidNameException
    {
        if ( a_component.indexOf( '=' ) == -1 ) 
        {
            throw new InvalidNameException( "Name component " + a_component 
                + " is not a valid distinguished name component." ) ;
        }
    }


    /**
     * Used by methods to test if a name component is a syntactically valid name
     * within the namespace.
     *
     * @param a_component the name component to test
     * @param a_index the index in the name the component is located in
     * @throws InvalidNameException if the name component is not valid according
     * to the namespace syntax
     * TODO must write a smaller name component parser to use here!
     */
    private void syntaxCheck( String a_component, int a_index )
        throws InvalidNameException
    {
        if ( a_component.indexOf( '=' ) == -1 ) 
        {
            throw new InvalidNameException( "Name component " 
                + a_component + " at index " + a_index 
                + " is not a valid distinguished name component." ) ;
        }
    }


    /**
     * Adds the components of a name -- in order -- to the end of this name.
     *
     * @param a_suffix the components to add
     * @return the updated name (not a new one)
     * @throws  InvalidNameException if <tt>suffix</tt> is not a valid name,
     *      or if the addition of the components would violate the syntax
     *      rules of this name
     */
    public Name addAll( Name a_suffix ) throws InvalidNameException
    {
        /*
         * Scan for syntax violations first before commiting to the operation
         * otherwise we may partially alter this name if an exception results
         */ 
        for ( int ii = 0; ii < a_suffix.size(); ii++ ) 
        {
            syntaxCheck( a_suffix.get( ii ), ii ) ;
        }

        // Add all the valid suffix name components now.
        for ( int ii = 0; ii < a_suffix.size(); ii++ ) 
        {
            m_list.add( 0, a_suffix.get( ii ) ) ;
        }

        m_isClean = false ;
        return this ;
    }


    /**
     * Adds the components of a name -- in order -- at a specified position
     * within this name.  Components of this name at or after the index of the 
     * first new component are shifted up (away from 0) to accommodate the new
     * components.
     *
     * @param a_name the components to add
     * @param a_posn the index in this name at which to add the new components.
     *      Must be in the range [0,size()].
     * @return the updated name (not a new one)
     * @throws ArrayIndexOutOfBoundsException if posn is outside the specified 
     *      range
     * @throws  InvalidNameException if <tt>n</tt> is not a valid name,
     *      or if the addition of the components would violate the syntax
     *      rules of this name
     */
    public Name addAll( int a_posn, Name a_name ) throws InvalidNameException
    {
        /*
         * Scan for syntax violations first before commiting to the operation
         * otherwise we may partially alter this name if an exception results
         */ 
        for ( int ii = 0; ii < a_name.size(); ii++ ) 
        {
            syntaxCheck( a_name.get( ii ), ii ) ;
        }

        // Add all the valid components now.
        for ( int ii = 0; ii < a_name.size(); ii++ ) 
        {
            m_list.add( size() - a_posn - ii, a_name.get( ii ) ) ;
        }

        m_isClean = false ;
        return this ;
    }


    /**
     * Adds a single component to the end of this name.
     *
     * @param a_comp
     *      the component to add
     * @return  the updated name (not a new one)
     *
     * @throws  InvalidNameException if adding <tt>comp</tt> would violate
     *      the syntax rules of this name
     */
    public Name add( String a_comp ) throws InvalidNameException
    {
        syntaxCheck( a_comp ) ;
        m_list.add( 0, a_comp ) ;
        m_isClean = false ;
        return this ;
    }


    /**
     * Adds a single component at a specified position within this name.
     * Components of this name at or after the index of the new component
     * are shifted up by one (away from index 0) to accommodate the new
     * component.
     *
     * @param a_comp the component to add
     * @param a_posn the index at which to add the new component.  Must be in 
     *      the range [0,size()].
     * @return  the updated name (not a new one)
     * @throws  ArrayIndexOutOfBoundsException if posn is outside the specified 
     *      range
     * @throws  InvalidNameException if adding <tt>comp</tt> would violate the 
     *      syntax rules of this name
     */
    public Name add( int a_posn, String a_comp ) throws InvalidNameException
    {
        syntaxCheck( a_comp ) ;
        m_list.add( size() - a_posn, a_comp ) ;
        m_isClean = false ;
        return this ;
    }


    /**
     * Removes a component from this name.
     * The component of this name at the specified position is removed.
     * Components with indexes greater than this position
     * are shifted down (toward index 0) by one.
     *
     * @param a_posn
     *      the index of the component to remove.
     *      Must be in the range [0,size()).
     * @return  the component removed (a String)
     *
     * @throws  IndexOutOfBoundsException
     *      if posn is outside the specified range
     * @throws  InvalidNameException if deleting the component
     *      would violate the syntax rules of the name
     */
    public Object remove( int a_posn ) throws InvalidNameException
    {
        m_isClean = false ;
        return m_list.remove( size() - a_posn - 1 ) ;
    }


    ////////////////////////////
    // Object Class Overrides //
    ////////////////////////////


    /**
     * Overriden to printout the entire LdapName with all name components in
     * comma separated format.
     *
     * @return the string representation of this LdapName.
     */
    public String toString()
    {
        if ( m_name != null && m_isClean ) 
        {
            return m_name ;
        }

        if ( m_list.size() == 0 ) 
        {
            m_name = "" ;
            return m_name ;
        }

        StringBuffer l_dnStr = new StringBuffer( ( String ) m_list.get( 0 ) ) ;
        for ( int ii = 1; ii < this.m_list.size(); ii++ ) 
        {
            l_dnStr.append( ',' ).append( m_list.get( ii ) ) ;
        }

        m_name = l_dnStr.toString() ;
        m_isClean = true ;
        return m_name ;
    }


    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( obj instanceof String )
        {
            return toString().equals( obj ) ;
        } 
        else if ( obj instanceof Name )
        {
            Name name = ( Name ) obj ;

            if ( name.size() != this.size() )
            {
                return false ;
            }

            for ( int ii = 0; ii < size(); ii++ ) 
            {
                if ( ! areComponentsEqual( name.get( ii ), get( ii ) ) )
                {
                    return false;
                }
            }

            // All components matched so we return true
            return true ;
        } 
        else 
        {
            return false ;
        }
    }


    private boolean areComponentsEqual( String comp0, String comp1 )
    {
        // Upon finding first non matching component we return false
        if ( ! comp0.equals( comp1 ) )
        {
            // before returning false check if we have composite components with
            // different order of attribute value pairs

            boolean isComposite = false;

            try
            {
                isComposite = NamespaceTools.hasCompositeComponents( comp0 );

                if ( isComposite && !areCompositeComponentsEqual( comp0, comp0 ) )
                {
                    return false;
                }
            }
            catch( NamingException e )
            {
                // @todo log somethihng here
                return false;
            }

            // before returning false check if the attributeType has a different case
            if ( ! isComposite && ! NamespaceTools.getRdnValue( comp0 ).equals(
                                    NamespaceTools.getRdnValue( comp1 ) ) )
            {
                return false;
            }
        }

        return true;
    }


    private int componentsCompareTo( String comp0, String comp1 )
    {
        // Upon finding first non matching component we return false
        if ( ! comp0.equals( comp1 ) )
        {
            // before returning false check if we have composite components with
            // different order of attribute value pairs

            boolean isComposite = false;

            try
            {
                isComposite = NamespaceTools.hasCompositeComponents( comp0 );

                if ( isComposite && !areCompositeComponentsEqual( comp0, comp1 ) )
                {
                    return comp0.compareTo( comp1 );
                }
            }
            catch( NamingException e )
            {
                // @todo log somethihng here
                return comp0.compareTo( comp1 );
            }

            // before returning false check if the attributeType has a different case
            if ( ! isComposite && ! NamespaceTools.getRdnValue( comp0 ).equals(
                                    NamespaceTools.getRdnValue( comp1 ) ) )
            {
                return comp0.compareTo( comp1 );
            }
        }

        return 0;
    }


    private boolean areCompositeComponentsEqual( String comp0, String comp1 ) throws NamingException
    {
        String[] comps0 = NamespaceTools.getCompositeComponents( comp0 );
        HashSet set0 = new HashSet(comps0.length);
        String[] comps1 = NamespaceTools.getCompositeComponents( comp1 );
        HashSet set1 = new HashSet(comps1.length);

        if ( comps0.length != comps1.length )
        {
            return false;
        }

        // normalize the name component
        for ( int ii = 0; ii < comps0.length; ii++ )
        {
            StringBuffer buf = new StringBuffer();
            String attr = NamespaceTools.getRdnAttribute( comps0[ii] );
            String value = NamespaceTools.getRdnValue( comps0[ii] );
            buf.append( attr.toLowerCase() );
            buf.append( "=" );
            buf.append( value );
            set0.add( buf.toString() );

            buf.setLength( 0 );
            attr = NamespaceTools.getRdnAttribute( comps1[ii] );
            value = NamespaceTools.getRdnValue( comps1[ii] );
            buf.append( attr.toLowerCase() );
            buf.append( "=" );
            buf.append( value );
            set1.add( buf.toString() );
        }

        Iterator list = set0.iterator();
        while ( list.hasNext() )
        {
            if ( ! set1.contains( list.next() ) )
            {
                return false;
            }
        }

        return true;
    }


//    private int compositeComponentsCompareTo( String comp0, String comp1 ) throws NamingException
//    {
//        String[] comps0 = NamespaceTools.getCompositeComponents( comp0 );
//        HashSet set0 = new HashSet(comps0.length);
//        String[] comps1 = NamespaceTools.getCompositeComponents( comp1 );
//        HashSet set1 = new HashSet(comps1.length);
//
//        if ( comps0.length != comps1.length )
//        {
//            return comp0.compareTo( comp1 );
//        }
//
//        // normalize the name component
//        for ( int ii = 0; ii < comps0.length; ii++ )
//        {
//            StringBuffer buf = new StringBuffer();
//            String attr = NamespaceTools.getRdnAttribute( comps0[ii] );
//            String value = NamespaceTools.getRdnValue( comps0[ii] );
//            buf.append( attr.toLowerCase() );
//            buf.append( "=" );
//            buf.append( value );
//            set0.add( buf.toString() );
//
//            buf.setLength( 0 );
//            attr = NamespaceTools.getRdnAttribute( comps1[ii] );
//            value = NamespaceTools.getRdnValue( comps1[ii] );
//            buf.append( attr.toLowerCase() );
//            buf.append( "=" );
//            buf.append( value );
//            set1.add( buf.toString() );
//        }
//
//        Iterator list = set0.iterator();
//        while ( list.hasNext() )
//        {
//            if ( ! set1.contains( list.next() ) )
//            {
//                return comp0.compareTo( comp1 );
//            }
//        }
//
//        return 0;
//    }


    /**
     * Gets the hashcode of the string representation of this name.
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return toString().hashCode() ;
    }


    // ------------------------------------------------------------------------
    // Static Methods
    // ------------------------------------------------------------------------


    /**
     * Easy conventient method to get ahold of the NameParser singleton for all 
     * distinguished names within the LDAP namespace.
     * 
     * @return the DN name parser for the LDAP namespace
     * @throws NamingException if the parser could not be initialized.
     */
    public static DnParser getNameParser() throws NamingException
    {
        if ( s_parser == null )
        {
            s_parser = new DnParser() ;
        }

        return s_parser ;
    }


    /**
     * Gets the Rdn of this LdapName.
     *
     * @return the rdn
     */
    public String getRdn()
    {
        if ( 0 == size() )
        {
            return "" ;
        }
        
        return get( size() - 1 ) ;
    }
    
    
    /**
     * Gets the Rdn of a distinguished name.
     *
     * @param a_dn the LDAP based Name to get the Rdn of 
     * @return the rdn
     */
    public static String getRdn( Name a_dn )
    {
        if ( 0 == a_dn.size() )
        {
            return "" ;
        }
        
        return a_dn.get( a_dn.size() - 1 ) ;
    }
    

    /*
    public static LdapName toOidName( LdapName dn, Map oids ) throws InvalidNameException
    {
    	if ( ( dn == null ) || ( dn.size() == 0 ) )
    	{
    		return dn;
    	}
    	
    	LdapName newDn = new LdapName();
    	
    	Enumeration rdns = dn.getAll();
    	
    	while ( rdns.hasMoreElements() )
    	{
    		 String rdn = (String)rdns.nextElement();
    		 
    		 if ( rdn.indexOf( '+' ) != -1 )
    		 {
    			 
    		 }
    		 else
    		 {
    			 int posEqual = rdn.indexOf( '=' );
    			 
    			 if ( posEqual > 0 )
    			 {
    				 String name = StringTools.trim( rdn.substring( 0, posEqual ) );
    				 
    				 if ( StringTools.isEmpty( StringTools.lowerCase( name ) ) == false )
    				 {
    					 String oid = (String)oids.get( name );
    					 
    					 if ( oid != null )
    					 {
        					 String newRdn = oid + rdn.substring( posEqual );
        					 newDn.add( newRdn );
    					 }
    					 else
    					 {
    						 return null;
    					 }
    				 }
    				 else
    				 {
    					 return null;
    				 }
    			 }
    			 else
    			 {
    				 return  null;
    			 }
				 
			 }
    	}
    	
    	return newDn;
    }
    */
}

