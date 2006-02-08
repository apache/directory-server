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
 * $Id: LdifEntry.java,v 1.3 2003/07/31 21:44:49 akarasulu Exp $
 *
 * -- (c) LDAPd Group
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.directory.shared.ldap.ldif;


import java.util.LinkedList;

import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;


/**
 * A entry to be populated by an ldif parser.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author <a href="mailto:jmachols@comcast.net">Jeff Machols</a>
 * @author $author$
 * @version $Revision$
 */
public class LdifEntry
{
    /** the version of the ldif */
    private int m_version;

    /** the modification type */
    private String m_modType;

    /** the modification item list */
    private LinkedList m_itemList;

    /** the dn of the ldif entry */
    private String m_dn;

    /** attributes of the entry */
    private BasicAttributes m_attributeList;


    /**
     * Creates a new LdifEntry object.
     */
    public LdifEntry()
    {
        m_modType = "add"; // Default LDIF content
        m_itemList = new LinkedList();
        m_dn = null;
        m_attributeList = new BasicAttributes( true );
        m_version = 1; // default version in ldif
    }


    /**
     * Sets the version of this ldif
     * 
     * @param a_ver
     *            sets the version of this ldif
     */
    public void setVersion( int a_ver )
    {
        m_version = a_ver;
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @param a_dn
     *            TODO DOCUMENT ME!
     */
    public void setDn( String a_dn )
    {
        m_dn = a_dn;
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @param a_modType
     *            TODO DOCUMENT ME!
     */
    public void setModType( String a_modType )
    {
        m_modType = a_modType;
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @param a_modOp
     *            TODO DOCUMENT ME!
     * @param a_attr
     *            TODO DOCUMENT ME!
     */
    public void addModificationItem( int a_modOp, Attribute a_attr )
    {
        ModificationItem l_item = new ModificationItem( a_modOp, a_attr );
        m_itemList.add( l_item );
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @param a_modOp
     *            TODO DOCUMENT ME!
     * @param a_id
     *            TODO DOCUMENT ME!
     * @param a_value
     *            TODO DOCUMENT ME!
     */
    public void addModificationItem( int a_modOp, String a_id, Object a_value )
    {
        BasicAttribute l_attr = new BasicAttribute( a_id, a_value );
        ModificationItem l_item = new ModificationItem( a_modOp, l_attr );
        m_itemList.add( l_item );
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @param a_attr
     *            TODO DOCUMENT ME!
     */
    public void addAttribute( Attribute a_attr )
    {
        m_attributeList.put( a_attr );
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @param a_id
     *            TODO DOCUMENT ME!
     * @param a_value
     *            TODO DOCUMENT ME!
     */
    public void addAttribute( String a_id, Object a_value )
    {
        m_attributeList.put( a_id, a_value );
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @return TODO DOCUMENT ME!
     */
    public String getModType()
    {
        return m_modType;
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @return TODO DOCUMENT ME!
     */
    public LinkedList getModificationItems()
    {
        return m_itemList;
    }


    /**
     * TODO DOCUMENT ME!
     * 
     * @return TODO DOCUMENT ME!
     */
    public String getDn()
    {
        return m_dn;
    }


    public int getVersion()
    {
        return this.m_version;
    }
}
