
package orbisoftware.recorder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;

public class AudioRecorder implements Runnable {

	private File wavFile;
	private AudioFileFormat.Type fileType;
	private AudioFormat format;
	private DataLine.Info info;
	private TargetDataLine line;
	private AudioInputStream ais;
	private String audioRecPropsFile = "audiorecprops.xml";
	private HashMap<String, String> audioRecPropsMap = new HashMap<>();

	AudioRecorder(File audioWavFile) throws Exception {
		
		wavFile = audioWavFile;
		loadXML();

		try {

			Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
			for (Mixer.Info mixerInfo : mixerInfos) {

				if (mixerInfo.getName().contains(audioRecPropsMap.get("MixerName"))) {

					Mixer mixer = AudioSystem.getMixer(mixerInfo);
							
					for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
						
						if (lineInfo.getLineClass() == javax.sound.sampled.TargetDataLine.class) {

							format = getAudioFormat();
							info = new DataLine.Info(TargetDataLine.class, format);
							line = (TargetDataLine) mixer.getLine(info);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadXML() throws Exception {

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(audioRecPropsFile);
			Element rootElem = doc.getDocumentElement();

			if (rootElem != null) {
				parseElements(rootElem);
			}
		} catch (Exception e) {

			System.out.println("Exception thrown when parsing audiorecprops.xml. Audio recording will not be enabled.");
			throw new Exception();
		}
	}

	private void parseElements(Element root) {

		String name = "";

		if (root != null) {

			NodeList nl = root.getChildNodes();

			if (nl != null) {

				for (int i = 0; i < nl.getLength(); i++) {
					Node node = nl.item(i);

					if (node.getNodeName().equalsIgnoreCase("setting")) {

						NodeList childNodes = node.getChildNodes();

						for (int j = 0; j < childNodes.getLength(); j++) {

							Node child = childNodes.item(j);

							if (child.getNodeName().equalsIgnoreCase("name"))
								name = child.getTextContent();
							else if (child.getNodeName().equalsIgnoreCase("value"))
								audioRecPropsMap.put(name, child.getTextContent());
						}
					}
				}
			}
		}
	}

	private AudioFormat getAudioFormat() {

		float sampleRate = Float.parseFloat(audioRecPropsMap.get("SampleRate"));
		int sampleSizeInBits = Integer.parseInt(audioRecPropsMap.get("SampleSizeInBits"));
		int channels = Integer.parseInt(audioRecPropsMap.get("Channels"));
		boolean signed = Boolean.parseBoolean(audioRecPropsMap.get("Signed"));
		boolean bigEndian = Boolean.parseBoolean(audioRecPropsMap.get("BigEndian"));
		AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		return format;
	}

	@Override
	public void run() {

		try {
			
//			This doesn't seem to work for mic volume control because line.getControls()
//			doesn't return anything.  Assume OS system sound level control instead.
//
//			Control[] controls = line.getControls();
//			int RECORD_VOLUME_LEVEL = 50;
//			
//			for (Control ctl : controls) {
//
//				if (ctl.getType().toString().equals("Select")) {
//					((BooleanControl) ctl).setValue(true);
//				}
//
//				if (ctl.getType().toString().equals("Volume")) {
//					FloatControl vol = (FloatControl) ctl;
//					float setVal = vol.getMinimum() + (vol.getMaximum() - vol.getMinimum()) * RECORD_VOLUME_LEVEL;
//					vol.setValue(setVal);
//				}
//			}

			line.open(format);
			line.start();

			fileType = AudioFileFormat.Type.WAVE;

			ais = new AudioInputStream(line);
			AudioSystem.write(ais, fileType, wavFile);

		} catch (Exception e) {
			System.out.println("Unexpected error in AudioRecorder thread.");
		}
	}

	void stopRecording() {

		try {
			line.stop();
			line.close();
			ais.close();
		} catch (Exception e) {
		}
	}
}