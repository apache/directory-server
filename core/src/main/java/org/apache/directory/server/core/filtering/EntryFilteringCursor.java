package org.apache.directory.server.core.filtering;


import java.util.List;

import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;


public interface EntryFilteringCursor extends Cursor<ClonedServerEntry>
{

    /**
     * Gets whether or not this BaseEntryFilteringCursor has been abandoned.
     *
     * @return true if abandoned, false if not
     */
    public abstract boolean isAbandoned();


    /**
     * Sets whether this BaseEntryFilteringCursor has been abandoned.
     *
     * @param abandoned true if abandoned, false if not
     */
    public abstract void setAbandoned( boolean abandoned );


    /**
     * Adds an entry filter to this BaseEntryFilteringCursor at the very end of 
     * the filter list.  EntryFilters are applied in the order of addition.
     * 
     * @param filter a filter to apply to the entries
     * @return the result of {@link List#add(Object)}
     */
    public abstract boolean addEntryFilter( EntryFilter filter );


    /**
     * Removes an entry filter to this BaseEntryFilteringCursor at the very end of 
     * the filter list.  
     * 
     * @param filter a filter to remove from the filter list
     * @return the result of {@link List#remove(Object)}
     */
    public abstract boolean removeEntryFilter( EntryFilter filter );


    /**
     * Gets an unmodifiable list of EntryFilters applied.
     *
     * @return an unmodifiable list of EntryFilters applied
     */
    public abstract List<EntryFilter> getEntryFilters();


    /**
     * @return the operationContext
     */
    public abstract SearchingOperationContext getOperationContext();
}