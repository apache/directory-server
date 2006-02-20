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
 * $Id$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.directory.shared.ldap.name;


import javax.naming.NamingException;


/**
 * Normalizers of ldap name component attributes and their values.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public interface NameComponentNormalizer
{
    /**
     * Checks to see if an attribute name/oid is defined.
     * 
     * @param id
     *            the name/oid of the attribute to see if it is defined
     * @return true if it is, false otherwise
     */
    boolean isDefined( String id );


    /**
     * Normalizes an attribute's value given the name of the attribute - short
     * names like 'cn' as well as 'commonName' should work here.
     * 
     * @param attributeName
     *            the name of the attribute
     * @param value
     *            the value of the attribute to normalize
     * @return the normalized value
     * @throws NamingException
     *             if there is a recognition problem or a syntax issue
     */
    String normalizeByName( String attributeName, String value ) throws NamingException;


    /**
     * Normalizes an attribute's value given the name of the attribute - short
     * names like 'cn' as well as 'commonName' should work here.
     * 
     * @param attributeName
     *            the name of the attribute
     * @param value
     *            the value of the attribute to normalize
     * @return the normalized value
     * @throws NamingException
     *             if there is a recognition problem or a syntax issue
     */
    String normalizeByName( String attributeName, byte[] value ) throws NamingException;


    /**
     * Normalizes an attribute's value given the OID of the attribute.
     * 
     * @param attributeOid
     *            the OID of the attribute
     * @param value
     *            the value of the attribute to normalize
     * @return the normalized value
     * @throws NamingException
     *             if there is a recognition problem or a syntax issue
     */
    String normalizeByOid( String attributeOid, String value ) throws NamingException;


    /**
     * Normalizes an attribute's value given the OID of the attribute.
     * 
     * @param attributeOid
     *            the OID of the attribute
     * @param value
     *            the value of the attribute to normalize
     * @return the normalized value
     * @throws NamingException
     *             if there is a recognition problem or a syntax issue
     */
    String normalizeByOid( String attributeOid, byte[] value ) throws NamingException;
}
