/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ws.tools.cmd;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

/**
 * An OutputStream that flushes out to a Category.<p>
 * A simple port of <a href="mailto://Jim.Moore@rocketmail.com">Jim Moore</a>'s
 * LoggingOutputStream contribution to log4j. 
 * 
 * Note that no data is written out to the Category until the stream is
 *   flushed or closed.<p>
 * 
 * Example:<pre>
 * // make sure everything sent to System.err is logged
 * System.setErr(new PrintStream(new LoggingOutputStream(Category.getRoot(), Priority.WARN), true));
 * 
 * // make sure everything sent to System.out is also logged
 * System.setOut(new PrintStream(new LoggingOutputStream(Category.getRoot(), Priority.INFO), true));
 * </pre>
 * 
 */
final class Log4jOutputStream extends OutputStream
{
   protected static final String LINE_SEPERATOR = System.getProperty("line.separator");

   /**
    * Used to maintain the contract of {@link #close()}.
    */
   protected boolean hasBeenClosed = false;

   /**
    * The internal buffer where data is stored. 
    */
   protected byte[] buf;

   /**
    * The number of valid bytes in the buffer. This value is always 
    *   in the range <tt>0</tt> through <tt>buf.length</tt>; elements 
    *   <tt>buf[0]</tt> through <tt>buf[count-1]</tt> contain valid 
    *   byte data.
    */
   protected int count;

   /**
    * Remembers the size of the buffer for speed.
    */
   private int bufLength;

   /**
    * The default number of bytes in the buffer. =2048
    */
   public static final int DEFAULT_BUFFER_LENGTH = 2048;
   protected Logger localLogger;

   /**
    * The priority to use when writing to the log.
    */
   protected Level priority;

   @SuppressWarnings("unused")
   private Log4jOutputStream()
   {
      // illegal
   }


   /**
    * Creates the LoggingOutputStream to flush to the given log.
    *
    * @param log        the log to write to
    *
    * @param priority   the priority to use when writing to the log
    *
    * @exception IllegalArgumentException
    *                   if log == null or priority == null
    */
   public Log4jOutputStream(Logger log, Level priority) throws IllegalArgumentException
   {
      if (log == null)
      {
         throw new IllegalArgumentException("log == null");
      }
      if (priority == null)
      {
         throw new IllegalArgumentException("priority == null");
      }

      this.priority = priority;
      localLogger = log;
      bufLength = DEFAULT_BUFFER_LENGTH;
      buf = new byte[DEFAULT_BUFFER_LENGTH];
      count = 0;
   }

   /**
    * Closes this output stream and releases any system resources
    *   associated with this stream. The general contract of <code>close</code>
    *   is that it closes the output stream. A closed stream cannot perform
    *   output operations and cannot be reopened.
    */
   public void close()
   {
      flush();
      hasBeenClosed = true;
   }

   /**
    * Writes the specified byte to this output stream. The general
    * contract for <code>write</code> is that one byte is written
    * to the output stream. The byte to be written is the eight
    * low-order bits of the argument <code>b</code>. The 24
    * high-order bits of <code>b</code> are ignored.
    * 
    * @param b          the <code>byte</code> to write
    * 
    * @exception IOException
    *                   if an I/O error occurs. In particular,
    *                   an <code>IOException</code> may be thrown if the
    *                   output stream has been closed.
    */
   public void write(final int b) throws IOException
   {
      if (hasBeenClosed)
      {
         throw new IOException("The stream has been closed.");
      }

      // don't log nulls
      if (b == 0)
      {
         return;
      }

      // would this be writing past the buffer?
      if (count == bufLength)
      {
         // grow the buffer
         final int newBufLength = bufLength + DEFAULT_BUFFER_LENGTH;
         final byte[] newBuf = new byte[newBufLength];

         System.arraycopy(buf, 0, newBuf, 0, bufLength);

         buf = newBuf;
         bufLength = newBufLength;
      }

      buf[count] = (byte)b;
      count++;
   }

   /**
    * Flushes this output stream and forces any buffered output bytes
    *   to be written out. The general contract of <code>flush</code> is
    *   that calling it is an indication that, if any bytes previously
    *   written have been buffered by the implementation of the output
    *   stream, such bytes should immediately be written to their
    *   intended destination.
    */
   public void flush()
   {
      if (count == 0)
      {
         return;
      }

      // don't print out blank lines; flushing from PrintStream puts out these
      if (count == LINE_SEPERATOR.length())
      {
         if (((char)buf[0]) == LINE_SEPERATOR.charAt(0) && ((count == 1) || // <- Unix & Mac, -> Windows
               ((count == 2) && ((char)buf[1]) == LINE_SEPERATOR.charAt(1))))
         {
            reset();
            return;
         }
      }

      final byte[] theBytes = new byte[count];

      System.arraycopy(buf, 0, theBytes, 0, count);

      localLogger.log(priority, new String(theBytes));
      reset();
   }

   private void reset()
   {
      // not resetting the buffer -- assuming that if it grew that it
      //   will likely grow similarly again
      count = 0;
   }

}
