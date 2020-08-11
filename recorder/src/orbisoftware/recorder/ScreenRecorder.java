
package orbisoftware.recorder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public abstract class ScreenRecorder implements Runnable {

	private Rectangle recordArea;

	private int frameSize;
	private int[] rawData;

	private OutputStream oStream;

	private boolean recording = false;

	private long startTime;
	private long frameTime;
	private boolean reset;

	private int THREAD_FPS_SLEEP = 50;
	private int THREAD_POLL_SLEEP = THREAD_FPS_SLEEP / 2;

	private ScreenRecorderListener listener;

	private class DataPack {
		public DataPack(int[] newData, long frameTime) {
			this.newData = newData;
			this.frameTime = frameTime;
		}

		public long frameTime;
		public int[] newData;
	}

	private class StreamPacker implements Runnable {

		Queue<DataPack> queue = new LinkedList<DataPack>();
		private FrameCompressor compressor;

		public StreamPacker(OutputStream oStream, int frameSize) {
			compressor = new FrameCompressor(oStream, frameSize);

			new Thread(this, "Stream Packer").start();
		}

		public void packToStream(DataPack pack) {

			queue.add(pack);
		}

		public void run() {
			while (recording) {
				while (queue.isEmpty() == false) {
					DataPack pack = queue.poll();

					try {
						compressor.pack(pack.newData, pack.frameTime, reset);

						if (reset == true) {
							reset = false;
						}
						Thread.sleep(THREAD_POLL_SLEEP);
					} catch (Exception e) {
						e.printStackTrace();
						try {
							oStream.close();
						} catch (Exception e2) {
						}
						return;
					}
				}

				try {
					Thread.sleep(THREAD_POLL_SLEEP);
				} catch (InterruptedException e) {
				}
			}
			try {
				oStream.flush();
				oStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private StreamPacker streamPacker;

	public ScreenRecorder(Rectangle rect, OutputStream oStream, ScreenRecorderListener listener) {

		this.recordArea = rect;
		this.listener = listener;
		this.oStream = oStream;
	}

	public void triggerRecordingStop() {
		recording = false;
	}

	public synchronized void run() {

		Display.getDefault().asyncExec(new Runnable() {

			public void run() {

				final Display display = Display.getDefault();

				recording = true;

				frameSize = recordArea.width * recordArea.height;
				streamPacker = new StreamPacker(oStream, frameSize);

				try {

					while (recording) {

						recordFrame(display);
						display.readAndDispatch();

						Thread.sleep(THREAD_FPS_SLEEP);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				recording = false;

				listener.recordingStopped();
			}
		});
	}

	public abstract Image captureScreen(Display display, Rectangle recordArea);

	public void recordFrame(Display display) throws IOException {

		Image swtImage = captureScreen(display, recordArea);

		frameTime = System.currentTimeMillis() - startTime;
		rawData = new int[frameSize];

		swtImage.getImageData().getPixels(0, 0, rawData.length, rawData, 0);

		streamPacker.packToStream(new DataPack(rawData, frameTime));
		listener.frameRecorded();
	}

	public void startRecording() {

		startTime = System.currentTimeMillis();

		if (recording || (recordArea == null)) {
			return;
		}
		try {
			oStream.write((recordArea.width & 0x0000FF00) >>> 8);
			oStream.write((recordArea.width & 0x000000FF));

			oStream.write((recordArea.height & 0x0000FF00) >>> 8);
			oStream.write((recordArea.height & 0x000000FF));
		} catch (Exception e) {
			e.printStackTrace();
		}

		new Thread(this, "Screen Recorder").start();
	}

	public void stopRecording() {

		if (!recording)
			return;

		triggerRecordingStop();
	}

	public boolean isRecording() {
		return recording;
	}

	public int getFrameSize() {
		return frameSize;
	}
}
