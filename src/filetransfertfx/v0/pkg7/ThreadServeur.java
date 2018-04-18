/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filetransfertfx.v0.pkg7;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Boss
 */
class ThreadServeur extends Thread{
    private int port;
    private String repertoire;
    private ServerSocket serveur = null;
    public static boolean cancel = false;
    
    public ThreadServeur(int i, String r) throws IOException {
    
        this.port = i;
        this.repertoire = r;
        serveur = new ServerSocket(port);
        System.out.println("Le serveur a demarré dans le thread "+Thread.currentThread());
    }
    
    public void run(){
        while(!cancel)
            try{
               Socket client = serveur.accept();
               System.out.println("Un client connecté..");
               new ThreadSessionReception(client, repertoire);
            } catch(IOException e){   
                ThreadSessionReception.error("Unable to launch receive session", "Maybe FiletranXfert could'nt accept the connection attemp from the remote host.");
            }
        System.out.println("Fin Thread Serveur...");
        try {
            serveur.close();
        } catch (IOException ex) { 
            ThreadSessionReception.error("Unable to close the server", "Maybe the server is already closed, or did not ever started.");
        }
        System.exit(0);
    }
    
    public void setRepertoire(String r) throws IOException{
        this.repertoire = r;
    }
    
}
