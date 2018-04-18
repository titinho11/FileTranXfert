/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quicksend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.util.Date;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author Boss
 */
public class QuickSend extends Application {
    private static String[] argument;
    private static int port = 1111; 
    private static int TAILLE = 1024;
    static final int NBBOUCHEE = 20;
    private String file, cible;
    private String dossier;
    private BufferedInputStream in = null;
    private ZipOutputStream zos;
    private OutputStream out = null;
    private Socket serveur = null;
    private boolean cFichier = true;
    
    @Override
    public void start(Stage primaryStage) {
        if (argument.length < 1) {
            Alert boite = new Alert(Alert.AlertType.ERROR);
            boite.setTitle("QuickSend");
            Stage stage = (Stage) boite.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image("file:i6.png"));
            boite.setHeaderText("Missing argument !");
            boite.setContentText("Veuillez executer ce programme en ligne de commandes ou depuis"
                    + "le menu contextuel, afin de preciser l'argument.");
            boite.showAndWait();
        }
        else {
            TextInputDialog boite2 = new TextInputDialog("127.0.0.1");
            boite2.setTitle("FileTranXfert");
            Stage stage = (Stage) boite2.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image("file:i6.png"));
            boite2.setHeaderText("Veuillez entrer l'addresse du destinataire");
            boite2.setContentText("IP : ");
            Optional<String> result = boite2.showAndWait();
            if (result.isPresent()){
                cible = result.get().trim();
                file = argument[0];
                //new ThreadSessionEnvoie(cible, "le teste.txt", PORT);
                try {
                    serveur = new Socket(cible, port);
                    in = new BufferedInputStream(serveur.getInputStream());
                    out = serveur.getOutputStream();
                    File fichier = new File(file);
                    if (fichier.isDirectory()){
                        dossier = fichier.getName()+".zip";
                        FileOutputStream fos = new FileOutputStream(fichier.getName()+".zip");
                        zos = new ZipOutputStream(new BufferedOutputStream(fos));
                        zos.setLevel(9);
                        zos.setMethod(ZipOutputStream.DEFLATED);

                        compresseDossier( fichier.getAbsolutePath(), "");
                        zos.close();
                        //ecrire(System.getProperty("os.name"), sortie);
                        fichier = new File(fichier.getName()+".zip");
                        file = fichier.getAbsolutePath();
                        cFichier = false;
                    }
                    long sizef = 0;
                    String infoFichier = null;
                    sizef = fichier.length();
                    TAILLE = Math.round(sizef/NBBOUCHEE);
                    infoFichier = fichier.getName() + "|" + sizef+"|"+System.getProperty("os.name")+"|"+((cFichier)? "file":"dir");;
                    String nomFichier = fichier.getName();
                    System.out.println("Fichier à envoyer " + infoFichier);
                    byte nameFile[] = infoFichier.getBytes();// recuperation du nom du fichier
                    out.write(nameFile, 0, nameFile.length);//emvoi le nom de fichier
                    int retour = in.read();
                    System.out.println("en retour de mon envoie : "+retour);
                    if (retour == 1){
                        System.out.println("dans le if de retour = 1...");
                        VBox root = new VBox();
                        root.setAlignment(Pos.CENTER);
                        root.setSpacing(7);
                        Scene scene = new Scene(root);
                        Button cancel = new Button();
                        Text info = new Text("456 Ko / 1256 Ko");
                        Button pause = new Button("Pause");
                        cancel.setText("Cancel");
                        cancel.setOnAction(new EventHandler<ActionEvent>() {

                            public void handle(ActionEvent event) {
                                TacheEnvoie.cancel = true;
                                cancel.setDisable(true);
                                pause.setDisable(true);
                            }

                        });

                        pause.setOnAction(new EventHandler<ActionEvent>() {

                            public void handle(ActionEvent event) {
                                if (pause.getText().equals("Pause")){
                                    TacheEnvoie.pause = true;
                                    pause.setText("Resume");
                                    info.setText("Download Stopped");
                                }
                                else {
                                    TacheEnvoie.pause = false;
                                    pause.setText("Pause");
                                    info.setText("Resuming...");
                                }
                            }

                        });
                        ProgressBar barre = new ProgressBar(0);
                        String bonneTaille = BonneTaille((new File(file)).length());
                        Text titre = new Text("Taille du fichier : "+bonneTaille);
                        final HBox hb = new HBox();
                        hb.setSpacing(5);
                        hb.setAlignment(Pos.CENTER);
                        final HBox hb2 = new HBox();
                        hb2.setSpacing(15);
                        root.setSpacing(3);
                        hb2.setAlignment(Pos.BOTTOM_CENTER);
                        barre.setPrefSize(295, 20);
                        hb.getChildren().addAll(barre);
                        hb2.getChildren().addAll(pause, cancel);
                        root.getChildren().addAll( titre, info, hb, hb2);
                        scene.setRoot(root);
                        Stage tage = new Stage();
                        tage.setHeight(130);
                        tage.setResizable(false);
                        tage.setWidth(305);
                        tage.setTitle("Envoie de : "+nomFichier);
                        tage.setScene(scene);
                        barre.setProgress(0);
                        tage.show();
                        TacheEnvoie transfert = new TacheEnvoie((new File(file)).toURL().openStream(), out, (new File(file)).getName(), (new File(file)).length(), TAILLE, cFichier, (new File(file)).getAbsolutePath());
                        barre.progressProperty().unbind();
                        barre.progressProperty().bind(transfert.progressProperty());
                        info.textProperty().unbind();
                        info.textProperty().bind(transfert.messageProperty());
                        transfert.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>(){
                            public void handle(WorkerStateEvent event) {
                                info.textProperty().unbind();
                                //fermer cette fenetre, afficher une avec copie terminer!
                                tage.close();
                                System.out.println("Finished sending !!");
                            }
                        });
                        transfert.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, new EventHandler<WorkerStateEvent>(){
                            public void handle(WorkerStateEvent event) {
                                info.textProperty().unbind();
                                Alert b = new Alert(Alert.AlertType.WARNING);
                                b.setTitle("Warning !");
                                b.setContentText("Sending Stopped !");
                                Stage stage2 = (Stage) b.getDialogPane().getScene().getWindow();
                                stage2.getIcons().add(new Image("file:i6.png"));
                                b.showAndWait();
                                tage.close();
                                System.out.println("Cancelled sending !!");
                            }
                        });

                        transfert.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>(){
                            public void handle(WorkerStateEvent event) {
                                info.textProperty().unbind();
                                Alert b = new Alert(Alert.AlertType.ERROR);
                                b.setTitle("Error !");
                                Stage stage2 = (Stage) b.getDialogPane().getScene().getWindow();
                                stage2.getIcons().add(new Image("file:i6.png"));
                                b.setContentText("An error occured while sending !!");
                                b.showAndWait();
                                tage.close();
                                System.out.println("error send!!");
                            }
                        });
                        System.out.println("avant thread envoie");
                        (new Thread(transfert)).start();
                        
                    }
                    
                } catch (IOException ex) { 
                    ex.printStackTrace();
                    Alert boite = new Alert(Alert.AlertType.ERROR);
                    boite.setTitle("FileTranXfert");
                    Stage stage2 = (Stage) boite.getDialogPane().getScene().getWindow();
                    stage2.getIcons().add(new Image("file:i6.png"));
                    boite.setHeaderText("Destinataire non existant !");
                    boite.setContentText("Veuillez vous assurer d'avoir entrer une addresse valide, et qu'elle corresponde bien a celle de votre destinataire.");
                    boite.showAndWait();
                }  
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        argument = args;
        launch(args);
    }
    
    public static String BonneTaille(long t){
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.##");
        String bonneTaille = t+" Octects";
        if (t/1000 >= 1) bonneTaille = df.format(t/1000)+" Ko";
        if (t/1000000 >= 1) bonneTaille = df.format(t/1000000)+" Mo";    
        if (t/1000000000 >= 1) bonneTaille = df.format(t/1000000000)+" Go";
        return bonneTaille;
    }
    
    static void error(String head, String content){
        Alert boite = new Alert(Alert.AlertType.ERROR);
        boite.setTitle("FileTranXfert");
        Stage stage = (Stage) boite.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image("file:i6.png"));
        boite.setHeaderText(head);
        boite.setContentText(content);
        boite.showAndWait();
    }
    
    private void addFileToArchive(FileInputStream fis, String nomFic) throws IOException{
        
        final int BUFFER = 2048;
        int s;
        byte[] data = new byte[BUFFER];
        BufferedInputStream bFis = new BufferedInputStream(fis, BUFFER);
        
        ZipEntry entree = new ZipEntry(nomFic);
        zos.putNextEntry(entree);
        
        while( (s = bFis.read(data, 0, BUFFER)) > 0 ) {
            zos.write(data, 0, s);
        }
        
        zos.closeEntry();
        bFis.close();
        
    }
    
    private void compresseDossier(String fullPath, String pre){
        
        try{
             File fic = new File(fullPath);
             String[] element = fic.list();
             String temp;
             for (int k=0; k<element.length; ++k){
                 
                 temp = (pre.equals(""))? fic.getName()+File.separator+element[k]:pre+File.separator+fic.getName()+File.separator+element[k];
                 
                 if ((new File(fullPath+File.separator+element[k])).isFile())
                     addFileToArchive(new FileInputStream(new File(fullPath+File.separator+element[k])), temp);
                 
                 else
                     compresseDossier(fullPath+File.separator+element[k], (pre.equals(""))? fic.getName():pre+File.separator+fic.getName());
            
             }
             
        } catch(Exception e){ error("An error occured while compressing the file", ""); }
        
    }
    
}

class TacheEnvoie extends Task{
    
    OutputStream out = null;
    InputStream lireFichier = null;
    int TAILLE = 1024;
    static final int NBBOUCHEE = 20;
    String nomFichier;
    Long fileSize ;
    private boolean cFichier = true;
    private ProgressBar barre = null;
    public static boolean cancel = false; 
    public static boolean pause = false;
    private String chemin;
    
    public TacheEnvoie(InputStream i, OutputStream o, String nf, Long si, int t, boolean f, String l){
        this.lireFichier = i;
        this.out = o;
        this.cFichier = f;
        this.chemin = l;
        this.nomFichier = nf;
        this.fileSize = si;
        this.TAILLE = t;
    }

    @Override
    protected Object call() throws Exception {
            try {

                java.text.DecimalFormat d = new java.text.DecimalFormat("0.#");
                byte donnee[] = new byte[TAILLE];
                int buffer;
                long dejaLu = 0;

                Date date = new Date();
                System.out.println("heure de debut : "+date);

                while(((buffer = lireFichier.read(donnee)) != -1) && !cancel){
                    while(pause) 
                        if (cancel) break;
                    if (cancel) break;
                    dejaLu += buffer;
                    out.write(donnee, 0, buffer);
                    updateProgress(dejaLu, fileSize);
                    updateMessage(""+d.format(100*dejaLu/fileSize)+" %");

                }
                out.flush();
                lireFichier.close();
                if (cancel) {
                    //updateProgress(0, fileSize);
                    updateMessage("TranXfert cancelled !!");
                    sleep(2000);
                }
                else{
                    if (!cFichier) (new File(chemin)).delete();
                    String infoArchive = System.getProperty("os.name")+"|"+((cFichier)? "file":"dir");
                    out.write(infoArchive.getBytes());
                    System.out.println("Envoyé : "+nomFichier+" de taille : "+dejaLu+" infoArch "+infoArchive+" dans le thread "+Thread.currentThread());
                }
                
            } catch (IOException e) { QuickSend.error("An error occured while sending the file", ""); }
            try{
                out.close();
            } catch (IOException e) { QuickSend.error("Unable to close the host output stream", ""); }
            return null;
        }

}


//tuer le thread apres un certain tempps sans reponse
//probleme de cancel et le thread infini