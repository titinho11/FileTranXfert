/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filetransfertfx.v0.pkg7;

import java.awt.AWTException;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.MenuItem;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javax.imageio.ImageIO;

/**
 *
 * @author Boss
 */
public class FileTransfertFXV07 extends Application {
    private static String[] argument;
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        
        Pane root = new StackPane();
        root = FXMLLoader.load(getClass().getResource("MainFrame.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        Platform.setImplicitExit(false);
        if (SystemTray.isSupported()){
            SystemTray tray = SystemTray.getSystemTray();
            java.awt.Image image = ImageIO.read(new File("i6p.jpg"));
            
            PopupMenu menu = new PopupMenu();
            MenuItem show = new MenuItem("Open");
            MenuItem exit = new MenuItem("Exit");
            menu.add(show); menu.addSeparator(); menu.add(exit);
            TrayIcon icon = new TrayIcon(image, "FileTranXfert", menu);
            
            icon.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    Platform.runLater(new Runnable(){
                        public void run(){
                            primaryStage.show();
                        }
                    });
                } 
            });
            show.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    Platform.runLater(new Runnable(){
                        public void run(){
                            primaryStage.show();
                        }
                    });
                } 
            });
            
            exit.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    
                    ThreadServeur.cancel = true;
                    System.exit(0);
                } 
            });
            
            try {
                tray.add(icon);
            } catch (AWTException e) { 
                e.printStackTrace();
                ThreadSessionReception.error("Unable to add the Systray icon", "ensure you to have i6p.jpg, i6.png and i6.ico in your main FileTranXfert folder.");
            }
        
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>(){
                public void handle(WindowEvent e){
                    if (SystemTray.isSupported()){
                         primaryStage.hide();
                         System.out.println(SystemTray.isSupported()+" on vient de hider");
                    }
                    else {
                        System.out.println(SystemTray.isSupported()+" on la pas fait");
                        System.exit(0);
                        
                    }
                }
            });
        }
        primaryStage.getIcons().add(new Image("file:i6.png"));
        //primaryStage.setResizable(false);
        primaryStage.setTitle("FileTranXfert v1.0");
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        argument = args;
        launch(args);
    }
    
}
