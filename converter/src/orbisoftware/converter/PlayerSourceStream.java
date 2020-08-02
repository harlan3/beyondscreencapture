
package orbisoftware.converter;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullBufferStream;

class PlayerSourceStream implements PullBufferStream {

   FileInputStream inStream;
   RecordingStream recordingStream;
   int width, height, frameRate;
   VideoFormat format;
   RenderedImage image;
   int nextImage = 0;
   boolean ended = false;

   public PlayerSourceStream(String screenRecordingFileName) throws IOException {

      inStream = new FileInputStream(screenRecordingFileName);
      recordingStream = new RecordingStream(inStream);

      width = (int) recordingStream.getArea().getWidth();
      height = (int) recordingStream.getArea().getHeight();
      frameRate = 5;
      format = new VideoFormat(VideoFormat.JPEG, new Dimension(width, height),
            Format.NOT_SPECIFIED, Format.byteArray, (float) frameRate);
   }

   public boolean willReadBlock() {
      return false;
   }

   public void read(Buffer buffer) throws IOException {

      if (recordingStream.isFinished()) {

         System.out.println("Done reading all images.");
         buffer.setEOM(true);
         buffer.setOffset(0);
         buffer.setLength(0);

         return;
      }

      RenderedImage newImage = recordingStream.readFrame();
      if (newImage != null) {
         image = newImage;
      }

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);

      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      writer.setOutput(ios);

      ImageWriteParam iwp = writer.getDefaultWriteParam();
      iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      iwp.setCompressionType("JPEG");
      iwp.setCompressionQuality(1);

      writer.write(null, new IIOImage(image, null, null), iwp);
      writer.dispose();

      byte[] data = outputStream.toByteArray();
      nextImage++;
      System.out.println("Processing frame: " + nextImage);
      buffer.setData(data);
      buffer.setOffset(0);
      buffer.setLength(data.length);
      buffer.setFormat(format);
      buffer.setFlags(buffer.getFlags() | Buffer.FLAG_KEY_FRAME);
   }

   public Format getFormat() {
      return format;
   }

   public ContentDescriptor getContentDescriptor() {
      return new ContentDescriptor(ContentDescriptor.RAW);
   }

   public long getContentLength() {
      return 0;
   }

   public boolean endOfStream() {
      return ended;
   }

   public Object[] getControls() {
      return new Object[0];
   }

   public Object getControl(String type) {
      return null;
   }
}
