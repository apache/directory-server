/*
 * $Id: ProtocolAdapter.java,v 1.2 2003/03/13 18:27:28 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


public class ProtocolAdapter
	implements ProtocolListener
{
    public void after(UnbindEvent a_event) { }

    public void after(SearchEvent a_event) { }

    public void after(ModifyDnEvent a_event) { }

    public void after(ModifyEvent a_event) { }

    public void after(ExtendedEvent a_event) { }

    public void after(DelEvent a_event) { }

    public void after(CompareEvent a_event) { }

    public void after(BindEvent a_event) { }

    public void after(AddEvent a_event) { }

    public void after(AbandonEvent a_event) { }

	public void before(UnbindEvent a_event) { }

	public void before(SearchEvent a_event) { }

	public void before(ModifyDnEvent a_event) { }

	public void before(ModifyEvent a_event) { }

	public void before(ExtendedEvent a_event) { }

	public void before(DelEvent a_event) { }

	public void before(CompareEvent a_event) { }

	public void before(BindEvent a_event) { }

	public void before(AddEvent a_event) { }

	public void before(AbandonEvent a_event) { }
}
