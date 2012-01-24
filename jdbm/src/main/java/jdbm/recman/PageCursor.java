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
 * $Id: PageCursor.java,v 1.1 2000/05/06 00:00:31 boisvert Exp $
 */
package jdbm.recman;


import java.io.IOException;


/**
 * This class provides a cursor that can follow lists of pages bi-directionally.
 */
final class PageCursor
{
    /** The PageManager */
    PageManager pageManager;

    /** The current block ID */
    long blockId;

    /** The page type */
    short type;


    /**
     * Constructs a page cursor that starts at the indicated block.
     * 
     * @param pageManager The PageManager
     */
    PageCursor( PageManager pageManager, long blockId )
    {
        this.pageManager = pageManager;
        this.blockId = blockId;
    }


    /**
     * Constructs a page cursor that starts at the first block of the 
     * indicated list.
     * 
     * @param pageManager The PageManager
     * @param type The page type
     */
    PageCursor( PageManager pageManager, short type ) throws IOException
    {
        this.pageManager = pageManager;
        this.type = type;
    }


    /**
     * @return the BlockId
     */
    long getBlockId() throws IOException
    {
        return blockId;
    }


    /**
     * @return the next blockId 
     */
    long next() throws IOException
    {
        if ( blockId == 0 )
        {
            blockId = pageManager.getFirst( type );
        }
        else
        {
            blockId = pageManager.getNext( blockId );
        }

        return blockId;
    }


    /**
     * @return the previous blockId
     */
    long prev() throws IOException
    {
        blockId = pageManager.getPrev( blockId );

        return blockId;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "Location( " + blockId + ", " + type + ")";
    }
}
