
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

	private LZ4FrameDecompressor decompressor;

	private long startTime;
	private long frameTime;
	private long lastFrameTime;

	private boolean running;
	private boolean paused;
	private boolean fastForward;
	private long pauseOffsetTime;
	private long systemTimeOffset;

	private boolean resetReq;

	private FileInputStream iStream;
	private String videoFile;
	private int width;
	private int height;

	private int THREAD_WAITING = 10;

	public ScreenPlayer(String videoFile, ScreenPlayerListener listener) {

		this.listener = listener;
		this.videoFile = videoFile;

		initialize();
	}

	private void initialize() {

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
			decompressor = new LZ4FrameDecompressor(iStream, width * height);
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
		pauseOffsetTime = 0;
		initialize();
	}

	public void play() {

		systemTimeOffset = 0;

		if (paused)
			startTime = System.currentTimeMillis() - pauseOffsetTime;
		else if (fastForward) {
			systemTimeOffset = lastFrameTime;
			startTime = System.currentTimeMillis();
		} else
			startTime = System.currentTimeMillis();

		pauseOffsetTime = 0;

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
		pauseOffsetTime = lastFrameTime;
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
					Thread.sleep(THREAD_WAITING);
				} catch (Exception e) {
				}
			}

			try {
				readFrame();
				listener.newFrame(frameTime);
			} catch (IOException ioe) {
				listener.showNewImage(null);
				break;
			}

			if (fastForward == true) {
				startTime -= (frameTime - lastFrameTime);
			} else {
				while ((System.currentTimeMillis() + systemTimeOffset - startTime < frameTime) && !paused) {
					try {
						Thread.sleep(THREAD_WAITING);
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

		LZ4FrameDecompressor.FramePacket frame = decompressor.unpack();

		int result = frame.getResult();
		if (result == 0) {
			return;
		} else if (result == -1) {
			paused = true;
			listener.playerPaused();
			return;
		}
		
		frameTime = frame.getTimeStamp();

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
