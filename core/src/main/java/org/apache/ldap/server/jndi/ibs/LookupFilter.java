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
package org.apache.ldap.server.jndi.ibs;


import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;


/**
 * A simple filter for modifying Attributes as they are returned to the caller
 * following a lookup operation.  For filtering search results see
 * {@link org.apache.ldap.server.db.SearchResultFilter}s.  These filters unlike ResultFilters
 * cannot stop an entry from being returned unless they throw exceptions like
 * {@link javax.naming.NameNotFoundException}.  That is why the method is named
 * <code>filter()</code> instead of <code>accept()</code>.  This is also why
 * <code>filter()</code> returns void instead of a boolean like <code>accept()
 * </code>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface LookupFilter
{
    /**
     * Filters attributes to be returned from
     * {@link org.apache.ldap.server.BackingStore#lookup(Name)} operations.
     *
     * @param dn the distinguished name of the entry looked up
     * @param entry the attributes of the entry that were looked up
     * @throws NamingException if there are any errors while trying to apply
     * the filter
     */
    void filter( LdapContext ctx, Name dn, Attributes entry )
            throws NamingException;

    /**
     * Filters attributes to be returned from
     * {@link org.apache.ldap.server.BackingStore#lookup(Name,String[])} operations.
     *
     * @param dn the distinguished name of the entry looked up
     * @param entry the attributes of the entry that were looked up
     * @param ids
     * @throws NamingException if there are any errors while trying to apply
     * the filter
     */
    void filter( LdapContext ctx, Name dn, Attributes entry, String[] ids )
            throws NamingException;
}
