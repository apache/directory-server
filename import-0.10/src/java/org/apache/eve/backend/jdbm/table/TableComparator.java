/*
 * $Id: TableComparator.java,v 1.2 2003/03/13 18:27:34 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


/**
 * Doc me!
 * @todo Doc me!
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public abstract class TableComparator
    extends jdbm.helper.Comparator
    implements java.util.Comparator
{
    public abstract int compare( Object an_obj1, Object an_obj2 ) ;
}
