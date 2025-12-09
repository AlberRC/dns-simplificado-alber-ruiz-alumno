import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Servidor {
    private static final int PUERTO = 5000;
    private static final int MAX_CLIENTES = 5;
    private static int clientesActivos = 0;
    private static final Object lock = new Object();
    //Global para todos los hilos
    public static final HashMap<String, ArrayList<Registro>> registros = obtenerRegistrosDeFichero();

    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while(true) {
                Socket cliente = serverSocket.accept();

                //El lock es para que no puedan entrar 2 clientes a la vez, y que si ya había 4, acaben habiendo 6 en total
                synchronized(lock) {
                    //Para que no acepte más de los clientes que se especifiquen
                    if (clientesActivos >= MAX_CLIENTES) {
                        PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);
                        salida.println("Maximun capacity of clients reached.");
                        cliente.close();
                        continue;
                    } else {
                        clientesActivos++;
                    }
                }

                HiloCliente hiloCliente = new HiloCliente(cliente);
                hiloCliente.start();
            }
        } catch (IOException e) {
            System.out.println("Error de entrada y salida: " + e.getMessage());
        }
    }

    public static HashMap<String, ArrayList<Registro>> obtenerRegistrosDeFichero() {
        File fichero = new File("src/ficheroDatos.txt");
        HashMap<String, ArrayList<Registro>> registrosMap = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fichero))) {
            String linea;
            while ((linea = bufferedReader.readLine()) != null) {
                String[] partesLinea = linea.split("\\s+");

                if(partesLinea.length == 3) {
                    String nombreDominio = partesLinea[0];
                    String tipoRegistro = partesLinea[1];
                    String valor = partesLinea[2];

                    Registro registro = new Registro(nombreDominio, tipoRegistro, valor);
                    if (!registrosMap.containsKey(nombreDominio)) {
                        registrosMap.put(nombreDominio, new ArrayList<>());
                    }
                    registrosMap.get(nombreDominio).add(registro);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Fichero no encontrado: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error de entrada y salida: " + e.getMessage());
        }
        return registrosMap;
    }

    //Es mejor que la varible quede como private y modificarla a través de un metodo
    public static void restarCliente() {
        clientesActivos--;
    }
}
