import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class Main {

	private static final String TEMP_CTAGS_OUT = "/temp/ctags.out";
	private static final String EXTENSION_OUT_DIRECTIVES = ".out.directives";
	private static final String EXTENSION_OUT_MAP = ".out.map";
	private static final String EXTENSION_OUT_FUNCTIONS = ".out.functions";
	private static final String CTAGS_COMMAND = "./bin/ctags_command.sh";
	private static final String AWK_COMMAND = "./bin/awk_command.sh ";

	public static void main(String[] args) {

		if (args.length > 0) {
			Main m = new Main();
			InputStream inputStream = null;

			try {

				String file = args[0];
				inputStream = m.getClass().getClassLoader().getResourceAsStream(file);
				Properties prop = new Properties();
				prop.load(inputStream);

				// get the property value
				String inputPath = prop.getProperty("input");
				String outputPath = prop.getProperty("output");
				if (inputPath == null || outputPath == null)
					throw new Exception("Config file is invalid.");

				System.out.format("Project: %s %n", inputPath);
				System.out.format("Resulted in: %s %n", outputPath);

				// Count elapsed time
				long tStart = System.currentTimeMillis();

				// Execute the "scripts"
//				m.writeFileWithFunctions(inputPath, outputPath);
//				m.writeFileWithDirectives(inputPath, outputPath);
				m.writeFileWithMapping(inputPath, outputPath);

				// print Elapsed time
				long tEnd = System.currentTimeMillis();
				long tDelta = tEnd - tStart;
				double elapsedSeconds = tDelta / 1000.0;
				System.out.format("Finished in %s seconds", elapsedSeconds);

			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (Exception exc) {
				exc.printStackTrace();
			}

		} else {
			System.err.println("Config file not found. You must pass a config file as parameter.");
			System.err.println("Eg.: java ReachabilityStudy /path/to/config/configfile.config");
			System.err.println("Check a config file example in the root directory of the project.jar");
		}
	}

	public void writeFileWithFunctions(String inputPath, String outputPath) throws IOException {

		Map<File, ArrayList<Function>> funcList = new HashMap<File, ArrayList<Function>>();
		File tempFile = new File(outputPath + TEMP_CTAGS_OUT);
		tempFile.getParentFile().mkdirs();
		tempFile.createNewFile();

		Utils.cmdExec2WithoutReturn(CTAGS_COMMAND, inputPath, tempFile.getAbsolutePath());

		FileInputStream inputStream = null;
		Scanner sc = null;
		try {
			inputStream = new FileInputStream(tempFile);
			sc = new Scanner(inputStream, "UTF-8");

			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				Function f = Function.fromCTags(line, inputPath);
				System.out.println(f);

				if (funcList.containsKey(f.getFile())) {
					funcList.get(f.getFile()).add(f);
				} else {
					ArrayList<Function> listTemp = new ArrayList<Function>();
					listTemp.add(f);
					funcList.put(f.getFile(), listTemp);
				}
			}
			// note that Scanner suppresses exceptions
			if (sc.ioException() != null) {
				throw sc.ioException();
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (sc != null) {
				sc.close();
			}
		}

		// Write output...
		for (File f : funcList.keySet()) {
			ArrayList<Function> listTemp = funcList.get(f);

			FileOutputStream os = new FileOutputStream(
					new File(outputPath + "/" + f.getName() + EXTENSION_OUT_FUNCTIONS));
			String output = "";

			for (Function function : listTemp) {
				List<String> linesError = new ArrayList<String>();
				List<String> linesOutput = new ArrayList<String>();
				String awkCommand = AWK_COMMAND + function.getStartLine() + " " + function.getFile().getAbsolutePath();
				Utils.cmdExec(awkCommand, linesOutput, linesError);

				if (!linesOutput.isEmpty()) {
					int endLine = Integer.parseInt(linesOutput.get(0));
					function.setEndline(endLine);
				}
				output += function.getID() + "\n";
			}
			os.write(output.getBytes());
			os.close();
		}

		// Delete the tempfile.
		// tempFile.delete();

	}

	public void writeFileWithDirectives(String inputPath, String outputPath) {
		List<File> files = new ArrayList<File>();
		Utils.listFilesAndFilesSubDirectories(inputPath, files, ".c");
		List<String> linesWithDirectives = new ArrayList<String>();
		for (File file : files) {
			// TODO Change to regex
			String[] expStart = { "#ifdef", "# ifdef", "#  ifdef", "#   ifdef", "#ifndef", "# ifndef", "#  ifndef",
					"#   ifndef", "#if ", "# if ", "#  if ", "#   if " };
			String[] expEnd = { "#endif", "# endif", "#  endif", "#   endif" };
			try {
				// Escreve no arquivo....
				FileOutputStream os = new FileOutputStream(
						new File(outputPath + "/" + file.getName() + EXTENSION_OUT_DIRECTIVES));
				String output = "";

				linesWithDirectives = Utils.localizeExpr(file, expStart, expEnd);
				int total = 0;
				for (String directive : linesWithDirectives) {
					output += directive + "\n";
					total++;
				}

				System.out.println("File:" + file.getName() + " has " + total + " directives.");

				os.write(output.getBytes());
				os.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}

	private void writeFileWithMapping(String inputPath, String outputPath) throws IOException {

		List<File> filesWithFunctions = new ArrayList<File>();
		Utils.listFilesAndFilesSubDirectories(outputPath, filesWithFunctions, EXTENSION_OUT_FUNCTIONS);

		for (File fileWithFunction : filesWithFunctions) {
			List<Directive> directives = new ArrayList<Directive>();
			List<Function> functions = new ArrayList<Function>();

			BufferedReader br = new BufferedReader(new FileReader(fileWithFunction));
			String line = "";
			// Read Directive functions
			while ((line = br.readLine()) != null) {
				Function function = Function.fromFunctionLine(line);
				functions.add(function);
			}
			br.close();
			br = new BufferedReader(new FileReader(
					fileWithFunction.getAbsolutePath().replace(EXTENSION_OUT_FUNCTIONS, EXTENSION_OUT_DIRECTIVES)));
			// Read Directive functions
			while ((line = br.readLine()) != null) {
				Directive directive = Directive.fromVariabilityLine(line);
				directives.add(directive);
			}
			br.close();

			File mapFile = new File(
					fileWithFunction.getAbsolutePath().replace(EXTENSION_OUT_FUNCTIONS, EXTENSION_OUT_MAP));
			FileWriter fw = new FileWriter(mapFile);

			for (Function function : functions) {
				for (Directive directive : directives) {
					function.checkVariability(directive);
				}
				if (function.containsVariablity()) {
					fw.write("0:" + function.getLineToWrite() + "\n");
				} else {
					fw.write("1:" + function.getLineToWrite() + "\n");
				}
			}

			for (Directive directive : directives) {
				if (!directive.containsFunction()) {
					fw.write("2:" + directive.getLineToWrite() + "\n");
				}
			}

			fw.close();			
		}

	}

}
