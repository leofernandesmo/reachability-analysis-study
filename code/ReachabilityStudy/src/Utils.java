import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Utils {
	/**
	 * List all files from a directory and its subdirectories (recursive)
	 * 
	 * @param directoryName
	 *            to be listed
	 */
	public static void listFilesAndFilesSubDirectories(String directoryName, List<File> filesOutput, String...extensions) {
		File directory = new File(directoryName);
		// get all the files from a directory
		File[] fList = directory.listFiles();
		for (File file : fList) {
			for (String ext : extensions) {
				if (file.isFile() && file.getName().endsWith(ext)) {
					filesOutput.add(file);
				} else if (file.isDirectory()) {
					listFilesAndFilesSubDirectories(file.getAbsolutePath(), filesOutput, extensions); 
				}
			}
			
			
		}
	}
	
	
	/**
	 * Localize a expression and change it.
	 * TODO Change to use Regex 
	 * @param file
	 * @param exprStart
	 * @param exprEnd
	 * @return
	 */
	public static List<String> localizeExpr(File file, String[] exprStart, String[] exprEnd) {
		List<String> listResult = new ArrayList<String>();
		Stack<String> exprStack = new Stack<String>();
		LineNumberReader reader;
		try {
			reader = new LineNumberReader(new FileReader(file));
			String line = "";
			while ((line = reader.readLine()) != null) {
				for (String es : exprStart) {
					if (line.contains(es) && !line.trim().startsWith("//") && !line.trim().startsWith("/*")) { // evitar
																												// comentarios
						String location = file.getAbsolutePath() + ":" + es + ":" + line + ":" + reader.getLineNumber();
						exprStack.push(location);
					}
				}
				for (String ee : exprEnd) {
					if (line.contains(ee) && !exprStack.isEmpty() && !line.trim().startsWith("//")
							&& !line.trim().startsWith("/*")) { // evitar
																// comentarios
						String location = exprStack.pop();
						location += ":" + reader.getLineNumber();
						listResult.add(location);
					}
				}
			}
			reader.close();
			if (!exprStack.isEmpty()) {
				System.err.println("---> ERRO em " + file.getAbsolutePath() + ". Verificar diretivas.");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return listResult;
	}
	
	
	/**
	 * Function to execute the command of the parameter e return the result
	 * 
	 * @param cmdLine
	 * @return
	 */
	public static void cmdExec(String cmdLine, List<String> linesOutput, List<String> linesError) {
		String line = "";
		// String output = "";
		try {
			Process p = Runtime.getRuntime().exec(cmdLine);
			// Read from ERROR
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			boolean keepRunning = true;
			while (keepRunning && (line = input.readLine()) != null) {
				// TODO Log lines error
				linesError.add(line);
				int cont = 0;
				while (!input.ready()) {
					Thread.sleep(100);
					if (cont++ == 10) {
						keepRunning = false;
						break;
					}
				}
			}
			// int exitVal = p.waitFor();
			input.close();

			// Read from default OUTPUT
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				linesOutput.add(line);
			}
			input.close();
			// exitVal = p.waitFor();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// return output;
	}

	// TODO Usar o codigo abaixo para execuatr o script shell
	// ProcessBuilder pb = new ProcessBuilder("test_command.sh",
	// ""+function.getStartLine(),
	// function.getFile().getAbsolutePath());
	// Map<String, String> env = pb.environment();
	// env.put("VAR1", "myValue");
	// env.remove("OTHERVAR");
	// env.put("VAR2", env.get("VAR1") + "suffix");
	// pb.directory(new
	// File("/home/ubuntu-vm/workspace/static-analysis/TestRegex/src/"));
	// try {
	// Process p = pb.start();
	// BufferedReader input = new BufferedReader(new
	// InputStreamReader(p.getErrorStream()));
	// String line = "";
	// while ((line = input.readLine()) != null) {
	// System.out.println(line);
	// }
	//// input.close();
	//
	//
	// } catch (IOException e) {
	//// TODO Auto-generated catch block
	// e.printStackTrace();
	// }


}
