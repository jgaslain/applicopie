import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class MainApplication extends Application {
	
	private static final double DEFAULT_FONT_SIZE = 20;
	private static final String DEFAULT_FONT = "Verdana";
	private static final String PROPERTIES_FILE = "config.ini";
	private static final String REPERTOIRE_TEXTES = "TEXTES";
	boolean stop = true;
	private List<Text> mots;
	private VBox vbox;
	private ComboBox<Texte> selectionnerTexte;
	private String prenom;
	private Alert alert;
	private TextArea saisie;
	private Stage stage;
	private int vitesse = 30;
	private int vitesseEffacement = 1000;

	@Override
	public void start(Stage stage) throws Exception {
		chargerProprietes();
		this.stage = stage;
		this.stage.setTitle("Applicopie");
		initStage(true);
	}
	
	private void initStage(boolean demanderPrenom) {
		
		Scene scene = new Scene(new Group(), 1000, 800);
		
        Group root = (Group) scene.getRoot();
        
		GridPane grid = new GridPane();
        grid.setVgap(4);
        grid.setHgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.add(new Label("Sélectionner le texte : "), 0, 0);
        
        initialiserSelectionText();
        grid.add(selectionnerTexte, 1, 0);
        
        Button commencer = new Button("Commencer");
        commencer.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
            	commencer.setDisable(true);
            	afficherTexte();
            	afficherSaisie();
            }
        });
        grid.add(commencer,  2,  0);
        
        root.getChildren().add(grid);
        
        vbox = new VBox();
		vbox.setPadding(new Insets(10, 10, 10, 10));
        root.getChildren().add(vbox);
        
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			
			@Override
			public void handle(WindowEvent event) {
				stop = true;
		        Platform.exit();
		        System.exit(0);
			}
		});
		
		root.setDisable(true);
		
		if (demanderPrenom) {
			TextInputDialog fenetrePrenom = new TextInputDialog();
			fenetrePrenom.setTitle("Bonjour");
			fenetrePrenom.setHeaderText("Entre ton prénom afin d'enregistrer tes résultats");
			fenetrePrenom.setContentText("Prénom :");
			Optional<String> prenom = fenetrePrenom.showAndWait();
			this.prenom = prenom.get();
			stage.setTitle(stage.getTitle() + " de " + this.prenom);
		}
		
		stage.setResizable(true);
		
		root.setDisable(false);
		
		alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Recommencer ?");
		alert.setHeaderText("Recommencer une autre saisie ?");
    }

	private void chargerProprietes() {
		final Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream(PROPERTIES_FILE);
			prop.load(input);
			if (prop.getProperty("vitesse") != null) {
				vitesse = Integer.parseInt(prop.getProperty("vitesse"));
			}
			if (prop.getProperty("vitesse-effacement") != null) {
				vitesseEffacement  = Integer.parseInt(prop.getProperty("vitesse-effacement"));
			}

		} catch (final IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void afficherSaisie() {
		saisie = new TextArea();
		saisie.setFont(Font.font(DEFAULT_FONT, DEFAULT_FONT_SIZE));
        saisie.setWrapText(true);
        saisie.setPrefRowCount(10);
        saisie.setOnKeyPressed(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				demarrerThread();
			}
		});
		vbox.getChildren().add(saisie);
	}

	private void afficherTexte() {
    	// Initialiser texte et démarrer thread
		mots = new ArrayList<>();
		TextFlow layout = new TextFlow();
		layout.setPadding(new Insets(40, 10, 10, 10));
		layout.setMaxWidth(900);
        vbox.getChildren().add(layout);
		for (String mot : selectionnerTexte.getSelectionModel().getSelectedItem().getCorps().split(" ")) {
			Text lettre = new Text(mot + " ");
			lettre.setFont(Font.font(DEFAULT_FONT, DEFAULT_FONT_SIZE));
			mots.add(lettre);
			layout.getChildren().add(lettre);
		}
	}

    private void initialiserSelectionText() {
    	selectionnerTexte = new ComboBox<>(lireFichiers());
    	selectionnerTexte.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Texte>() {
			@Override
			public void changed(ObservableValue<? extends Texte> observable, Texte oldValue, Texte newValue) {
				selectionnerTexte.setDisable(true);
			}
		});
	}

	private ObservableList<Texte> lireFichiers() {
		
    	ObservableList<Texte> list = FXCollections.observableArrayList();
    	String nomRepertoire = REPERTOIRE_TEXTES;
		File repertoire = new File(nomRepertoire);
		String liste[] = repertoire.list();
		if (liste != null) {
			for (int i = 0; i < liste.length; i++) {
				try {
					String titre = null;
					StringBuffer corps = new StringBuffer();
					InputStream flux = new FileInputStream(nomRepertoire + "/" + liste[i]);
					InputStreamReader lecture = new InputStreamReader(flux, StandardCharsets.UTF_8);
					BufferedReader buff = new BufferedReader(lecture);
					String ligne = buff.readLine();
					if (ligne != null) {
						titre = ligne;
					}
					while ((ligne = buff.readLine()) != null) {
						corps.append(ligne).append("\n");
					}
					buff.close();
					
					list.add(new Texte(titre, corps.toString()));
				} catch (Exception e) {
					System.out.println(e.toString());
				}
			}
		} else {
			System.err.println("Nom de repertoire invalide : " + nomRepertoire);
		}
    	return list;
	}

	protected synchronized void demarrerThread() {
		if (stop == true) {
			System.out.println("Démarrage du thread");
			stop = false;
			new Thread() {
	        	
				@Override
				public void run() {
					try {
						while(!stop) {
							sleep(2000);
							for (Text mot : mots) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {
										
										final Animation animation = new Transition() {

								            {
								                setCycleDuration(Duration.millis(5 * vitesseEffacement + mot.getText().length() * vitesseEffacement));
								                setInterpolator(Interpolator.EASE_OUT);
								            }

								            @Override
								            protected void interpolate(double frac) {
								                Color vColor = new Color(0, 0, 0, 1 - frac);
								                mot.setFill(vColor);
								            }
								        };
								        animation.play();
										
									}
								});
								sleep(50000 / vitesse);
								if (stop) {
									break;
								}
							}
							// Tout le texte est effacé
							sleep(5000);
							enregistrer();
							afficherQuitterOuRecommencer();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
	        	
	        }.start();
		}
	}

	protected void afficherQuitterOuRecommencer() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (!alert.isShowing()) {
					Optional<ButtonType> result = alert.showAndWait();
					if (result.get() == ButtonType.OK){
					    initStage(false);
					} else {
						stop = true;
						stage.close();
				        Platform.exit();
				        System.exit(0);
					}
				}
			}
		});
	}

	private void enregistrer() {
		if (this.saisie.getText() != null && !"".equals(this.saisie.getText())) {
			String nomDuFichier = "ENREGISTREMENTS/" + new SimpleDateFormat("yyyy-MM-dd_HH'h'mm").format(new Date()) + "_" + this.prenom.replaceAll("[^a-zA-Z0-9]", "_") + "_" + this.selectionnerTexte.getSelectionModel().getSelectedItem().getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
			System.out.println("Enregistrement de " + nomDuFichier);
			try {
				FileWriter fw = new FileWriter(nomDuFichier);
				fw.write("\nTexte d'origine : \n\n");
				fw.write(selectionnerTexte.getSelectionModel().getSelectedItem().getCorps());
				fw.write("\n\nTexte saisi : \n\n");
				fw.write(this.saisie.getText());
				fw.write("\n\n");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.saisie.clear();
		}
	}

	public static void main(String[] args) {
        launch();
    }
}
