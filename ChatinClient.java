import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

public class ChatinClient {

    private static String host = "localhost";
    private static int port = 1234;

    public static void main(String[] args){

        try {
            // crea el socket
            Socket s = new Socket(host, port);

            // crea una variable comuna perque si el sender tanca el socket, el receiver s'enteri i el pugui tancar
            AtomicBoolean finished = new AtomicBoolean(false);

            // crea el thread i fa que escolti al servidor
            Thread receiver = new Thread(new Receiver(s, finished));
            receiver.start();
            // crea el thread per comunicar-se ell amb el servidor
            Thread sender = new Thread(new Sender(s, finished));
            sender.start();

        } catch (Exception e) {
            System.out.println("Servidor no disponible!");
        }
    }

    // aquest es el thread que llegeix per la consola i ho envia pel socket
    public static class Sender implements Runnable{
        private final Socket socket;
        private final AtomicBoolean finished;

        public Sender(Socket s, AtomicBoolean finished){
            this.socket = s;
            this.finished = finished;
        }

        // per llegir de consola
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        @Override
        public void run() {
            try {

                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                String lecturaTerminal = "";
                // deixa d'enviar lo llegit per consola si s'escriu per terminal FI o si s'ha tancat el socket
                while(!lecturaTerminal.equals("FI") && !finished.get()){
                    // entra si s'escriu a la consola
                    if(System.in.available() > 0){
                        lecturaTerminal = br.readLine();
                        dos.writeUTF(lecturaTerminal);
                        dos.flush();
                    } else {
                        // per no consumir la CPU si no està rebent res per terminal
                        Thread.sleep(250);
                    }
                }
                dos.close();
                socket.close();
            }
            // si salta alguna excepció canviarà el valor de la variable perque ho sapiguen 
            catch (IOException e) {
                finished.set(true);
            }
            catch (InterruptedException e){
                finished.set(true);
            }
        }

    }

    // aquest es el thread que llegeix a traves del socket i ho imprimeix per consola
    public static class Receiver implements Runnable{
        private final Socket socket;
        private final AtomicBoolean finished;

        public Receiver(Socket s, AtomicBoolean finished){
            this.socket = s;
            this.finished = finished;
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                System.out.println("Server: <<" + dis.readUTF() + ">>");

                String lecturaServer = "";
                while(!lecturaServer.equals("FI")){
                    lecturaServer = dis.readUTF();
                    // si es llegeix espais en blanc no s'imprimirà per pantalla
                    if(!lecturaServer.trim().isEmpty()){
                        System.out.println("Server: <<" + lecturaServer + ">>");
                    }
                }
                finished.set(true);

                dis.close();
                socket.close();

            } catch (IOException e) {
                finished.set(true);
                System.out.println("Connexió tancada.");
            }
        }
    }
} 