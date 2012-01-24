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
 * $Id: FileHeader.java,v 1.3 2005/06/25 23:12:32 doomdark Exp $
 */
package jdbm.recman;


import java.io.IOException;

import org.apache.directory.server.i18n.I18n;


/**
 * This class represents a file header. It is a 1:1 representation of
 * the data that appears in block 0 of a file.<br/>
 * The FileHeader is stored into a BlockIo.<br/>
 * The BlockIo will always contain the following bytes : <br/>
 * <ul>
 * <li>[0..3] 0x1350 : a marker for a FILE_HEADER</li>
 * <li>[4..11]  : The BlockIo reference to the first FREE_PAGE ID</li>
 * <li>[12..19] : The BlockIo reference to the last FREE_PAGE ID</li>
 * <li>[20..27] : The BlockIo reference to the first USED_PAGE ID</li>
 * <li>[28..35] : The BlockIo reference to the last USED_PAGE ID</li>
 * <li>[36..43] : The BlockIo reference to the first TRANSLATION_PAGE ID</li>
 * <li>[44..51] : The BlockIo reference to the last TRANSLATION_PAGE ID</li>
 * <li>[52..59] : The BlockIo reference to the first FREELOGIDS_PAGE ID</li>
 * <li>[60..67] : The BlockIo reference to the last FREELOGIDS_PAGE ID</li>
 * <li>[68..71] : The BlockIo reference to the first FREEPHYSIDS_PAGE ID</li>
 * <li>[72..79] : The BlockIo reference to the last FREEPHYSIDS_PAGE ID</li>
 * <li>[80..87]* : The reference to the BlockIo which is the root for the data contained in this File.
 * We may have more than one, but no more than 1014, if the BLOCK_SIZE is 8192</li>
 * </ul> 
 */
class FileHeader implements BlockView
{
    /** Position of the Magic number for FileHeader */
    private static final short O_MAGIC = 0; // short magic

    /** Position of the Lists in the blockIo */
    private static final short O_LISTS = Magic.SZ_SHORT; // long[2*NLISTS]

    /** Position of the ROOTs in the blockIo */
    private static final int O_ROOTS = O_LISTS + ( Magic.NLISTS * 2 * Magic.SZ_LONG );

    /** The BlockIo used to store the FileHeader */
    private BlockIo block;

    /** The number of "root" rowids available in the file. */
    static final int NROOTS = ( RecordFile.BLOCK_SIZE - O_ROOTS ) / Magic.SZ_LONG;


    /**
     * Constructs a FileHeader object from a block.
     *
     * @param block The block that contains the file header
     * @param isNew If true, the file header is for a new file.
     * @throws IOException if the block is too short to keep the file
     *         header.
     */
    FileHeader( BlockIo block, boolean isNew )
    {
        this.block = block;

        if ( isNew )
        {
            block.writeShort( O_MAGIC, Magic.FILE_HEADER );
        }
        else if ( block.readShort( O_MAGIC ) != Magic.FILE_HEADER )
        {
            throw new Error( I18n.err( I18n.ERR_544, block.readShort( O_MAGIC ) ) );
        }
    }


    /** 
     * Returns the offset of the "first" block of the indicated list 
     */
    private short offsetOfFirst( int list )
    {
        return ( short ) ( O_LISTS + ( 2 * Magic.SZ_LONG * list ) );
    }


    /** 
     * Returns the offset of the "last" block of the indicated list 
     */
    private short offsetOfLast( int list )
    {
        return ( short ) ( offsetOfFirst( list ) + Magic.SZ_LONG );
    }


    /** 
     * Returns the offset of the indicated root 
     */
    private short offsetOfRoot( int root )
    {
        return ( short ) ( O_ROOTS + ( root * Magic.SZ_LONG ) );
    }


    /**
     * Returns the first block of the indicated list
     */
    long getFirstOf( int list )
    {
        return block.readLong( offsetOfFirst( list ) );
    }


    /**
     * Sets the first block of the indicated list
     */
    void setFirstOf( int list, long value )
    {
        block.writeLong( offsetOfFirst( list ), value );
    }


    /**
     * Returns the last block of the indicated list
     */
    long getLastOf( int list )
    {
        return block.readLong( offsetOfLast( list ) );
    }


    /**
     * Sets the last block of the indicated list
     */
    void setLastOf( int list, long value )
    {
        block.writeLong( offsetOfLast( list ), value );
    }


    /**
     *  Returns the indicated root rowid. A root rowid is a special rowid
     *  that needs to be kept between sessions. It could conceivably be
     *  stored in a special file, but as a large amount of space in the
     *  block header is wasted anyway, it's more useful to store it where
     *  it belongs.
     *
     *  @see #NROOTS
     */
    long getRoot( int root )
    {
        return block.readLong( offsetOfRoot( root ) );
    }


    /**
     *  Sets the indicated root rowid.
     *
     *  @see #getRoot
     *  @see #NROOTS
     */
    void setRoot( int root, long rowid )
    {
        block.writeLong( offsetOfRoot( root ), rowid );
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "FileHeader ( " );

        // The blockIO
        sb.append( block ).append( ", " );

        // The free pages
        sb.append( "free[" );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.FREE_PAGE ) ) ) );
        sb.append( ", " );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.FREE_PAGE ) + Magic.SZ_LONG ) ) );
        sb.append( "], " );

        // The used pages
        sb.append( "used[" );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.USED_PAGE ) ) ) );
        sb.append( ", " );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.USED_PAGE ) + Magic.SZ_LONG ) ) );
        sb.append( "], " );

        // The translation pages
        sb.append( "translation[" );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.TRANSLATION_PAGE ) ) ) );
        sb.append( ", " );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.TRANSLATION_PAGE ) + Magic.SZ_LONG ) ) );
        sb.append( "], " );

        // The freeLogIds pages
        sb.append( "freeLogIds[" );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.FREELOGIDS_PAGE ) ) ) );
        sb.append( ", " );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.FREELOGIDS_PAGE ) + Magic.SZ_LONG ) ) );
        sb.append( "], " );

        // The freePhysIds pages
        sb.append( "freePhysIds[" );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.FREEPHYSIDS_PAGE ) ) ) );
        sb.append( ", " );
        sb.append( block.readLong( ( short ) ( 2 + ( 2 * Magic.SZ_LONG * Magic.FREEPHYSIDS_PAGE ) + Magic.SZ_LONG ) ) );
        sb.append( "]" );

        sb.append( " )" );

        return sb.toString();
    }
}
