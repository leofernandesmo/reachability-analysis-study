import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

	private static final String CTAGS_COMMAND = "ctags -x --c-kinds=f ";
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

				//Execute the "scripts"
				m.writeFileWithFunctions(inputPath, outputPath);
				m.writeFileWithDirectives(inputPath, outputPath);
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

	
	public void writeFileWithFunctions(String inputPath, String outputPath) {

		List<File> files = new ArrayList<File>();
		Utils.listFilesAndFilesSubDirectories(inputPath, files, ".c");
		for (File file : files) {
			String ctagsCommand = CTAGS_COMMAND + file.getAbsolutePath();
			List<String> linesError = new ArrayList<String>();
			List<String> linesOutput = new ArrayList<String>();

			Utils.cmdExec(ctagsCommand, linesOutput, linesError);
			List<Function> funcList = new ArrayList<Function>();

			int total = 0;
			for (String line : linesOutput) {
				Function f = new Function();
				String subAux = line;
				int indexAux = subAux.indexOf(" ");
				String funcName = subAux.substring(0, indexAux).trim();
				f.setName(funcName);
				subAux = subAux.substring(indexAux).trim();
				indexAux = subAux.indexOf(" ");
				String type = subAux.substring(0, indexAux).trim();
				subAux = subAux.substring(indexAux).trim();
				indexAux = subAux.indexOf(" ");
				String lineNumber = subAux.substring(0, indexAux).trim();
				f.setStartLine(Integer.parseInt(lineNumber));
				subAux = subAux.substring(indexAux).trim();
				indexAux = subAux.indexOf(" ");
				String filepath = subAux.substring(0, indexAux).trim();
				f.setFile(new File(filepath));
				subAux = subAux.substring(indexAux).trim();
				String funcexpr = subAux.trim();
				f.setSignature(funcexpr);

				funcList.add(f);
				total++;
			}

			try {
				FileOutputStream os = new FileOutputStream(
						new File(outputPath + "/" + file.getName() + ".out.functions"));
				String output = "";
				for (Function function : funcList) {
					linesOutput.clear();
					linesError.clear();
					String awkCommand = AWK_COMMAND + function.getStartLine() + " "
							+ function.getFile().getAbsolutePath();
					Utils.cmdExec(awkCommand, linesOutput, linesError);

					if (!linesOutput.isEmpty()) {
						int endLine = Integer.parseInt(linesOutput.get(0));
						function.setEndline(endLine);
					}
					output += function.getID() + "\n";
				}
				os.write(output.getBytes());
				System.out.println("File: " + file.getName() + " has " + total + " functions.");

			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

		}
	}

	public void writeFileWithDirectives(String inputPath, String outputPath) {
		List<File> files = new ArrayList<File>();
		Utils.listFilesAndFilesSubDirectories(inputPath, files, ".c");
		List<String> linesWithDirectives = new ArrayList<String>();
		for (File file : files) {

			String[] expStart = { "#ifdef", "# ifdef", "#  ifdef", "#   ifdef", "#ifndef", "# ifndef", "#  ifndef",
					"#   ifndef", "#if", "# if", "#  if", "#   if" };
			String[] expEnd = { "#endif", "# endif", "#  endif", "#   endif" };
			try {
				// Escreve no arquivo....
				FileOutputStream os = new FileOutputStream(
						new File(outputPath + "/" + file.getName() + ".out.directives"));
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

	

	

	private void writeFileWithMapping(String inputPath, String outputPath) {
		try {
			// List<File> filesWithFunctions = new ArrayList<File>();
			List<File> filesWithDirectives = new ArrayList<File>();
			// listFilesAndFilesSubDirectories(OUTPUT_DIRECTORY,
			// filesWithFunctions, ".functions");
			Utils.listFilesAndFilesSubDirectories(outputPath, filesWithDirectives, ".directives");

			for (File fileWithDirective : filesWithDirectives) {
				List<Function> listResult = new ArrayList<Function>();
				try {
					BufferedReader br = new BufferedReader(new FileReader(fileWithDirective));
					String output = fileWithDirective.getAbsolutePath() + "\n";
					String lineDirective = "";
					// Read Directive functions
					while ((lineDirective = br.readLine()) != null) {
						Directive directive = Directive.fromVariabilityLine(lineDirective);
						String strFunctionFile = fileWithDirective.getAbsolutePath().replace(".directives",
								".functions");
						try {
							BufferedReader brFunctions = new BufferedReader(new FileReader(strFunctionFile));

							String input = "";
							String lineFunction = "";
							while ((lineFunction = brFunctions.readLine()) != null) {
								Function function = Function.fromFunctionLine(lineFunction);
								if (listResult.contains(function)) {
									int index = listResult.indexOf(function);
									function = listResult.get(index);
								} else {
									listResult.add(function);
								}

								int startLine = directive.getStartLine();
								int endLine = directive.getEndLine();

								int functionStartLine = function.getStartLine();
								int functionEndLine = function.getEndline();
								// Directive wrap function
								if (functionStartLine >= startLine && functionStartLine <= endLine) {
									function.addVariability(directive);
									// Function wrap directive
								} else if (startLine >= functionStartLine && startLine <= functionEndLine) {
									function.addVariability(directive);
								}
							}

							brFunctions.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					br.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}

				FileOutputStream os = new FileOutputStream(new File(fileWithDirective.getAbsolutePath() + ".map"));
				String output = "";
				for (Function function : listResult) {
					String line = function.getLineWithDrectives();
					output += line + "\n";
					System.out.println(line);
				}
				os.write(output.getBytes());
				os.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
