/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.schema ;

import java.util.List ;
import java.util.Iterator ;
import java.util.ArrayList ;
import java.util.Hashtable ;
import java.util.Collections ;

import javax.naming.NamingException ;


/**
 * Default OID registry implementation used to resolve a schema object OID 
 * to a name and vice-versa.
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class DefaultOidRegistry implements OidRegistry
{ 
    /** Maps OID to a name or a list of names if more than one name exists */
    private Hashtable m_byOid = new Hashtable() ;
    /** Maps several names to an OID */
    private Hashtable m_byName = new Hashtable() ;
    /** Default OidRegistryMonitor */
    private OidRegistryMonitor m_monitor = null ;
    
    
    /**
     * @see org.apache.ldap.server.schema.OidRegistry#getOid(java.lang.String)
     */
    public String getOid( String a_name ) throws NamingException
    {
        /* If a_name is an OID than we return it back since inherently the 
         * OID is another name for the object referred to by OID and the
         * caller does not know that the argument is an OID String.
         */
        if ( Character.isDigit( a_name.charAt( 0 ) ) 
            && m_byOid.containsKey( a_name ) )
        {
            m_monitor.getOidWithOid( a_name ) ;
            return a_name ;
        }

        // If a_name is mapped to a OID already return OID
        if ( m_byName.containsKey( a_name ) )
        {
            String l_oid = ( String ) m_byName.get( a_name ) ; 
            m_monitor.oidResolved( a_name, l_oid ) ;
            return l_oid ;
        }
        
        /*
         * As a last resort we check if a_name is not normalized and if the 
         * normalized version used as a key returns an OID.  If the normalized
         * name works add the normalized name as a key with its OID to the 
         * byName lookup.  BTW these normalized versions of the key are not 
         * returned on a getNameSet.
         */
         String l_lowerCase = a_name.trim().toLowerCase() ;
         if ( ! a_name.equals( l_lowerCase ) 
            && m_byName.containsKey( l_lowerCase ) )
         {
             String l_oid = ( String ) m_byName.get( l_lowerCase ) ;
             m_monitor.oidResolved( a_name, l_lowerCase, l_oid ) ;
             
             // We expect to see this version of the key again so we add it 
             m_byName.put( a_name, l_oid ) ;
             return l_oid ;
         }
         
         NamingException l_fault = new NamingException ( "OID for name '" 
                 + a_name + "' was not " + "found within the OID registry" ) ; 
         m_monitor.oidResolutionFailed( a_name, l_fault ) ;
         throw l_fault ;
    }


    /**
     * @see org.apache.ldap.server.schema.OidRegistry#getPrimaryName(java.lang.String)
     */
    public String getPrimaryName( String a_oid ) throws NamingException
    {
        Object l_value = m_byOid.get( a_oid ) ;
        
        if ( null == l_value )
        {
            NamingException l_fault = new NamingException ( "OID '" + a_oid 
                    + "' was not found within the OID registry" ) ; 
            m_monitor.oidDoesNotExist( a_oid, l_fault ) ;
            throw l_fault ;
        }
        
        if ( l_value instanceof String )
        {
            m_monitor.nameResolved( a_oid, ( String ) l_value ) ;
            return ( String ) l_value ;
        }
        
        String l_name = ( String ) ( ( List ) l_value ).get( 0 ) ;
        m_monitor.nameResolved( a_oid, l_name ) ;
        return l_name ;
    }


    /**
     * @see org.apache.ldap.server.schema.OidRegistry#getNameSet(java.lang.String)
     */
    public List getNameSet( String a_oid ) throws NamingException
    {
        Object l_value = m_byOid.get( a_oid ) ;
        
        if ( null == l_value )
        {
            NamingException l_fault = new NamingException ( "OID '" + a_oid 
                    + "' was not found within the OID registry" ) ; 
            m_monitor.oidDoesNotExist( a_oid, l_fault ) ;
            throw l_fault ;
        }
        
        if ( l_value instanceof String )
        {
            List l_list = Collections.singletonList( l_value ) ;
            m_monitor.namesResolved( a_oid, l_list ) ;
            return l_list ;
        }
        
        m_monitor.namesResolved( a_oid, ( List ) l_value ) ;
        return ( List ) l_value ;
    }


    /**
     * @see org.apache.ldap.server.schema.OidRegistry#list()
     */
    public Iterator list()
    {
        return Collections.unmodifiableSet( m_byOid.keySet() ).iterator() ;
    }


    /**
     * @see org.apache.ldap.server.schema.OidRegistry#add(java.lang.String, 
     * java.lang.String)
     */
    public void register( String a_name, String a_oid )
    {
        /*
         * Add the entry for the given name as is and its lowercased version if
         * the lower cased name is different from the given name name.  
         */
        String l_lowerCase = a_name.toLowerCase() ;
        if ( ! l_lowerCase.equals( a_name ) )
        {
            m_byName.put( l_lowerCase, a_oid ) ;
        }
        
        // Put both the name and the oid as names
        m_byName.put( a_name, a_oid ) ;
        m_byName.put( a_oid, a_oid ) ;
        
        /*
         * Update OID Map
         * 
         * 1). Check if we already have a value[s] stored
         *      1a). Value is a single value and is a String
         *          Replace value with list containing old and new values
         *      1b). More than one value stored in a list
         *          Add new value to the list
         * 2). If we do not have a value then we just add it as a String
         */
        Object l_value = null ;
        if ( ! m_byOid.containsKey( a_oid ) )
        {
            l_value = a_name ;
        }
        else 
        {
            ArrayList l_list = null ;
            l_value = m_byOid.get( a_oid ) ;
            
            if ( l_value instanceof String )
            {
                String l_existingName = ( String ) l_value ;
                
                // if the existing name is already there we don't readd it
                if ( l_existingName.equalsIgnoreCase( a_name ) )
                {
                    return ;
                }
                
                l_list = new ArrayList() ;
                l_list.add( l_value ) ;
                l_value = l_list ;
            }
            else if ( l_value instanceof ArrayList )
            {
                l_list = ( ArrayList ) l_list ;
                
                for ( int ii = 0; ii < l_list.size(); ii++ )
                {
                    // One form or another of the name already exists in list
                    if ( ! a_name.equalsIgnoreCase( ( String ) 
                        l_list.get( ii ) ) )
                    {
                        return ;
                    }
                }
                
                l_list.add( a_name ) ;
            }
        }

        m_byOid.put( a_oid, l_value ) ;
        m_monitor.registered( a_name, a_oid ) ;
    }
    
    
    /**
     * Gets the monitor.
     * 
     * @return the monitor
     */
    OidRegistryMonitor getMonitor()
    {
        return m_monitor ;
    }

    
    /**
     * Sets the monitor.
     * 
     * @param a_monitor monitor to set.
     */
    void setMonitor( OidRegistryMonitor a_monitor )
    {
        m_monitor = a_monitor ;
    }
}

