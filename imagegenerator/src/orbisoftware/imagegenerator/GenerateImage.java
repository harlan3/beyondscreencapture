
package orbisoftware.imagegenerator;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class GenerateImage implements Runnable {

	private LZ4FrameDecompressor decompressor;

	private FileInputStream iStream;
	private String videoFile;
	private String destDir;
	private int width;
	private int height;
	private boolean finished;
	private long nextFrame;

	private MemoryImageSource mis;
	
	public GenerateImage(String videoFile, String destDir) {

		this.videoFile = videoFile;
		this.destDir = destDir;

		initialize();
	}

	private void initialize() {

		finished = false;
		nextFrame = 0;

		try {

			iStream = new FileInputStream(videoFile);

			width = iStream.read();
			width = width << 8;
			width += iStream.read();

			height = iStream.read();
			height = height << 8;
			height += iStream.read();

			decompressor = new LZ4FrameDecompressor(iStream, width * height);
		} catch (Exception e) {
			e.printStackTrace();
		}

		File dir = new File(destDir);

		if (!dir.mkdir()) {
			System.out.println("Could not make dest dir: " + destDir);
			System.exit(0);
		}

		new Thread(this, "Generate Image").start();
	}

	public synchronized void run() {

		while (!finished) {
			try {
				generateFrame();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (finished)
				System.out.println("Images generated in: " + destDir);
		}
	}

	private void generateFrame() throws IOException {

		LZ4FrameDecompressor.FramePacket frame = decompressor.unpack();
		BufferedImage destImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		Image memImage = null;
		
		int result = frame.getResult();
		
		if (result == 0) {
			return;
		} else if (result == -1) {
			finished = true;
			return;
		}

		try {
			
			if (mis == null)
				mis = new MemoryImageSource(width, height, frame.getData(), 0, width);
			else
				mis.newPixels(frame.getData(), ColorModel.getRGBdefault(), 0, width);
			
			memImage = Toolkit.getDefaultToolkit().createImage(mis);
			destImage.getGraphics().drawImage(memImage, 0,  0,  null);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		nextFrame++;

		File outputFile = new File(destDir + File.separator + "IMG_" + nextFrame + ".png");

		try {
			System.out.println("Generating image: " + "IMG_" + nextFrame + ".png");
			ImageIO.write(destImage, "png", outputFile);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error writing: " + "IMG_" + nextFrame + ".png");
		}
	}
}
