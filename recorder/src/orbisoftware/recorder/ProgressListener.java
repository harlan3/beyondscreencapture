
package orbisoftware.recorder;

public interface ProgressListener {

	void finished();

	void message(String message);

	void progress(long poisition, long end);
}
