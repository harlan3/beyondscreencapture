
package orbisoftware.player;

import java.awt.Image;

public interface ScreenPlayerListener {

	public void showNewImage(Image image);

	public void playerPaused();

	public void playerStopped();

	public void newFrame(long frameTime);

}
