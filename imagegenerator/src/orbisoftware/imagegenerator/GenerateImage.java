
package orbisoftware.imagegenerator;

import java.awt.image.BufferedImage;
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
		BufferedImage destImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		
		int result = frame.getResult();
		
		if (result == 0) {
			return;
		} else if (result == -1) {
			finished = true;
			return;
		}

		try {
			
			// Using loop here because of issues putting frame data in
			// a Buffered Image.  The alpha channel causes problems.
		    for (int y = 0; y < height; y++) {
		        for (int x = 0; x < width; x++) {
		        	int rgb = frame.getData()[((y * width) + x)];
		        	destImage.setRGB(x, y, rgb);
		        }
		    }
			
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
