
package orbisoftware.player;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.FileInputStream;
import java.io.IOException;

public class ScreenPlayer implements Runnable {

	private ScreenPlayerListener listener;

	private MemoryImageSource mis = null;
	private Rectangle area;

	private FrameDecompressor decompressor;

	private long startTime;
	private long frameTime;
	private long lastFrameTime;

	private boolean running;
	private boolean paused;
	private boolean fastForward;

	private boolean resetReq;

	private FileInputStream iStream;
	private String videoFile;
	private int width;
	private int height;

	public ScreenPlayer(String videoFile, ScreenPlayerListener listener) {

		this.listener = listener;
		this.videoFile = videoFile;

		initialize();
	}

	private void initialize() {

		startTime = System.currentTimeMillis();
		frameTime = startTime;
		lastFrameTime = startTime;
		paused = true;

		try {

			iStream = new FileInputStream(videoFile);

			width = iStream.read();
			width = width << 8;
			width += iStream.read();

			height = iStream.read();
			height = height << 8;
			height += iStream.read();

			area = new Rectangle(width, height);
			decompressor = new FrameDecompressor(iStream, width * height);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void reset_req() {

		paused = true;
		fastForward = false;
		resetReq = true;
	}

	public void reset() {

		resetReq = false;
		initialize();
	}

	public void play() {

		fastForward = false;
		paused = false;

		if (running == false) {
			new Thread(this, "Screen Player").start();
		}
	}

	public void fastforward() {
		fastForward = true;
		paused = false;
	}

	public void pause() {
		paused = true;
	}

	public void stop() {
		paused = false;
		running = false;
	}

	public synchronized void run() {

		running = true;

		while (running) {

			while (paused && !resetReq) {

				try {
					Thread.sleep(50);
				} catch (Exception e) {
				}
				startTime += 50;
			}

			try {
				readFrame();
				listener.newFrame();
			} catch (IOException ioe) {
				listener.showNewImage(null);
				break;
			}

			if (fastForward == true) {
				startTime -= (frameTime - lastFrameTime);
			} else {
				while ((System.currentTimeMillis() - startTime < frameTime) && !paused) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {
					}
				}
			}

			lastFrameTime = frameTime;
		}

		listener.playerStopped();
	}

	private void readFrame() throws IOException {

		if (resetReq) {
			reset();
			return;
		}

		FrameDecompressor.FramePacket frame = decompressor.unpack();
		frameTime = frame.getTimeStamp();

		int result = frame.getResult();
		if (result == 0) {
			return;
		} else if (result == -1) {
			paused = true;
			listener.playerPaused();
			return;
		}

		if (mis == null) {
			mis = new MemoryImageSource(area.width, area.height, frame.getData(), 0, area.width);
			mis.setAnimated(true);
			listener.showNewImage(Toolkit.getDefaultToolkit().createImage(mis));
			return;
		} else {
			mis.newPixels(frame.getData(), ColorModel.getRGBdefault(), 0, area.width);
			return;
		}
	}
}
