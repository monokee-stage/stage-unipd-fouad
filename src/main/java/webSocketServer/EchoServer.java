package webSocketServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
/**
 * @ServerEndpoint da un nome all'end point
 * Questo può essere acceduto via ws://localhost:8080/myfirstws/echo
 * "localhost" è l'indirizzo dell'host dove è deployato il server ws,
 * "myfirstws" è il nome del package
 * ed "echo" è l'indirizzo specifico di questo endpoint
 */
@ServerEndpoint("/echo")
public class EchoServer {
    /**
     * @OnOpen questo metodo ci permette di intercettare la creazione di una nuova sessione.
     * La classe session permette di inviare messaggi ai client connessi.
     * Nel metodo onOpen, faremo sapere all'utente che le operazioni di handskake
     * sono state completate con successo ed è quindi possibile iniziare le comunicazioni.
     *
     */
    Vector<String> v;

    public void removeAll(){
        v.removeAllElements();
    }
    public void addElements(String s){
        v.addElement(s);
    }
    public Vector<String> get(){
        return v;
    }
    public void print(){
         System.out.println("VECTOR message ricevuti ------------->");
        for (int i=0;i<v.size();i++){
            System.out.println(v.get(i));
        }
    }

    @OnOpen
    public void onOpen(Session session){
        v=new Vector<>();
        System.out.println(session.getId() + " ha aperto una connessione");
        try {
            session.getBasicRemote().sendText("Connessione Stabilita!");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Quando un client invia un messaggio al server questo metodo intercetterà tale messaggio
     * e compierà le azioni di conseguenza. In questo caso l'azione è rimandare una eco del messaggi indietro.
     */
    @OnMessage
    public void onMessage(String message, Session session){

        System.out.println("Ricevuto messaggio da: " + session.getId() + ": " + message);
        addElements(message);

        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Metodo che intercetta la chiusura di una connessine da parte di un client
     *
     * Nota: non si possono inviare messaggi al client da questo metodo
     */
    @OnClose
    public void onClose(Session session)  {
        System.out.println("Session " +session.getId()+" terminata");
        print();
        Socket connessione= null;
            try {
                connessione = new Socket("localhost",8000);
                OutputStream output=connessione.getOutputStream();
                PrintWriter invioDati=new PrintWriter(output);
                for (int i=0;i<v.size();i++){
                    invioDati.println(v.get(i));
                }
                invioDati.close();
                connessione.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

}