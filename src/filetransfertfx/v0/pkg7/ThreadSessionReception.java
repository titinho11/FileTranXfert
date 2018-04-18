/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filetransfertfx.v0.pkg7;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.util.Date;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author Boss
 */
class ThreadSessionReception extends Thread{
    private Socket client = null;
    OutputStream out = null;
    private static char separateur = File.separatorChar;
    BufferedInputStream in = null;
    static int TAILLE = 1024;
    static final int NBBOUCHEE = 20;
    String repertoire;
    public static Button Cancel = new Button("Cancel");
    public static Button Pause = new Button("Pause");
    StringTokenizer tok2 = null;
    public static String OS = System.getProperty("os.name");
    String nomFichier;
    Long fileSize ;
    private String reponse = new String();
    
    public ThreadSessionReception(Socket s, String e) throws IOException{
        this.client = s;
        this.repertoire = e;
        in = new BufferedInputStream (client.getInputStream());
        out = client.getOutputStream();
        start();
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
    
    public static String BonneTaille(long t){
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.##");
        String bonneTaille = t+" Octects";
        if (t/1000 >= 1) bonneTaille = df.format(t/1000)+" Ko";
        if (t/1000000 >= 1) bonneTaille = df.format(t/1000000)+" Mo";    
        if (t/1000000000 >= 1) bonneTaille = df.format(t/1000000000)+" Go";
        return bonneTaille;
    }
    
    public static void decompresser(String nomArchive) throws IOException{
        
        ZipInputStream arch = new ZipInputStream(new FileInputStream(new File(nomArchive)));
        ZipEntry item;
        FileOutputStream fos;
        int s;
        final int BUFFER = 2048;
        byte[] data = new byte[BUFFER];
        
        OS = OS.toLowerCase();
        if ( OS.contains("windows") ) separateur = '\\';//System.out.println("ya win...");
        else if ( OS.contains("nux") || OS.contains("ubuntu") || OS.contains("backbox") || OS.contains("mac")) separateur = '/';//System.out.println("ya linux...");
        else separateur = '/';//System.out.println("ya mac os...");
        
        while ((item = arch.getNextEntry()) != null ) {

             //b.append("element "+i+" : "+item.getName()+" "+item.isDirectory());
             
             System.out.println("on cree "+longDir(item.getName())+" "+(new File(vraiNom(longDir(item.getName())))).mkdirs());
             String name = vraiNom(item.getName());System.out.println(name);
             fos = new FileOutputStream(name);
             
             while( (s=arch.read(data, 0, BUFFER)) > 0 ) 
                 fos.write(data, 0, s); 
             
             fos.flush();
             fos.close();
             arch.closeEntry();
        }
        arch.close();
        //(new File(nomArchive)).delete();
        
    }
    
    private static String vraiNom(String n){
    	
    	String a = "";
    	for (int i=0;i<n.length();++i) {
    		
    		if (n.charAt(i) == separateur) a += File.separatorChar;
    		else a+= n.charAt(i);
    		
    	}
    	return a;
    }
    
    
    private static String longDir(String dir){
        
        String l="";
        int max=0;
        if (dir.indexOf(separateur) == -1) return l;
        else for (int i=0;i<dir.length();++i) if (dir.charAt(i) == separateur) max = i;
        return dir.substring(0, max+1);
    }
    
    private boolean confirmer(StringTokenizer t) throws InterruptedException{
        Flag flag = new Flag();//objet a passer en parametre du thread confirmeur, qui impose la synchro
        Confirmeur c = new Confirmeur(t, reponse, client, flag);
        Platform.runLater(c);
        synchronized(c){
             try{
                 System.out.println("J'attends que l'on confirme..");
                 c.wait();//accepter derange ici au debut, les premier essaie ne traversent pas..
             } catch (InterruptedException e){ ThreadSessionReception.error("The Host does'nt respond", ""); }         
         }
        return flag.getValue();
    }
    
    public void run(){
        try {
            byte b[] = new byte[64000];
            // exception
            in.read(b);
            // selection du dossier d'enregistrement
            String copy = (new String(b, 0, b.length)).trim();
            StringTokenizer token = new StringTokenizer(copy, "|");
            tok2 = new StringTokenizer(copy, "|");
            nomFichier = token.nextToken();
            long tailleFichier = Long.parseLong(token.nextToken());
            TAILLE = Math.round(tailleFichier/NBBOUCHEE)+1;
            String isWhat = token.nextToken();
            token = new StringTokenizer(copy, "|");
            System.out.println("j'ai recu : "+copy);
            
            if (!confirmer(token)) out.write(0);
            else {
                out.write(1);
                Platform.runLater(new Runnable(){
                    public void run(){
                        VBox root = new VBox();
                        root.setAlignment(Pos.CENTER);
                        root.setSpacing(7);
                        Scene scene = new Scene(root);
                        
                        Cancel.setOnAction(new EventHandler<ActionEvent>() {
                            
                            public void handle(ActionEvent event) {
                                Tache.cancel = true;
                                Cancel.setDisable(true);
                                Pause.setDisable(true);
                            }
                            
                        });
                        
                        Pause.setOnAction(new EventHandler<ActionEvent>() {
                            
                            public void handle(ActionEvent event) {
                                if (Pause.getText().equals("Pause")){
                                    TacheEnvoie.pause = true;
                                    Pause.setText("Resume");
                                    }
                                    else {
                                        TacheEnvoie.pause = false;
                                        Pause.setText("Pause");
                                    }
                            }
                            
                        });
                        ProgressBar barre = new ProgressBar(0);
                        String bonneTaille = BonneTaille(tailleFichier);
                        Text titre = new Text("Taille du fichier : "+bonneTaille);
                        Text info = new Text("456 Ko / 1256 Ko");
                        final HBox hb = new HBox();
                        hb.setSpacing(5);
                        hb.setAlignment(Pos.CENTER);
                        final HBox hb2 = new HBox();
                        hb2.setSpacing(15);
                        root.setSpacing(3);
                        hb2.setAlignment(Pos.BOTTOM_CENTER);
                        barre.setPrefSize(295, 20);
                        hb.getChildren().addAll(barre);
                        hb2.getChildren().addAll(Pause, Cancel);
                        root.getChildren().addAll( titre, info, hb, hb2);
                        scene.setRoot(root);
                        Stage primaryStage = new Stage();
                        primaryStage.setHeight(130);
                        primaryStage.setResizable(false);
                        primaryStage.setWidth(305);
                        primaryStage.setTitle("Reception : "+nomFichier);
                        primaryStage.setScene(scene);
                        barre.setProgress(0);
                        primaryStage.show();
                        Tache transfert = new Tache(in, out, nomFichier, tailleFichier, TAILLE, repertoire, tok2);
                        barre.progressProperty().unbind();
                        barre.progressProperty().bind(transfert.progressProperty());
                        info.textProperty().unbind();
                        info.textProperty().bind(transfert.messageProperty());
                        transfert.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>(){
                            public void handle(WorkerStateEvent event) {
                                info.textProperty().unbind();
                                //fermer cette fenetre, afficher une avec copie terminer!
                                primaryStage.close();
                                System.out.println("Finished Receiving !!");
                                System.out.println("Avant bon alert");
                                Alert boite = new Alert(Alert.AlertType.CONFIRMATION);
                                boite.setTitle("FileTranXfert");
                                Stage stage = (Stage) boite.getDialogPane().getScene().getWindow();
                                stage.getIcons().add(new Image("file:i6.png"));
                                boite.setHeaderText("Reception of "+nomFichier+" completed !");
                                boite.setContentText("Do you want to open the "+(isWhat.equals("dir")? "directory ?":"file ?"));
                                Optional<ButtonType> answer = boite.showAndWait();
                                if (answer.get() == ButtonType.OK) {
                                    try {
                                        Desktop.getDesktop().open(new File(((repertoire.equals("-"))? nomFichier:repertoire+File.separator+nomFichier)));
                                    } catch (IOException ex) {
                                        info.textProperty().unbind();
                                        Alert b = new Alert(Alert.AlertType.ERROR);
                                        b.setTitle("Error !");
                                        Stage s = (Stage)b.getDialogPane().getScene().getWindow();
                                        s.getIcons().add(new Image("file:i6p.png"));
                                        b.setContentText("Unable to open this "+(isWhat.equals("dir")? "directory ":" file ")+repertoire+File.separator+nomFichier);
                                        b.showAndWait();
                                    }
                                }
                                else {
                                System.out.println("on laisse couler..");
                                }
                            
                            }
                        });
                        transfert.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, new EventHandler<WorkerStateEvent>(){
                            public void handle(WorkerStateEvent event) {
                                info.textProperty().unbind();
                                Alert b = new Alert(Alert.AlertType.WARNING);
                                b.setTitle("Warning !");
                                b.setContentText("Download Stopped !");
                                b.showAndWait();
                                primaryStage.close();
                                System.out.println("Cancelled !!");
                            }
                        });
                        transfert.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>(){
                            public void handle(WorkerStateEvent event) {
                                info.textProperty().unbind();
                                Alert b = new Alert(Alert.AlertType.ERROR);
                                b.setTitle("Error !");
                                b.setContentText("An error occured while downloading !!");
                                b.showAndWait();
                                primaryStage.close();
                                System.out.println("error!!");
                            }
                        });
                        (new Thread(transfert)).start();
                    }
                });                
            }
        } catch (IOException e){ ThreadSessionReception.error("Unable to init reception session", ""); } catch (InterruptedException ex) { ThreadSessionReception.error("Unable to init reception session", ""); }
    }
}

class Confirmeur implements Runnable{
     
    private StringTokenizer token = null;
    private String reponse;
    private Socket client = null;
    private Flag fl = null;
    
    public Confirmeur(StringTokenizer t, String rep, Socket c, Flag f){
        this.token = t;
        this.reponse = rep;
        this.client = c;
        this.fl = f;
    }
    
    public void run(){
         synchronized(this){
             String nom = token.nextToken(); String size = token.nextToken(); token.nextToken(); String isFile = token.nextToken();
             
             Alert dialog = new Alert(AlertType.CONFIRMATION);
             if (isFile.equals("dir")) nom = nom.substring(0, nom.length()-4);
             dialog.setContentText("Voulez vous recevoir ce "+(isFile.equals("dir")? "dossier : ":"fichier : ")+ nom+"\nde "+client.getInetAddress()+
                   " , taille du fichier :"+ThreadSessionReception.BonneTaille(Long.parseLong(size))+" ?");
             dialog.setHeaderText(null);
             dialog.setTitle("Reception de "+(isFile.equals("dir")? "dossier : ":"fichier : "));
             Optional<ButtonType> rep = dialog.showAndWait();

             if (rep.get()== ButtonType.OK){
                fl.setValue(true);
             }
             else {
                fl.setValue(false);
             }
             notify();
        }
    } 
}

class Flag {
    private boolean value;
    public void setValue(boolean a){
        value = a;
    }
    public boolean getValue(){
        return value;
    }
}

class Tache extends Task{
    
    OutputStream out = null;
    BufferedInputStream in = null;
    static int TAILLE = 1024;
    static final int NBBOUCHEE = 20;
    String nomFichier;
    StringTokenizer tok = null;
    Long fileSize ;
    static boolean cancel = false;
    static boolean pause = false;
    private ProgressBar barre = null;
    String repertoire="-";
    
    public Tache(BufferedInputStream i, OutputStream o, String nf, Long si, int t, String r, StringTokenizer tok2){
        this.in = i;
        this.out = o;
        this.tok = tok2;
        this.nomFichier = nf;
        this.fileSize = si;
        this.repertoire = r;
        this.TAILLE = t;
    }

    @Override
    protected Object call() throws Exception {
                        try {
                            FileOutputStream ecrireFichier;
                            ecrireFichier = new FileOutputStream((repertoire.equals("-"))? nomFichier:repertoire+File.separator+nomFichier);
                            
                            byte donnee[] = new byte[TAILLE];
                            int buffer;
                            long dejaLu = 0;
                            java.text.DecimalFormat d = new java.text.DecimalFormat("0.#");
                            
                            while(((buffer = in.read(donnee)) != -1) && !cancel){
                                while(pause) {
                                    if (cancel) break;
                                    updateMessage("Paused");
                                }
                                if (cancel) break;
                                updateMessage("Resuming...");
                                dejaLu += buffer;
                                ecrireFichier.write(donnee, 0, buffer);
                                updateProgress(dejaLu, fileSize);
                                updateMessage(""+d.format(100*dejaLu/fileSize)+" %");
                                
                            }
                            ecrireFichier.close();
                            out.flush();
                            if (cancel) {
                                //updateProgress(0, fileSize);
                                updateMessage("TranXfert cancelled !!");
                                sleep(2000);
                            }
                            else {
                                Date date = new Date();
                                System.out.println("heure de reception : "+date);
                                tok.nextToken(); tok.nextToken();
                                System.out.println("Recu : "+nomFichier+" de taille : "+dejaLu+" dans le thread "+Thread.currentThread());
                                ThreadSessionReception.OS = (tok.nextToken()).trim();
                                
                                String isWhat = tok.nextToken();
                                if (isWhat.equals("dir")){
                                    updateProgress(-1, 1);
                                    updateMessage("Decompressing...");
                                    ThreadSessionReception.Pause.setDisable(true);
                                    ThreadSessionReception.Cancel.setDisable(true);
                                    ThreadSessionReception.decompresser((repertoire.equals("-"))? nomFichier:repertoire+File.separator+nomFichier);
                                    (new File((repertoire.equals("-"))? nomFichier:repertoire+File.separator+nomFichier)).delete();
                                }
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ThreadSessionReception.class.getName()).log(Level.SEVERE, null, ex);
                            Alert boite = new Alert(Alert.AlertType.ERROR);
                            boite.setTitle("FileTranXfert");
                            Stage stage = (Stage) boite.getDialogPane().getScene().getWindow();
                            stage.getIcons().add(new Image("file:i6.png"));
                            boite.setHeaderText("Probleme de reception");
                            boite.setContentText("FileTranXfert a rencontré un probleme lors de la reception, l'application va se fermer..");
                            boite.showAndWait();
                            System.exit(1);
                        }
                        try {
                            in.close();
                            out.close();
                        } catch(IOException e){ 
                            e.printStackTrace(); 
                            ThreadSessionReception.error("Unable to close host's intput stream and/or receiving file output stream", "");
                        }
                        return null;
                    }

    
}
