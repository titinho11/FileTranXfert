/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filetransfertfx.v0.pkg7;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import static java.lang.Thread.sleep;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author Boss
 */
class ThreadSessionEnvoie extends Thread{
    private String file, cible;
    private static int port = 1111; 
    private static int TAILLE = 1024;
    static final int NBBOUCHEE = 20;
    private String dossier;
    private BufferedInputStream in = null;
    private ZipOutputStream zos;
    private OutputStream out = null;
    private Socket serveur = null;
    private boolean cFichier = true;
    
    public ThreadSessionEnvoie(String ip, String fi, int p){
        this.file = fi;
        this.cible = ip;
        this.port = p;
        try {
            serveur = new Socket(cible, port);
            in = new BufferedInputStream(serveur.getInputStream());
            out = serveur.getOutputStream();
        } catch (IOException ex) { 
            ex.printStackTrace();
            ThreadSessionReception.error("Unable to connect to this host", "ensure you to enter the right value of the ip address, and to be connected to the same network that your host.");
        }
        start();
    }
    
    public String BonneTaille(long t){
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.##");
        String bonneTaille = t+" Octects";
        if (t/1000 >= 1) bonneTaille = df.format(t/1000)+" Ko";
        if (t/1000000 >= 1) bonneTaille = df.format(t/1000000)+" Mo";    
        if (t/1000000000 >= 1) bonneTaille = df.format(t/1000000000)+" Go";
        return bonneTaille;
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
             
        } catch(Exception e){ 
            ThreadSessionReception.error("Unable to compresse the folder"+fullPath, "");
        }
        
    }
    
    public void run(){
     
        try {
            File fichier = new File(file);
            if (fichier.isDirectory()){
                dossier = fichier.getName()+".zip";
                FileOutputStream fos = new FileOutputStream(fichier.getName()+".zip");
                zos = new ZipOutputStream(new BufferedOutputStream(fos));
                zos.setLevel(9);
                zos.setMethod(ZipOutputStream.DEFLATED);
                
                compresseDossier( fichier.getAbsolutePath(), "");
                zos.close();
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
                Platform.runLater(new Runnable(){
                    public void run(){
                        try {
                            VBox root = new VBox();
                            root.setAlignment(Pos.CENTER);
                            root.setSpacing(7);
                            Scene scene = new Scene(root);
                            Button cancel = new Button();
                            Button pause = new Button("Pause");
                            cancel.setText("Cancel");
                            Text info = new Text("456 Ko / 1256 Ko");
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
                            Stage primaryStage = new Stage();
                            primaryStage.setHeight(130);
                            primaryStage.setResizable(false);
                            primaryStage.setWidth(305);
                            primaryStage.setTitle("Envoie : "+nomFichier);
                            primaryStage.setScene(scene);
                            barre.setProgress(0);
                            primaryStage.show();
                            TacheEnvoie transfert = new TacheEnvoie((new File(file)).toURL().openStream(), out, (new File(file)).getName(), (new File(file)).length(), TAILLE, cFichier, (new File(file)).getAbsolutePath());
                            barre.progressProperty().unbind();
                            barre.progressProperty().bind(transfert.progressProperty());
                            info.textProperty().unbind();
                            info.textProperty().bind(transfert.messageProperty());
                            transfert.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>(){
                                public void handle(WorkerStateEvent event) {
                                    info.textProperty().unbind();
                                    //fermer cette fenetre, afficher une avec copie terminer!
                                    primaryStage.close();
                                    System.out.println("Finished sending !!");
                                }
                            });
                            (new Thread(transfert)).start();
                        } catch (MalformedURLException ex) { 
                            ThreadSessionReception.error("Unable to open the file'stream", "unable to open a strem to this file : "+(new File(file)).getAbsolutePath());
                        } catch (IOException ex) {
                            ThreadSessionReception.error("Unable to init the progress bar windows, or the sending operation.", "");
                        }
                    }
                });
            }
         } catch (IOException ex) { 
             ThreadSessionReception.error("Unable to operate the pre-sendig operations...", "");
         }
    
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
                
            } catch (IOException e) { ThreadSessionReception.error("Unable to operate the sending instructions...", ""); }
            try{
                out.close();
            } catch (IOException e) { ThreadSessionReception.error("Unable to close the host output item", "it is maybe already closed, or has never been openned"); }
            return null;
        }

}

