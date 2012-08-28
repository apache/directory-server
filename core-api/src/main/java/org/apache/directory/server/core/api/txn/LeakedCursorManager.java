package org.apache.directory.server.core.api.txn;


import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;


public interface LeakedCursorManager
{
    Cursor<Entry> createLeakedCursor( EntryFilteringCursor cursor ) throws Exception;


    void trackCursor( EntryFilteringCursor cursor );
}
