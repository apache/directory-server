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
package org.apache.ldap.server.enumeration;


import org.apache.ldap.server.invocation.Invocation;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


/**
 * A filter is used to modify search results while they are being returned from
 * naming enumerations containing DbSearchResults.  These filters are used in
 * conjunction with a {@link SearchResultFilteringEnumeration}.
 * Multiple filters can be applied one after the other and hence they are stackable.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SearchResultFilter
{
    /**
     * Filters the contents of search results on the way out the door to client
     * callers.  These filters can and do produce side-effects on the results if
     * if need be the attributes or names within the result should be cloned.
     *
     * @param result the database search result to return
     * @param controls search controls associated with the invocation
     * @return true if the result is to be returned, false if it is to be
     * discarded from the result set
     */
    boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
        throws NamingException;
}
