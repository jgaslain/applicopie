
public class Texte {
	
	private String titre;
	private String corps;
	
	public Texte(String titre, String corps) {
		super();
		this.titre = titre;
		this.corps = corps;
	}

	public String getTitre() {
		return titre;
	}

	public String getCorps() {
		return corps;
	}

	@Override
	public String toString() {
		return this.titre;
	}
	
	
}
