
/*
 * This code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, 
 * MA  02111-1307, USA.
 */
/*
 * Updated by Cheng Chi for personal usage, 2012.01
 */
package btracevisualmonitor;



import java.util.*;
import java.io.File;
import java.lang.ref.WeakReference;



/**
 * Class for monitoring changes in disk files.
 * Usage:
 *
 *    1. Implement the FileListener interface.
 *    2. Create a FileMonitor instance.
 *    3. Add the file(s)/directory(ies) to listen for.
 *
 * fileChanged() will be called when a monitored file is created,
 * deleted or its modified time changes.
 *
 * @author <a href="mailto:info@geosoft.no">GeoSoft</a>
 */   
public class FileMonitor
{
  private Timer       timer_;
  private HashMap     files_;       // File -> Long
  private Collection  listeners_;   // of WeakReference(FileListener)

  /**
   * Create a file monitor instance with specified polling interval.
   * 
   * @param pollingInterval  Polling interval in milli seconds.
   */
  public FileMonitor (long pollingInterval)
  {
    files_     = new HashMap();
    listeners_ = new ArrayList();

    timer_ = new Timer (true);
    //timer_.schedule (new FileMonitorNotifierByModTime(), 0, pollingInterval);
    timer_.schedule ( new FileMonitorNotifierBySize(), 0, pollingInterval);
  }

  /**
   * Stop the file monitor polling.
   */
  public void stop()
  {
    timer_.cancel();
  }
  

  /**
   * Add file to listen for. File may be any java.io.File (including a
   * directory) and may well be a non-existing file in the case where the
   * creating of the file is to be trepped.
   * <p>
   * More than one file can be listened for. When the specified file is
   * created, modified or deleted, listeners are notified.
   * 
   * @param file  File to listen for.
   */
  public void addFileWithModTime (File file)
  {
    if (!files_.containsKey (file)) {
      long modifiedTime = file.exists() ? file.lastModified() : -1;
      files_.put (file, new Long (modifiedTime));
    }
  }

  public void addFileWithSize(File file)
  {
    if (!files_.containsKey (file)) {
      long filesize = file.exists() ? file.length() : -1;
      files_.put (file, new Long (filesize));
    }
  }

  /**
   * Remove specified file for listening.
   * 
   * @param file  File to remove.
   */
  public void removeFile (File file)
  {
    files_.remove (file);
  }


  
  /**
   * Add listener to this file monitor.
   * 
   * @param fileListener  Listener to add.
   */
  public void addListener (FileListener fileListener)
  {
    // Don't add if its already there
    for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
        
      FileListener listener = (FileListener) i.next();
      if (listener == fileListener)
        return;
    }

    // add just once
    listeners_.add (fileListener);
  }
 
  /**
   * Remove listener from this file monitor.
   * 
   * @param fileListener  Listener to remove.
   */
  public void removeListener (FileListener fileListener)
  {
    for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
      FileListener listener = (FileListener) i.next();
      if (listener == fileListener) {
        i.remove();
        break;
      }
    }
  }


  
  /**
   * This is the timer thread which is executed every n milliseconds
   * according to the setting of the file monitor. It investigates the
   * file in question and notify listeners if changed.
   */
  private class FileMonitorNotifierByModTime extends TimerTask
  {
    public void run()
    {
      // Loop over the registered files and see which have changed.
      // Use a copy of the list in case listener wants to alter the
      // list within its fileChanged method.
      Collection files = new ArrayList (files_.keySet());
      
      for (Iterator i = files.iterator(); i.hasNext(); ) {
        File file = (File) i.next();
        long lastModifiedTime = ((Long) files_.get (file)).longValue();
        long newModifiedTime  = file.exists() ? file.lastModified() : -1;

        // Chek if file has changed
        if (newModifiedTime != lastModifiedTime) {

          //System.out.println("listeners:" + listeners_);
          // Register new modified time
          files_.put (file, new Long (newModifiedTime));

          // Notify listeners
          for (Iterator j = listeners_.iterator(); j.hasNext(); ) {
            FileListener listener = (FileListener) j.next();
            
            listener.fileChanged(file);
          }
        }
      }
    }
  }
  
  private class FileMonitorNotifierBySize extends TimerTask
  {
    public void run()
    {
      // Loop over the registered files and see which have changed.
      // Use a copy of the list in case listener wants to alter the
      // list within its fileChanged method.
      Collection files = new ArrayList (files_.keySet());
      
      for (Iterator i = files.iterator(); i.hasNext(); ) {
        File file = (File) i.next();
        long fileSize = ((Long) files_.get (file)).longValue();
        long newFileSize  = file.exists() ? file.length() : -1;
        
        // Chek if file has changed
        if (newFileSize != fileSize) {

          //System.out.println("listeners:" + listeners_);
          // Register new modified time
          files_.put (file, new Long (newFileSize));

          // Notify listeners
          for (Iterator j = listeners_.iterator(); j.hasNext(); ) {
            FileListener listener = (FileListener) j.next();
            
            listener.fileChanged(file);
          }
        }
      }
    }
  }
}

