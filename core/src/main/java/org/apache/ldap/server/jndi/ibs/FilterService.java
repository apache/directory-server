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


import org.apache.ldap.server.db.SearchResultFilter;
import org.apache.ldap.server.db.SearchResultFilter;


/**
 * A service which applies a linear combination of filters to attributes before
 * they are returned from calls to the following operations:
 * <ul>
 *   <li><code>{@link org.apache.eve.BackingStore#lookup(javax.naming.Name)}</code></li>
 *   <li><code>{@link org.apache.eve.BackingStore#lookup(javax.naming.Name,String[])}</code></li>
 *   <li><code>{@link org.apache.eve.BackingStore#search(javax.naming.Name,
 * java.util.Map, org.apache.ldap.common.filter.ExprNode,
 * javax.naming.directory.SearchControls)}</code></li>
 * </ul>
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface FilterService
{
    /**
     * Adds a new lookup filter to the end of the list of filters be applied to
     * attributes being returned to Eve JNDI Contexts from calls made to
     * {@link org.apache.eve.BackingStore#lookup(javax.naming.Name)} and
     * {@link org.apache.eve.BackingStore#lookup(javax.naming.Name,String[])}
     *
     * @param filter the filter to be added
     * @return the return value from {@link java.util.List#add(Object)}
     */
    boolean addLookupFilter( LookupFilter filter );


    /**
     * Adds a new database search result filter to the end of the list of
     * filters be applied to attributes being returned via NamingEnumerations
     * created via calls made to {@link org.apache.eve.BackingStore#search(
     * javax.naming.Name, java.util.Map, org.apache.ldap.common.filter.ExprNode,
     * javax.naming.directory.SearchControls)}
     *
     * @param filter the filter to be added
     * @return the return value from {@link java.util.List#add(Object)}
     */
    boolean addSearchResultFilter( SearchResultFilter filter );
}
