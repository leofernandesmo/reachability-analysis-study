import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Stream;

public class Main {

	private static final String CODE_DIRECTIVE_WITHOUT_FUNCTION = "2:";
	private static final String CODE_FUNCTION_WITHOUT_DIRECTIVE = "1:";
	private static final String CODE_FUNCTION_WITH_DIRECTIVE = "0:";
	private static final String TEMP_CTAGS_OUT = "/temp/ctags.out";
	private static final String TEMP_COAN_OUT = "/temp/coan.out";
	private static final String TEMP_SUMMARY_OUT = "/temp/summary.out";
	private static final String EXTENSION_OUT_DIRECTIVES = ".out.directives";
	private static final String EXTENSION_OUT_MAP = ".out.map";
	private static final String EXTENSION_OUT_FUNCTIONS = ".out.functions";
	private static final String CTAGS_COMMAND = "./bin/ctags_command.sh";
	private static final String COAN_COMMAND = "./bin/coan_command.sh";
	private static final String AWK_COMMAND = "./bin/awk_command.sh ";

	public static final String LOG_FILE = "/home/leofernandesmo/workspace/reachability-analysis-study/output/logerror.log";

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

				// Count elapsed time
				long tStart = System.currentTimeMillis();

				// Execute the "scripts"
				m.writeFileWithFunctions(inputPath, outputPath);
				m.writeFileWithDirectives2(inputPath, outputPath);
				m.writeFileWithMapping(inputPath, outputPath);
				m.summary(outputPath);

				// print Elapsed time
				long tEnd = System.currentTimeMillis();
				long tDelta = tEnd - tStart;
				double elapsedSeconds = tDelta / 1000.0;
				System.out.format("Finished in %s seconds %n", elapsedSeconds);
				System.out.format("Project: %s %n", inputPath);
				System.out.format("Resulted in: %s %n", outputPath);

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

			int total = 0;
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
				total++;
			}
			System.out.println("File:" + f.getName() + " has " + total + " functions.");
			os.write(output.getBytes());
			os.close();
		}

		// Delete the tempfile.
		// tempFile.delete();

	}

	/**
	 * Versão nova do método que gera as diretivas de um arquivo em c. Nele
	 * usamos o programa COAN para descobrir as diretivas e onde elas começam.
	 * Depois apenas verificamos onde cada diretiva termina.
	 * 
	 * @param inputPath
	 * @param outputPath
	 * @throws IOException
	 */
	public void writeFileWithDirectives2(String inputPath, String outputPath) throws IOException {
		Map<File, ArrayList<Directive>> directiveList = new HashMap<File, ArrayList<Directive>>();
		File tempFile = new File(outputPath + TEMP_COAN_OUT);
		tempFile.getParentFile().mkdirs();
		tempFile.createNewFile();

		Utils.cmdExec2WithoutReturn(COAN_COMMAND, inputPath, tempFile.getAbsolutePath());

		FileInputStream inputStream = null;
		Scanner sc = null;
		try {
			inputStream = new FileInputStream(tempFile);
			sc = new Scanner(inputStream, "UTF-8");

			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				line = line.replace("(", ":");
				line = line.replace(")", ":");

				Directive f = Directive.fromCoan(line);
				int endLine = Utils.localizeEndDirective(f);
				f.setEndLine(endLine);

				if (directiveList.containsKey(f.getFile())) {
					directiveList.get(f.getFile()).add(f);
				} else {
					ArrayList<Directive> listTemp = new ArrayList<Directive>();
					listTemp.add(f);
					directiveList.put(f.getFile(), listTemp);
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

		for (File f : directiveList.keySet()) {
			ArrayList<Directive> listTemp = directiveList.get(f);

			FileOutputStream os = new FileOutputStream(
					new File(outputPath + "/" + f.getName() + EXTENSION_OUT_DIRECTIVES));
			String output = "";

			int total = 0;
			for (Directive function : listTemp) {
				output += function.getID() + "\n";
				total++;
			}
			System.out.println("File:" + f.getName() + " has " + total + " directives.");
			os.write(output.getBytes());
			os.close();

		}
	}

	/**
	 * Versão anterior do método que gerava o arquivo com diretivas. Nele
	 * usávamos apenas leitura de texto linha a linha e comparação com um
	 * conjunto de strings (ex. #if, #ifdef, #ifndef)
	 * 
	 * @param inputPath
	 * @param outputPath
	 */
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
			File fileWithDirectives = new File(
					fileWithFunction.getAbsolutePath().replace(EXTENSION_OUT_FUNCTIONS, EXTENSION_OUT_DIRECTIVES));
			if (fileWithDirectives.exists()) {
				br = new BufferedReader(new FileReader(fileWithDirectives));
				// Read Directive functions
				while ((line = br.readLine()) != null) {
					Directive directive = Directive.fromVariabilityLine(line);
					directives.add(directive);
				}
				br.close();
			}

			File mapFile = new File(
					fileWithFunction.getAbsolutePath().replace(EXTENSION_OUT_FUNCTIONS, EXTENSION_OUT_MAP));
			FileWriter fw = new FileWriter(mapFile);

			for (Function function : functions) {
				for (Directive directive : directives) {
					function.checkVariability(directive);
				}
				if (function.containsVariablity()) {
					fw.write(CODE_FUNCTION_WITH_DIRECTIVE + function.getLineToWrite() + "\n");
				} else {
					fw.write(CODE_FUNCTION_WITHOUT_DIRECTIVE + function.getLineToWrite() + "\n");
				}
			}

			for (Directive directive : directives) {
				if (!directive.containsFunction()) {
					fw.write(CODE_DIRECTIVE_WITHOUT_FUNCTION + directive.getLineToWrite() + "\n");
				}
			}

			fw.close();
		}

	}

	public void summary(String outputPath) throws IOException {
		List<File> files = new ArrayList<File>();
		Utils.listFilesAndFilesSubDirectories(outputPath, files, EXTENSION_OUT_MAP);

		FileWriter fw = new FileWriter(outputPath + TEMP_SUMMARY_OUT);

		int totalFunctions = 0;
		int totalFunctionsWithDirectives = 0;
		int totalFunctionsWithoutDirectives = 0;
		int totalDirectivesWithoutFunction = 0;
		int totalDirectives = 0;

		for (File file : files) {
			FileInputStream inputStream = new FileInputStream(file);
			Scanner sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.startsWith(CODE_FUNCTION_WITH_DIRECTIVE)) {
					totalFunctions++;
					totalFunctionsWithDirectives++;
				} else if (line.startsWith(CODE_FUNCTION_WITHOUT_DIRECTIVE)) {
					totalFunctions++;
					totalFunctionsWithoutDirectives++;
				} else if (line.startsWith(CODE_DIRECTIVE_WITHOUT_FUNCTION)) {
					totalDirectivesWithoutFunction++;
				}
			}

			sc.close();
			inputStream.close();
		}

		fw.write("Total Funtions: " + totalFunctions + "\n");
		fw.write("Total Funtions With Directives: " + totalFunctionsWithDirectives + "\n");
		fw.write("Total Funtions Without Directives: " + totalFunctionsWithoutDirectives + "\n");
		fw.write("Total Directives Without Function: " + totalDirectivesWithoutFunction + "\n");

		// Get total directives
		// files.clear();
		// Utils.listFilesAndFilesSubDirectories(outputPath, files,
		// EXTENSION_OUT_DIRECTIVES);
		//
		// for (File file : files) {
		// FileInputStream inputStream = new FileInputStream(file);
		// Scanner sc = new Scanner(inputStream, "UTF-8");
		// while (sc.hasNextLine()) {
		// totalDirectives++;
		// }
		// sc.close();
		// inputStream.close();
		// }
		//
		// fw.write("Total Directives: " + totalDirectives + "\n");
		fw.close();

	}

}
