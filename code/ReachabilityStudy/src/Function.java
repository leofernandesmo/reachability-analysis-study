


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Function {

	private int level;
	private String name;
	private String signature;
	private File file;
	private int startline;
	private int endline;
	
	//Teste com o uso do Colligens
	private List<Directive> variabilities;
	
	
	public static Function fromFunctionLine(String line) {
		Function vd = new Function();
		String[] info = line.split(":");
		vd.setName(info[1]);
		vd.setStartLine(Integer.parseInt(info[2]));
		vd.setEndline(Integer.parseInt(info[3]));
//		vd.setSignature(info[4]);
		vd.setFile(new File(info[0]));
		return vd;
	}
	

	public Function() {
		super();
		variabilities = new ArrayList<Directive>();
	}
	
	
	public static Function fromCTags(String line, String inputPath){
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
		String strFile = subAux.substring(0, indexAux).trim();
		f.setFile(new File(inputPath + strFile.replace("./", "/")));
		subAux = subAux.substring(indexAux).trim();
		String funcexpr = subAux.trim();
		f.setSignature(funcexpr);
		return f;
		
	}
	
	public boolean containsVariablity(){
		if(variabilities != null && variabilities.size() > 0){
			return true;
		} else {
			return false;
		}
	}
	
	private void addVariability(Directive vd){
    	variabilities.add(vd);
    }
	
	public void checkVariability(Directive directive){
		int directiveStartLine = directive.getStartLine();
		int directiveEndLine = directive.getEndLine();

		int functionStartLine = getStartLine();
		int functionEndLine = getEndline();
		// Directive wrap function
		if (functionStartLine >= directiveStartLine && functionStartLine <= directiveEndLine) {
			this.addVariability(directive);
			directive.addFunction(this);
			// Function wrap directive
		} else if (directiveStartLine >= functionStartLine && directiveStartLine <= functionEndLine) {
			this.addVariability(directive);
			directive.addFunction(this);
		}
		
	}
	
	

	public void setVariabilities(List<Directive> variabilities) {
		this.variabilities = variabilities;
	}
	
	public List<Directive> getAllVariabilities(){
    	return this.variabilities;
    }
	
	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public int getStartLine() {
		return startline;
	}

	public void setStartLine(int line) {
		this.startline = line;
	}

	public int getEndline() {
		return endline;
	}

	public void setEndline(int endline) {
		this.endline = endline;
	}

	
	public String getID(){
		return getFile().getAbsolutePath() + ":" + getName() + ":" + getStartLine() + ":" + getEndline();
	}
	
	
	public String getLineToWrite(){
		String result = "";
		result = getFile().getAbsolutePath() + ":" + getName();
		for (Directive directive : getAllVariabilities()) {
			result += ":"+directive.getName();
		}
		return result;
	}

	@Override
	public String toString() {
		return this.getID();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null)
			if (obj instanceof Function)
				if (((Function) obj).getID().equals(this.getID()))
					return true;
		return false;
	}

}