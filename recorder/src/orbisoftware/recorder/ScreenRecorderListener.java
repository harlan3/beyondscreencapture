
package orbisoftware.recorder;

import java.io.IOException;

public interface ScreenRecorderListener {

	public void frameRecorded() throws IOException;

	public void recordingStopped();
}
