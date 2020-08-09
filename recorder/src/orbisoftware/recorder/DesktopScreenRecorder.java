
package orbisoftware.recorder;

import java.awt.MouseInfo;
import java.awt.Point;
import java.io.File;
import java.io.OutputStream;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.ImageLoader;

public class DesktopScreenRecorder extends ScreenRecorder {

	public static boolean useWhiteCursor;
	private Image mouseCursor;

	private String myOS = System.getProperty("os.name").toLowerCase();

	public DesktopScreenRecorder(Display display, Rectangle rect, OutputStream oStream,
			ScreenRecorderListener listener) {
		super(rect, oStream, listener);

		try {
			String mouseCursorFile;

			if (useWhiteCursor)
				mouseCursorFile = "white_cursor.png";
			else
				mouseCursorFile = "black_cursor.png";

			File file = new File("mouse_cursors" + File.separator + mouseCursorFile);
			ImageData[] cursorImageData = new ImageLoader().load(file.getAbsolutePath());
			mouseCursor = new Image(display, cursorImageData[0]);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isWindows() {

		return (myOS.indexOf("win") >= 0);
	}

	public Image captureScreen(Display display, Rectangle recordArea) {

		Point mousePosition = MouseInfo.getPointerInfo().getLocation();
		mousePosition.x = mousePosition.x - recordArea.x;
		mousePosition.y = mousePosition.y - recordArea.y;
		Image swtImage;

		if (isWindows()) {
			PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
			ImageData imageData = new ImageData(recordArea.width, recordArea.height, 24, palette);
			swtImage = new Image(display, imageData);
		} else
			swtImage = new Image(display, recordArea);

		final GC gc = new GC(display);
		gc.copyArea(swtImage, recordArea.x, recordArea.y);
		gc.dispose();

		final GC gc2 = new GC(swtImage);
		gc2.drawImage(mouseCursor, mousePosition.x - 8, mousePosition.y - 5);
		gc2.dispose();

		return swtImage;
	}
}
