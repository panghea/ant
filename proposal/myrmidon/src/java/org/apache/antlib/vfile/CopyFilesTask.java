/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.FileType;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * A task that copies files.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 *
 * @ant:task name="v-copy"
 */
public class CopyFilesTask
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( CopyFilesTask.class );

    private FileObject m_srcFile;
    private FileObject m_destFile;
    private FileObject m_destDir;
    private ArrayList m_fileSets = new ArrayList();

    /**
     * Sets the source file.
     */
    public void setFile( final FileObject file )
    {
        m_srcFile = file;
    }

    /**
     * Sets the destination file.
     */
    public void setTofile( final FileObject file )
    {
        m_destFile = file;
    }

    /**
     * Sets the destination directory.
     */
    public void setTodir( final FileObject file )
    {
        m_destDir = file;
    }

    /**
     * Adds a source file set.
     */
    public void add( final FileSet fileset )
    {
        m_fileSets.add( fileset );
    }

    /**
     * Execute task.
     * This method is called to perform actual work associated with task.
     * It is called after Task has been Configured and Initialized and before
     * beig Disposed (If task implements appropriate interfaces).
     *
     * @exception TaskException if an error occurs
     */
    public void execute()
        throws TaskException
    {
        if( m_srcFile == null && m_fileSets.size() == 0 )
        {
            final String message = REZ.getString( "copyfilestask.no-source.error", getContext().getName() );
            throw new TaskException( message );
        }
        if( m_destFile == null && m_destDir == null )
        {
            final String message = REZ.getString( "copyfilestask.no-destination.error", getContext().getName() );
            throw new TaskException( message );
        }
        if( m_fileSets.size() > 0 && m_destDir == null )
        {
            final String message = REZ.getString( "copyfilestask.no-destination-dir.error", getContext().getName() );
            throw new TaskException( message );
        }

        try
        {
            // Copy the source file across
            if( m_srcFile != null )
            {
                if( m_destFile == null )
                {
                    m_destFile = m_destDir.resolveFile( m_srcFile.getName().getBaseName() );
                }

                copyFile( m_srcFile, m_destFile );
            }

            // Copy the contents of the filesets across
            for( Iterator iterator = m_fileSets.iterator(); iterator.hasNext(); )
            {
                FileSet fileset = (FileSet)iterator.next();
                FileSetResult result = fileset.getResult( getContext() );
                final FileObject[] files = result.getFiles();
                final String[] paths = result.getPaths();
                for( int i = 0; i < files.length; i++ )
                {
                    final FileObject srcFile = files[ i ];
                    final String path = paths[ i ];

                    // TODO - map destination name

                    // TODO - maybe include empty dirs
                    if( srcFile.getType() != FileType.FILE )
                    {
                        continue;
                    }

                    // TODO - use scope here, to make sure that the result
                    // is a descendent of the dest dir
                    final FileObject destFile = m_destDir.resolveFile( path );
                    copyFile( srcFile, destFile );
                }
            }
        }
        catch( FileSystemException e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }

    /**
     * Copies a file.
     */
    private void copyFile( final FileObject srcFile, final FileObject destFile )
        throws TaskException
    {
        getLogger().info( "copy " + srcFile + " to " + destFile );

        try
        {
            // TODO - move copy behind FileObject interface
            InputStream instr = srcFile.getContent().getInputStream();
            try
            {
                OutputStream outstr = destFile.getContent().getOutputStream();
                byte[] buffer = new byte[ 4096 ];
                while( true )
                {
                    int nread = instr.read( buffer );
                    if( nread == -1 )
                    {
                        break;
                    }
                    outstr.write( buffer, 0, nread );
                }
                outstr.close();
            }
            finally
            {
                instr.close();
            }
        }
        catch( Exception exc )
        {
            final String message = REZ.getString( "copyfilestask.copy-file.error", srcFile, destFile );
            throw new TaskException( message, exc );
        }
    }
}
