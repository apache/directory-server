/**
 * JDBM LICENSE v1.00
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "JDBM" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Cees de Groot.  For written permission,
 *    please contact cg@cdegroot.com.
 *
 * 4. Products derived from this Software may not be called "JDBM"
 *    nor may "JDBM" appear in their names without prior written
 *    permission of Cees de Groot. 
 *
 * 5. Due credit should be given to the JDBM Project
 *    (http://jdbm.sourceforge.net/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE JDBM PROJECT AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * CEES DE GROOT OR ANY CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2000 (C) Cees de Groot. All Rights Reserved.
 * Contributions are Copyright (C) 2000 by their associated contributors.
 *
 * $Id: FreeLogicalRowIdPageManager.java,v 1.1 2000/05/06 00:00:31 boisvert Exp $
 */

package jdbm.recman;


import java.io.IOException;


/**
 *  This class manages free Logical rowid pages and provides methods
 *  to free and allocate Logical rowids on a high level.
 */
final class FreeLogicalRowIdPageManager
{
    /** our record recordFile */
    private RecordFile recordFile;

    /** our page manager */
    private PageManager pageManager;


    /**
     *  Creates a new instance using the indicated record file and
     *  page manager.
     */
    FreeLogicalRowIdPageManager( PageManager pageManager ) throws IOException
    {
        this.pageManager = pageManager;
        this.recordFile = pageManager.getRecordFile();
    }


    /**
     *  Returns a free Logical rowid, or null if nothing was found.
     */
    Location get() throws IOException
    {
        // Loop through the free Logical rowid list until we find
        // the first rowid.
        // Create a cursor to browse the pages
        PageCursor cursor = new PageCursor( pageManager, Magic.FREELOGIDS_PAGE );

        // Loop on the pages now
        while ( cursor.next() != 0 )
        {
            // Get the blockIo associated with the blockId
            BlockIo blockIo = recordFile.get( cursor.getBlockId() );
            FreeLogicalRowIdPage fp = FreeLogicalRowIdPage.getFreeLogicalRowIdPageView( blockIo );

            // Get the first allocated FreeLogicalRowId
            int slot = fp.getFirstAllocated();

            if ( slot != -1 )
            {
                // got one!
                Location location = new Location( fp.get( slot ) );

                // Remove the block from the page
                fp.free( slot );

                boolean hasMore = fp.getCount() != 0;

                // Upate the recordFile
                recordFile.release( cursor.getBlockId(), hasMore );

                if ( !hasMore )
                {
                    // page became empty - free it
                    pageManager.free( Magic.FREELOGIDS_PAGE, cursor.getBlockId() );
                }

                return location;
            }
            else
            {
                // no luck, go to next page
                recordFile.release( cursor.getBlockId(), false );
            }
        }

        return null;
    }


    /**
     *  Puts the indicated rowid on the free list
     *  
     *  @param rowId The Location where we will store the rowId
     */
    void put( Location rowId ) throws IOException
    {

        PhysicalRowId free = null;

        // Create a cursor on the FREELOGIDs list
        PageCursor curs = new PageCursor( pageManager, Magic.FREELOGIDS_PAGE );
        long freePage = 0;

        // Loop on all the list
        while ( curs.next() != 0 )
        {
            freePage = curs.getBlockId();
            BlockIo curBlockIo = recordFile.get( freePage );
            FreeLogicalRowIdPage fp = FreeLogicalRowIdPage.getFreeLogicalRowIdPageView( curBlockIo );
            int slot = fp.getFirstFree();

            if ( slot != -1 )
            {
                free = fp.alloc( slot );
                break;
            }

            recordFile.release( curBlockIo );
        }

        if ( free == null )
        {
            // No more space on the free list, add a page.
            freePage = pageManager.allocate( Magic.FREELOGIDS_PAGE );
            BlockIo curBlockIo = recordFile.get( freePage );
            FreeLogicalRowIdPage fp = FreeLogicalRowIdPage.getFreeLogicalRowIdPageView( curBlockIo );
            free = fp.alloc( 0 );
        }

        free.setBlock( rowId.getBlock() );
        free.setOffset( rowId.getOffset() );
        recordFile.release( freePage, true );
    }
}