


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
	
	public void addVariability(Directive vd){
    	variabilities.add(vd);
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
	
	
	public String getLineWithDrectives(){
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