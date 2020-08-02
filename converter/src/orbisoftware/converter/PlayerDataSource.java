
package orbisoftware.converter;

import java.io.IOException;

import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;

class PlayerDataSource extends PullBufferDataSource {
   PlayerSourceStream streams[];

   private PlayerSourceStream playerSourceStream;

   PlayerDataSource(String screenRecordingFileName) throws IOException {
      streams = new PlayerSourceStream[1];
      playerSourceStream = new PlayerSourceStream(screenRecordingFileName);
      streams[0] = playerSourceStream;
   }

   public void setLocator(MediaLocator source) {
   }

   public MediaLocator getLocator() {
      return null;
   }

   public String getContentType() {
      return ContentDescriptor.RAW;
   }

   public void connect() {
   }

   public void disconnect() {
   }

   public void start() {
   }

   public void stop() {
   }

   public PullBufferStream[] getStreams() {
      return streams;
   }

   public Time getDuration() {
      return DURATION_UNKNOWN;
   }

   public Object[] getControls() {
      return new Object[0];
   }

   public Object getControl(String type) {
      return null;
   }
}
