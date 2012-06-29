package org.apache.directory.server.core.shared.txn;


import java.io.File;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.txn.LeakedCursorManager;
import org.apache.directory.server.core.shared.txn.DefaultTxnManager.LogSyncer;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;


/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

public class DefaultLeakedCursorManager implements LeakedCursorManager
{
    /** List of tracked cursors */
    private ConcurrentLinkedQueue<EntryFilteringCursor> cursors = new ConcurrentLinkedQueue<EntryFilteringCursor>();

    private static int LEAK_CHECK_INTERVAL = 1000;

    private static int LEAK_TIMEOUT =  1000;

    private static final String CURSOR_SUFFIX = "cursor";

    private String cursorFolderPath;

    private AtomicInteger idx = new AtomicInteger( 0 );

    CursorChecker checker;


    public DefaultLeakedCursorManager( String folderPath )
    {
        cursorFolderPath = folderPath;
        
        File folder = new File( cursorFolderPath );
        folder.mkdirs();
    }


    public void init()
    {
        if ( checker == null )
        {
            checker = new CursorChecker();
            checker.setDaemon( true );
            checker.start();
        }
    }


    public void shutdown()
    {
        checker.interrupt();

        try
        {
            checker.join();
        }
        catch ( InterruptedException e )
        {
            //Ignore
        }
        
        cursors.clear();
    }


    public Cursor<Entry> createLeakedCursor( EntryFilteringCursor cursor ) throws Exception
    {
        File cursorFile = makeCursorFileName();

        cursorFile.createNewFile();

        RandomAccessFile raf = new RandomAccessFile( cursorFile, "rw" );

        try
        {
            raf.setLength( 0 );
            raf.getFD().sync();
        }
        finally
        {
            raf.close();
        }

        boolean canMoveBeforeFirst = false;

        if ( cursor.previous() )
        {
            cursor.next();
        }
        else
        {
            canMoveBeforeFirst = true;
        }
       

        return new RandomFileCursor( cursorFile, cursor, canMoveBeforeFirst,
                cursor.getOperationContext().getSession().getDirectoryService().getSchemaManager() );
    }


    public void trackCursor( EntryFilteringCursor cursor )
    {
        cursors.add( cursor );
    }


    private File makeCursorFileName()
    {
        int fileIdx = idx.incrementAndGet();

        return new File( cursorFolderPath + File.separatorChar + fileIdx + "."
            + CURSOR_SUFFIX );
    }


    private void checkLeakedCursors() throws Exception
    {
        Iterator<EntryFilteringCursor> it = cursors.iterator();
        EntryFilteringCursor cursor;
        long currentTimestamp = System.currentTimeMillis();

        while ( it.hasNext() )
        {
            cursor = it.next();

            if ( cursor.isClosed() )
            {   
                it.remove();
                continue;
            }

            if ( ( currentTimestamp - cursor.getTimestamp() ) >= LEAK_TIMEOUT )
            {
                cursor.pinCursor();

                if ( cursor.isClosed() )
                {
                    cursor.unpinCursor();
                    it.remove();
                    continue;
                }

                System.out.println("Doing leaked cursor management2" + cursor);
                
                try
                {
                    cursor.doLeakedCursorManagement( this );
                }
                finally
                {
                    cursor.unpinCursor();
                }

                it.remove();
                continue;

            }
            else
            {
                // Maybe break here
            }

        }
    }

    class CursorChecker extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                while ( true )
                {
                    Thread.sleep( LEAK_CHECK_INTERVAL );

                    try
                    {
                        checkLeakedCursors();
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }
            }
            catch ( InterruptedException e )
            {
                // Bail out
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }
}
