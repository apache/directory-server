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

package org.apache.directory.shared.ldap.message;


/**
 * Super interface used as a marker for all protocol response type messages.
 * Note that only 4 response interfaces directly extend this interfaces. They
 * are listed below:
 * <ul>
 * <li> UnbindResponse </li>
 * <li> AbandonResponse </li>
 * <li> SearchResponseEntry </li>
 * <li> SearchResponseReference </li>
 * </ul>
 * <br>
 * All other responses derive from the ResultResponse interface. These responses
 * unlike the three above have an LdapResult component. The ResultResponse
 * interface takes this into account providing a Response with an LdapResult
 * property.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public interface Response extends Message
{
}
