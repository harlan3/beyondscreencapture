
package orbisoftware.player;

import java.io.IOException;
import java.io.InputStream;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

public class FrameDecompressor {
	private static final int ALPHA = 0xFF000000;

	private LZ4Factory factory = LZ4Factory.fastestInstance();
	private LZ4FastDecompressor decompressor = factory.fastDecompressor();

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
			frame.iStream.read(zData, 0, zSize);

			frame.packed = decompressor.decompress(zData, (frame.frameSize * 4));

		} catch (Exception e) {
			e.printStackTrace();
			frame.result = 0;
			return frame;
		}

		runLengthDecode();

		return frame;
	}

	private void runLengthDecode() {
		frame.newData = new int[frame.frameSize];

		int inCursor = 0;
		int outCursor = 0;

		int blockSize = 0;

		int rgb = 0xFF000000;

		while (inCursor < (frame.packed.length - 4) && outCursor < frame.frameSize) {
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
