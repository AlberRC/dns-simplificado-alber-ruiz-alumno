import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Servidor {
    public static final int PUERTO = 5000;
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            HashMap<String, ArrayList<Registro>> registros = obtenerRegistrosDeFichero();

            while(true) {
                Socket cliente = serverSocket.accept();
                System.out.println("Cliente conectado desde " + cliente.getInetAddress().getHostAddress());

                BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);

                atenderClienteActual(entrada, salida, registros);

                cliente.close();
                entrada.close();
                salida.close();
            }
        } catch (IOException e) {
            System.out.println("Error de entrada y salida: " + e.getMessage() );
        }
    }

    public static HashMap<String, ArrayList<Registro>> obtenerRegistrosDeFichero() {
        File fichero = new File("src/ficheroDatos.txt");
        HashMap<String, ArrayList<Registro>> registrosMap = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fichero))) {
            String linea;
            while ((linea = bufferedReader.readLine()) != null) {
                //La expresión regular es por si hay más de un espacio entre las palabras
                String[] partesLinea = linea.split("\\s+");

                if(partesLinea.length == 3) { //Por si hay líneas vacías o los registros no cumplen con el formato requerido
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

    public static void atenderClienteActual(BufferedReader entrada, PrintWriter salida, HashMap<String, ArrayList<Registro>> registros) throws IOException {
        try {
            boolean clienteActivo = true;
            while(clienteActivo) {
                String solicitudCliente = entrada.readLine();
                solicitudCliente = limpiarCadenaTelnet(solicitudCliente);
                System.out.println("El cliente solicita: " + solicitudCliente);
                String[] partesMensaje = solicitudCliente.split(" ");
                if(solicitudCliente.equals("EXIT")) {
                    System.out.println("Conexión con cliente cerrada.");
                    clienteActivo = false;

                } else if(partesMensaje[0].equals("LIST")) {
                    System.out.println("Listando registros.");
                    salida.println("150 Inicio listado");
                    for (String dominio : registros.keySet()) {
                        ArrayList<Registro> registrosDominio = registros.get(dominio);
                        for (Registro registro : registrosDominio) {
                            salida.println(registro.getNombreDominio() + " " + registro.getTipoRegistro() + " " + registro.getValor());
                        }
                    }
                    salida.println("226 Fin listado");
                } else if(!partesMensaje[0].equals("LOOKUP") || partesMensaje.length != 3) {
                    System.out.println("Formato de solicitud incorrecto.");
                    salida.println("400 Bad Request");
                } else {
                    String tipoRegistroSolicitado = partesMensaje[1];
                    String valorSolicitado = partesMensaje[2];
                    if (registros.containsKey(valorSolicitado)) {
                        ArrayList<Registro> registrosDominio = registros.get(valorSolicitado);
                        ArrayList<String> valoresEncontrados = new ArrayList<>();

                        //Por si hay más de un registro (MX) con el mismo nombre de dominio
                        for (Registro registro : registrosDominio) {
                            //Aparte del nombre de dominio, que sea el mismo tipo de registro
                            if (registro.getTipoRegistro().equals(tipoRegistroSolicitado)) {
                                valoresEncontrados.add(registro.getValor());
                            }
                        }

                        if (!valoresEncontrados.isEmpty()) {
                            System.out.println("Resultado encontrado.");
                            for (String valorEncontrado : valoresEncontrados) {
                                salida.println("200 " + valorEncontrado);
                            }
                        } else {
                            System.out.println("No se encontró ningún resultado.");
                            salida.println("404 Not Found");
                        }
                    } else {
                        System.out.println("No se encontró ningún resultado.");
                        salida.println("404 Not Found");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error inesperado.");
            salida.println("500 Server Error");
        }
    }

    //Esto sirve porque telnet para cambiar un caracter, no lo elimina, así que la limpieza del String se hace aquí
    public static String limpiarCadenaTelnet(String solicitudCliente) {
        StringBuilder resultado = new StringBuilder();
        boolean enSecuenciaEscape = false;

        for (int i = 0; i < solicitudCliente.length(); i++) {
            char c = solicitudCliente.charAt(i);

            if (c == '\u001B') {
                enSecuenciaEscape = true;
                continue;
            }

            if (enSecuenciaEscape) {
                if (c == '[') {
                    continue;
                }
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                    enSecuenciaEscape = false;
                }
                continue;
            }

            if (c == '\b' || c == 127) {
                if (!resultado.isEmpty()) {
                    resultado.deleteCharAt(resultado.length() - 1);
                }
                continue;
            }

            if (c >= 32 && c <= 126) {
                resultado.append(c);
            }
        }

        return resultado.toString();
    }
}
