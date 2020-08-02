
package orbisoftware.recorder;

import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.net.URL;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import javax.imageio.ImageIO;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

public class DesktopScreenRecorder extends ScreenRecorder {

	public static boolean useWhiteCursor;
	private BufferedImage mouseCursor;

	public DesktopScreenRecorder(Rectangle rect, OutputStream oStream, ScreenRecorderListener listener) {
		super(rect, oStream, listener);

		try {

			String mouseCursorFile;

			if (useWhiteCursor)
				mouseCursorFile = "white_cursor.png";
			else
				mouseCursorFile = "black_cursor.png";

			URL cursorURL = getClass().getClassLoader().getResource("mouse_cursors/" + mouseCursorFile);
			mouseCursor = ImageIO.read(cursorURL);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public BufferedImage captureScreen(Display display, Rectangle recordArea) {

		Point mousePosition = MouseInfo.getPointerInfo().getLocation();
		mousePosition.x = mousePosition.x - recordArea.x;
		mousePosition.y = mousePosition.y - recordArea.y;

		final Image swtImage = new Image(display, recordArea);
		final GC gc = new GC(display);
		gc.copyArea(swtImage, recordArea.x, recordArea.y);
		gc.dispose();

		//Slower method
		//BufferedImage image = ImageCoversion.convertToAWT(swtImage.getImageData());
		
		int width = swtImage.getBounds().width;
		int height = swtImage.getBounds().height;	
		int[] pixels = new int[(width * height)];
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		swtImage.getImageData().getPixels(0, 0, pixels.length, pixels, 0);
		image.getRaster().setDataElements(0, 0, width, height, pixels);
		
		Graphics2D grfx = image.createGraphics();
		grfx.drawImage(mouseCursor, mousePosition.x - 8, mousePosition.y - 5, null);
		grfx.dispose();

		return image;
	}
}
