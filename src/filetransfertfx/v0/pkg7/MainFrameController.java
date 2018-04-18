/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filetransfertfx.v0.pkg7;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javax.swing.JOptionPane;

/**
 * FXML Controller class
 *
 * @author Boss
 */
public class MainFrameController implements Initializable {
    @FXML
    private Label file;
    private Button fileChooser;
    @FXML
    private Button scan;
    @FXML
    private Button send;
    @FXML
    private TextArea scanField;
    @FXML
    private TextField address;
    @FXML
    private Button setting;
    @FXML
    private Button port;
    private int PORT = 1111;
    private String repertoire = "";
    private ThreadServeur serveur = null;
    @FXML
    private Button FILE;
    @FXML
    private Button dir;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        FileReader reader = null;
        scan.setDisable(true);
        try {
            // TODO
            port.setDisable(true);
            setting.setText("Dir");
            address.setText("127.0.0.1");
            file.setText("Fichier selectionné...");
            reader = new FileReader(new File("config.ftx"));
            BufferedReader b = new BufferedReader(reader);
            String e = b.readLine();
            StringTokenizer tok = new StringTokenizer(e, "*");
            repertoire = tok.nextToken();
            PORT = Integer.parseInt(tok.nextToken());
            b.close();
            serveur = new ThreadServeur(PORT, repertoire);
            serveur.start();
            
        } catch (FileNotFoundException ex) {
            ThreadSessionReception.error("config.ftx not found !", "Please, ensure you to have the file config.ftx inside the main directory of the program, just neir to FileTranXfert.exe");
        } catch (IOException ex) {
            ThreadSessionReception.error("Unable to start the server", "FileTranXfert.exe failed to start the server, the file config.ftx might be modified or moved, or maybe a FileTranXfert's server is already running.Ensure you to close the sofware if already running, and if persist, try to reinstall the sofware.");
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                ThreadSessionReception.error("Unable to close the input item from config.ftx", "");
            }
        }
    }    

    @FXML
    private void onScan(ActionEvent event) {
    }

    @FXML
    private void onSend(ActionEvent event) {
        if (file.equals("Fichier selectionné...") || address.getText().equals("")){
            JOptionPane.showMessageDialog(null, ("Rassurez vous d'avoir entrer l'adresse ip du destinataire et le fichier a envoyer !"));
        }
        else{
            String cible = address.getText().trim();
            new ThreadSessionEnvoie(cible, file.getText(), PORT);
        }
    }

    private void onBrowse(ActionEvent event) {
        
    }

    @FXML
    private void onSetting(ActionEvent event) throws IOException {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Receiving Directory");
        File dir = chooser.showDialog(setting.getScene().getWindow());
        if (dir != null){
            
            repertoire = dir.getAbsolutePath();
            FileWriter ecrire = new FileWriter(new File("config.ftx"));
            ecrire.write(repertoire+"*"+PORT);
            ecrire.flush();
            ecrire.close();
            serveur.setRepertoire(repertoire);
        }
              
    }

    @FXML
    private void onPort(ActionEvent event) throws IOException {
        TextInputDialog setting = new TextInputDialog(Integer.toString(PORT));
        setting.setContentText("Port : ");
        setting.setTitle("Port of the server");
        setting.setHeaderText("Current : "+PORT);//serveur.stop();
        Optional<String> result = setting.showAndWait();
        if (result.isPresent()){
            PORT = Integer.parseInt(result.get());
            FileWriter ecrire = new FileWriter(new File("config.ftx"));
            ecrire.write(repertoire+"*"+PORT);
            ecrire.flush();
            ecrire.close();
            Alert b = new Alert(Alert.AlertType.INFORMATION);
            b.setHeaderText("Modification effectuée");
            b.setContentText("Modification prise en compte et effective au prochain \ndemarrage de l'application, assurez vous cependant que vos correspondants aient enregistré le meme port .\n Nouveau port : "+PORT);
            b.showAndWait();
        }
    }

    @FXML
    private void onFILE(ActionEvent event) {
        FileChooser f = new FileChooser();
        final File fil = f.showOpenDialog(dir.getScene().getWindow());
        if (fil != null ) file.setText(fil.getAbsolutePath());
    }

    @FXML
    private void onDir(ActionEvent event) {
        DirectoryChooser f = new DirectoryChooser();
        final File fil = f.showDialog(dir.getScene().getWindow());
        if (fil != null ) file.setText(fil.getAbsolutePath());
    }
    
}