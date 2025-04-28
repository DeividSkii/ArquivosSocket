import java.io.*;
import java.net.Socket;

public class ClienteArquivos {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Digite o usuário:");
            String usuario = consoleIn.readLine();
            out.writeUTF(usuario);

            System.out.println("Digite a senha:");
            String senha = consoleIn.readLine();
            out.writeUTF(senha);

            String resposta = in.readUTF();
            if (resposta.equals("ERRO")) {
                System.out.println("Login falhou.");
                return;
            }

            System.out.println("Login realizado com sucesso!");

            while (true) {
                System.out.println(in.readUTF());
                String opcao = consoleIn.readLine();
                out.writeUTF(opcao);

                switch (opcao) {
                    case "1":
                        System.out.println(in.readUTF());
                        break;
                    case "2":
                        baixarArquivo(in, out, consoleIn);
                        break;
                    case "3":
                        enviarArquivo(in, out, consoleIn);
                        break;
                    case "4":
                        return;
                    default:
                        System.out.println(in.readUTF());
                }
            }
        } catch (IOException e) {
            System.out.println("Erro no cliente: " + e.getMessage());
        }
    }

    private static void baixarArquivo(DataInputStream in, DataOutputStream out, BufferedReader consoleIn) throws IOException {
        System.out.println(in.readUTF());
        String tipo = consoleIn.readLine();
        out.writeUTF(tipo);

        System.out.println(in.readUTF());
        String nomeArq = consoleIn.readLine();
        out.writeUTF(nomeArq);

        String status = in.readUTF();
        if (status.startsWith("ERRO")) {
            System.out.println(status);
            return;
        }

        long tamanhoArquivo = in.readLong();
        File destino = new File(nomeArq);

        try (FileOutputStream fileOut = new FileOutputStream(destino)) {
            byte[] buffer = new byte[8192];
            long totalLido = 0;
            int bytesLidos;
            while (totalLido < tamanhoArquivo && (bytesLidos = in.read(buffer, 0, (int)Math.min(buffer.length, tamanhoArquivo - totalLido))) != -1) {
                fileOut.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;
            }
        }

        System.out.println("Arquivo baixado com sucesso!");
    }

    private static void enviarArquivo(DataInputStream in, DataOutputStream out, BufferedReader consoleIn) throws IOException {
        System.out.println(in.readUTF());
        String tipo = consoleIn.readLine();
        out.writeUTF(tipo);

        System.out.println(in.readUTF());
        String nomeArq = consoleIn.readLine();
        out.writeUTF(nomeArq);

        String status = in.readUTF();
        if (!status.equals("OK")) {
            System.out.println("Erro ao enviar arquivo.");
            return;
        }

        File origem = new File(nomeArq);
        if (!origem.exists()) {
            System.out.println("Arquivo local não encontrado!");
            return;
        }

        out.writeLong(origem.length());

        try (FileInputStream fileIn = new FileInputStream(origem)) {
            byte[] buffer = new byte[8192];
            int bytesLidos;
            while ((bytesLidos = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLidos);
            }
        }

        System.out.println(in.readUTF());
    }
}
