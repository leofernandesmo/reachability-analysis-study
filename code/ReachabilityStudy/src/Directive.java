
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Directive {
	private File file;
	private String type;
	private int startLine;
	private int endLine;
	private String name;
	
	private List<Function> functions;
	
	
	

	public Directive() {
		super();
		functions = new ArrayList<Function>();
	}

	public static Directive fromVariabilityLine(String line) {
		Directive vd = new Directive();
		// try {
		String[] info = line.split(":");
		vd.setType(info[1]);
		vd.setName(info[2]);
		vd.setStartLine(Integer.parseInt(info[3]));
		vd.setEndLine(Integer.parseInt(info[4]));
		vd.setFile(new File(info[0]));
		// } catch (NumberFormatException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		return vd;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getStartLine() {
		return startLine;
	}

	public void setStartLine(int line) {
		this.startLine = line;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getEndLine() {
		return endLine;
	}

	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}
	
	
	public void addFunction(Function f){
    	functions.add(f);
    }
	
	public void setFunctions(List<Function> functions) {
		this.functions = functions;
	}
	
	public List<Function> getAllFunctions(){
    	return this.functions;
    }
	
	
	public boolean containsFunction(){
		if(functions != null && functions.size() > 0){
			return true;
		} else {
			return false;
		}
	}
	
	
	public String getLineToWrite(){
		String result = "";
		result = getFile().getAbsolutePath() + ":" + getName();
		return result;
	}

	@Override
	public String toString() {
		return getID();
	}

	public String getID() {
		return getFile().getName() + ":" + getName() + ":" + getStartLine();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null)
			if (obj instanceof Directive)
				if (((Directive) obj).getID().equals(this.getID()))
					return true;
		return false;
	}

}
