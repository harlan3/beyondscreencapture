package orbisoftware.imagegenerator;

public class ImageGenerator {

	public static void main(String[] args) {

		if ((args.length != 2) || !args[0].endsWith("bcap") || args[1].length() < 1) {
			System.out.println("Usage: java -jar imagegenerator.jar <above_screen_cap_file> <destination_dir>");
			return;
		}

		try {
			GenerateImage generateImage = new GenerateImage(args[0], args[1]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
