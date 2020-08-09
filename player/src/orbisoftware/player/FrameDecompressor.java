
package orbisoftware.player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class FrameDecompressor {
	private static final int ALPHA = 0xFF000000;

	public class FramePacket {

		private InputStream iStream;
		private int[] previousData;
		private int result;
		private long frameTimeStamp;
		private byte[] packed;
		private int frameSize;
		private int[] newData;

		private FramePacket(InputStream iStream, int expectedSize) {
			this.frameSize = expectedSize;
			this.iStream = iStream;
			previousData = new int[frameSize];
		}

		private void nextFrame() {
			if (newData != null) {
				previousData = newData;
			}
		}

		public int[] getData() {
			return newData;
		}

		public int getResult() {
			return result;
		}

		public long getTimeStamp() {
			return frameTimeStamp;
		}
	}

	public FramePacket frame;

	public FrameDecompressor(InputStream iStream, int frameSize) {
		frame = new FramePacket(iStream, frameSize);
	}

	public FramePacket unpack() throws IOException {
		frame.nextFrame();

		// try{
		int i = frame.iStream.read();
		int time = i;
		time = time << 8;
		i = frame.iStream.read();
		time += i;
		time = time << 8;
		i = frame.iStream.read();
		time += i;
		time = time << 8;
		i = frame.iStream.read();
		time += i;

		frame.frameTimeStamp = (long) time;

		byte type = (byte) frame.iStream.read();

		if (type <= 0) {
			frame.result = type;
			return frame;
		}

		ByteArrayOutputStream bO = new ByteArrayOutputStream();
		try {
			i = frame.iStream.read();
			int zSize = i;
			zSize = zSize << 8;
			i = frame.iStream.read();
			zSize += i;
			zSize = zSize << 8;
			i = frame.iStream.read();
			zSize += i;
			zSize = zSize << 8;
			i = frame.iStream.read();
			zSize += i;

			byte[] zData = new byte[zSize];
			int readCursor = 0;
			int sizeRead = 0;

			while (sizeRead > -1) {
				readCursor += sizeRead;
				if (readCursor >= zSize) {
					break;
				}

				sizeRead = frame.iStream.read(zData, readCursor, zSize - readCursor);
			}

			ByteArrayInputStream bI = new ByteArrayInputStream(zData);
			GZIPInputStream zI = new GZIPInputStream(bI);

			byte[] buffer = new byte[1000];
			sizeRead = zI.read(buffer);

			while (sizeRead > -1) {
				bO.write(buffer, 0, sizeRead);
				bO.flush();

				sizeRead = zI.read(buffer);
			}
			bO.flush();
			bO.close();
		} catch (Exception e) {
			e.printStackTrace();
			frame.result = 0;
			return frame;
		}

		frame.packed = bO.toByteArray();

		runLengthDecode();

		return frame;
	}

	private void runLengthDecode() {
		frame.newData = new int[frame.frameSize];

		int inCursor = 0;
		int outCursor = 0;

		int blockSize = 0;

		int rgb = 0xFF000000;

		while (inCursor < frame.packed.length && outCursor < frame.frameSize) {
			if (frame.packed[inCursor] == -1) {
				inCursor++;

				int count = (frame.packed[inCursor] & 0xFF);
				inCursor++;

				int size = count * 126;
				if (size > frame.newData.length) {
					size = frame.newData.length;
				}

				for (int loop = 0; loop < (126 * count); loop++) {
					frame.newData[outCursor] = frame.previousData[outCursor];
					outCursor++;
					if (outCursor == frame.newData.length) {
						break;
					}
				}

			} else if (frame.packed[inCursor] < 0) // uncomp
			{
				blockSize = frame.packed[inCursor] & 0x7F;
				inCursor++;

				for (int loop = 0; loop < blockSize; loop++) {
					rgb = ((frame.packed[inCursor] & 0xFF) << 16) | ((frame.packed[inCursor + 1] & 0xFF) << 8)
							| (frame.packed[inCursor + 2] & 0xFF) | ALPHA;
					if (rgb == ALPHA) {
						rgb = frame.previousData[outCursor];
					}
					inCursor += 3;
					frame.newData[outCursor] = rgb;
					outCursor++;
					if (outCursor == frame.newData.length) {
						break;
					}
				}
			} else {
				blockSize = frame.packed[inCursor];
				inCursor++;
				rgb = ((frame.packed[inCursor] & 0xFF) << 16) | ((frame.packed[inCursor + 1] & 0xFF) << 8)
						| (frame.packed[inCursor + 2] & 0xFF) | ALPHA;

				boolean transparent = false;
				if (rgb == ALPHA) {
					transparent = true;
				}
				inCursor += 3;
				for (int loop = 0; loop < blockSize; loop++) {
					if (transparent) {
						frame.newData[outCursor] = frame.previousData[outCursor];
					} else {
						frame.newData[outCursor] = rgb;
					}
					outCursor++;
					if (outCursor == frame.newData.length) {
						break;
					}
				}
			}
		}
		frame.result = outCursor;
	}
}
