
package orbisoftware.recorder;

import java.io.File;
import java.io.FileOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class JRecorder implements ScreenRecorderListener {

	private ScreenRecorder screenRecorder;
	private File videoTemp;
	private AudioRecorder audioRecorder;
	private File audioTemp;
	private boolean audioRecordingEnabled = true;
	
	private boolean shuttingDown = false;
	@SuppressWarnings("unused")
	private int frameCount = 0;

	private ClippingSelector clippingSelector = new ClippingSelector();
	private String myOS = System.getProperty("os.name").toLowerCase();

	private Display display;
	private Shell shell;

	public boolean isWindows() {

		return (myOS.indexOf("win") >= 0);
	}

	public boolean isUnix() {

		return (myOS.indexOf("nix") >= 0 || myOS.indexOf("nux") >= 0 || myOS.indexOf("aix") > 0);
	}

	public JRecorder() {

		display = new Display();
		shell = new Shell(display, (SWT.ON_TOP | SWT.RESIZE | SWT.CLOSE | SWT.TITLE));

		RowLayout layout = new RowLayout();

		if (isWindows())
			shell.setSize(318, 70);
		else if (isUnix())
			shell.setSize(308, 37);

		shell.setText("Recorder");
		shell.setLayout(layout);
		shell.open();

		// ***** Bounds Button *****
		final Button bounds = new Button(shell, SWT.PUSH);
		bounds.setText("Bounds");
		bounds.setBounds(0, 0, 100, 30);
		Listener boundsListener = new Listener() {
			public void handleEvent(Event event) {
				clippingSelector.showClippingSelector();
			}
		};
		bounds.addListener(SWT.Selection, boundsListener);

		// ***** Start Button *****
		final Button start = new Button(shell, SWT.PUSH);
		start.setText("Start");
		start.setBounds(100, 0, 100, 30);
		Listener startListener = new Listener() {
			public void handleEvent(Event event) {
				try {
					videoTemp = File.createTempFile("videotemp", "rec");
					audioTemp = File.createTempFile("audiotemp", "wav");
					
					startVideoRecording();
					startAudioRecording();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		start.addListener(SWT.Selection, startListener);

		// ***** Stop Button *****
		final Button stop = new Button(shell, SWT.PUSH);
		stop.setText("Stop");
		stop.setBounds(200, 0, 100, 30);
		Listener stopListener = new Listener() {
			public void handleEvent(Event event) {
				if (screenRecorder != null)
					screenRecorder.stopRecording();
				if (audioRecorder != null)
					audioRecorder.stopRecording();
			}
		};
		stop.addListener(SWT.Selection, stopListener);

		// Sleep until disposed
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		display.dispose();
	}

	public void startVideoRecording() {

		if ((screenRecorder != null) || (clippingSelector.clipRect == null)) {
			return;
		}

		try {
			FileOutputStream oStream = new FileOutputStream(videoTemp.getAbsoluteFile());
			screenRecorder = new DesktopScreenRecorder(display, clippingSelector.clipRect, oStream, this);
			screenRecorder.startRecording();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void startAudioRecording() {
		
		try {
			audioRecorder = new AudioRecorder(audioTemp);
		} catch (Exception e) {
			audioRecordingEnabled = false;
			return;
		}
		
		Thread audioThread = new Thread(audioRecorder);
		audioThread.start();
	}

	public void frameRecorded() {
		frameCount++;
	}

	public void recordingStopped() {

		if (!shuttingDown) {

			FileDialog dialog = new FileDialog(shell, SWT.SAVE);
			dialog.setFilterNames(new String[] { "Beyond Capture Files" });
			dialog.setFilterExtensions(new String[] { "*.bcap" });

			dialog.setFileName("MyCapture.bcap");
			String saveFileName = dialog.open();

			// Save audio and video
			if (saveFileName != null) {
				File video = new File(saveFileName);
				File audio = new File(saveFileName.replaceAll(".bcap", ".wav"));
				
				if (video != null) {

					if (!video.getName().endsWith(".bcap"))
						video = new File(video + ".bcap");

					FileHelper.copy(videoTemp, video);
				}
				
				if (audio != null && audioRecordingEnabled) {
					
					if (!audio.getName().endsWith(".wav"))
						audio = new File(audio + ".wav");
					
					FileHelper.copy(audioTemp, audio);
				}
			}

			FileHelper.delete(videoTemp);
			FileHelper.delete(audioTemp);
			
			screenRecorder = null;
			frameCount = 0;

			shutdown();
		} else {
			FileHelper.delete(videoTemp);
			FileHelper.delete(audioTemp);
		}
	}

	public static void main(String[] args) {

		if (args.length >= 1)
			if (args[0].equals("-white_cursor"))
				DesktopScreenRecorder.useWhiteCursor = true;
			else {
				System.out.println("Usage: java -jar recorder.jar [OPTION]...");
				System.out.println("Start the screen recorder.");
				System.out.println("Options:   ");
				System.out.println("   -white_cursor   record with white cursor");
				System.exit(0);
			}
		@SuppressWarnings("unused")
		JRecorder jRecorder = new JRecorder();
	}

	public void shutdown() {

		shuttingDown = true;

		if (screenRecorder != null)
			screenRecorder.stopRecording();
		
		if (audioRecorder != null)
			audioRecorder.stopRecording();

		shell.dispose();
		System.exit(0);
	}
}
