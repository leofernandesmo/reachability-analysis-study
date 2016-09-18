
import java.io.File;

public class Directive {
	private File file;
	private String type;
	private int startLine;
	private int endLine;
	private String name;

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
