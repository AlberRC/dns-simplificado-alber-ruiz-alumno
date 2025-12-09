import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class HiloCliente extends Thread {
    private final Socket cliente;

    public HiloCliente(Socket cliente) {
        this.cliente = cliente;
    }

    @Override
    public void run() {
        try {
            System.out.println("Cliente conectado desde " + cliente.getInetAddress().getHostName());
            BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);

            atenderCliente(entrada, salida);

            entrada.close();
            salida.close();
        } catch (IOException e) {
            System.out.println("Error de entrada y salida: " + e.getMessage());
        } finally {
            try {
                cliente.close();
            } catch (IOException e) {
                System.out.println("Error de entrada y salida: " + e.getMessage());
            }

            Servidor.restarCliente();
        }
    }

    private void atenderCliente(BufferedReader entrada, PrintWriter salida) throws IOException {
        try {
            boolean clienteActivo = true;
            while(clienteActivo) {
                String solicitudCliente = entrada.readLine();
                if (solicitudCliente == null) {
                    break;
                }

                solicitudCliente = limpiarCadenaTelnet(solicitudCliente);
                String[] partesMensaje = solicitudCliente.split(" ");

                if(solicitudCliente.equals("EXIT")) {
                    System.out.println("Conexión con cliente cerrada.");
                    clienteActivo = false;
                } else if(partesMensaje[0].equals("REGISTER") && partesMensaje.length == 4 &&
                        (partesMensaje[2].equals("A") || partesMensaje[2].equals("MX") || partesMensaje[2].equals("CNAME"))) {
                    String nombreDominio = partesMensaje[1];
                    String tipoRegistro = partesMensaje[2];
                    String valor = partesMensaje[3];

                    //Para que no interactúen varios clientes a la vez, causando errores
                    synchronized(Servidor.registros) {
                        Registro registro = new Registro(nombreDominio, tipoRegistro, valor);

                        if (!Servidor.registros.containsKey(nombreDominio)) {
                            Servidor.registros.put(nombreDominio, new ArrayList<>());
                        }
                        Servidor.registros.get(nombreDominio).add(registro);

                        try {
                            File file = new File("src/ficheroDatos.txt");
                            PrintWriter salidaNuevoRegistro = new PrintWriter(new FileWriter(file, true));
                            salidaNuevoRegistro.printf("\n%s   %s   %s", nombreDominio, tipoRegistro, valor);
                            System.out.println("Nuevo registro añadido correctamente.");
                            salida.println("200 Record added");
                            salidaNuevoRegistro.close();
                        } catch (IOException e) {
                            salida.println("Error writing new register");
                        }
                    }
                } else if(solicitudCliente.equals("LIST")) {
                    System.out.println("Listando registros.");
                    salida.println("150 Inicio listado");

                    //Para que no interactúen varios clientes a la vez, causando errores
                    synchronized(Servidor.registros) {
                        for (String dominio : Servidor.registros.keySet()) {
                            ArrayList<Registro> registrosDominio = Servidor.registros.get(dominio);
                            for (Registro registro : registrosDominio) {
                                salida.println(registro.getNombreDominio() + " " + registro.getTipoRegistro() + " " + registro.getValor());
                            }
                        }
                    }
                    salida.println("226 Fin listado");
                } else if(partesMensaje[0].equals("LOOKUP") && partesMensaje.length == 3) {
                    String tipoRegistroSolicitado = partesMensaje[1];
                    String valorSolicitado = partesMensaje[2];

                    //Para que no interactúen varios clientes a la vez, causando errores
                    synchronized(Servidor.registros) {
                        if (Servidor.registros.containsKey(valorSolicitado)) {
                            ArrayList<Registro> registrosDominio = Servidor.registros.get(valorSolicitado);
                            ArrayList<String> valoresEncontrados = new ArrayList<>();

                            for (Registro registro : registrosDominio) {
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

                } else {
                    System.out.println("Formato de solicitud incorrecto.");
                    salida.println("400 Bad Request");
                }
            }
        } catch (Exception e) {
            System.out.println("Error inesperado.");
            salida.println("500 Server Error");
        }
    }

    public String limpiarCadenaTelnet(String solicitudCliente) {
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