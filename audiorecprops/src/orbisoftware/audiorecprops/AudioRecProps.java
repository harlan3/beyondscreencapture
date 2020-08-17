
package orbisoftware.audiorecprops;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class AudioRecProps {

	private ArrayList<String> sourceMixers;

	AudioRecProps() {
	}

	private int selectMixer() {

		int mixerCnt = 0;

		sourceMixers = new ArrayList<String>();

		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (Mixer.Info mixerInfo : mixerInfos) {

			Mixer mixer = AudioSystem.getMixer(mixerInfo);

			for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
				if (lineInfo.getLineClass() == javax.sound.sampled.TargetDataLine.class) {
					mixerCnt++;
					sourceMixers.add(mixerInfo.getName());
					System.out.println(
							"   " + mixerCnt + ". " + mixerInfo.getName() + " - " + mixerInfo.getDescription());
				}
			}
		}

		Scanner scan = new Scanner(System.in);
		System.out.println();
		System.out.print("Please select the mixer index to use for audio recording: ");

		int selectedIndex = scan.nextInt() - 1;

		scan.close();

		System.out.println();
		System.out.println("Mixer " + sourceMixers.get(selectedIndex) + " has been selected.");

		return selectedIndex;
	}

	private void displaySupportedAudioFormats(String mixerName) {

		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (Mixer.Info mixerInfo : mixerInfos) {

			if (mixerInfo.getName().equals(mixerName)) {

				Mixer mixer = AudioSystem.getMixer(mixerInfo);

				for (Line.Info lineInfo : mixer.getSourceLineInfo()) {

					if (SourceDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
						SourceDataLine.Info sourceInfo = (SourceDataLine.Info) lineInfo;
						System.out.println(sourceInfo);
						AudioFormat[] formats = sourceInfo.getFormats();
						System.out.println("  Supported Audio formats: ");
						for (AudioFormat format : formats) {
							System.out.println("    " + format);
						}
					}
				}
			}
		}
	}

	private void genAudioRecPropsXML(String mixerName) {

		try {
			PrintWriter writer = new PrintWriter(new File("audiorecprops.xml"));
			writer.println("<settings>");
			writer.println("   <setting>");
			writer.println("      <name>MixerName</name>");
			writer.println("      <value>" + mixerName + "</value>");
			writer.println("   </setting>");
			writer.println("   <setting>");
			writer.println("      <name>SampleRate</name>");
			writer.println("      <value>8000</value>");
			writer.println("   </setting>");
			writer.println("   <setting>");
			writer.println("      <name>SampleSizeInBits</name>");
			writer.println("      <value>16</value>");
			writer.println("   </setting>");
			writer.println("   <setting>");
			writer.println("      <name>Channels</name>");
			writer.println("      <value>1</value>");
			writer.println("   </setting>");
			writer.println("   <setting>");
			writer.println("      <name>Signed</name>");
			writer.println("      <value>true</value>");
			writer.println("   </setting>");
			writer.println("   <setting>");
			writer.println("      <name>BigEndian</name>");
			writer.println("      <value>true</value>");
			writer.println("   </setting>");
			writer.println("</settings>");
			writer.flush();
			writer.close();
			
			System.out.println();
			System.out.println("Copy generated audiorecprops.xml file to recorder directory to enable audio recording.");
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		AudioRecProps audioRecProps = new AudioRecProps();

		int selectedIndex = audioRecProps.selectMixer();
		audioRecProps.displaySupportedAudioFormats(audioRecProps.sourceMixers.get(selectedIndex));
		audioRecProps.genAudioRecPropsXML(audioRecProps.sourceMixers.get(selectedIndex));
	}
}