import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Esta clase representa un servidor de chat que acepta conexiones de múltiples clientes.
 */
public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    /**
     * Método principal que crea una instancia del servidor y ejecuta el método run.
     *
     * @param args Argumentos de la línea de comandos (no se utilizan en este caso).
     */
    public static void main(String[] args) {
    	Server server = new Server();
    	server.run();
    }
    
    /**
     * Constructor de la clase Server.
     */
    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    /**
     * El método run se ejecuta cuando se inicia un hilo para este servidor.
     */
    @Override
    public void run() {
        try {
            int port = 8080;
            server = new ServerSocket(port);
            pool = Executors.newCachedThreadPool();
            System.out.println("Servidor iniciado en el puerto: " + port);
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    /**
     * Este método se utiliza para enviar un mensaje a todos los clientes conectados, excepto al que lo envió.
     *
     * @param message   El mensaje a enviar.
     * @param nickname  El apodo del remitente del mensaje.
     */
    public void broadcast(String message, String nickname) {
        for (ConnectionHandler ch : connections) {
            if (ch != null && !ch.nickname.equals(nickname)) {
                ch.sendMessage(message);
            }
        }
    }

    /**
     * Este método se utiliza para cerrar todas las conexiones y limpiar recursos.
     */
    public void shutdown() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Clase interna que representa el manejador de conexión para un cliente.
     */
    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        /**
         * Constructor de la clase ConnectionHandler.
         *
         * @param client El socket de cliente asociado a este manejador de conexión.
         */
        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Ingrese un nombre de usuario: ");
                nickname = in.readLine();
                System.out.println("Usuario " + nickname + " conectado");
                broadcast(nickname + " se ha unido al chat!", null);
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("chao")) {
                        System.out.println("Usuario " + nickname + " desconectado");
                        broadcast("El usuario " + nickname + " abandonó", null);
                        shutdown();
                    } else {
                        System.out.println(nickname + ": " + message);
                        broadcast(nickname + ": " + message, nickname);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        /**
         * Este método se utiliza para enviar un mensaje al cliente asociado a este manejador de conexión.
         *
         * @param message El mensaje a enviar.
         */
        public void sendMessage(String message) {
            out.println(message);
        }

        /**
         * Este método se utiliza para cerrar la conexión y limpiar recursos.
         */
        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

}
