import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ServidorArquivos {
    private static final int PORTA = 12345;
    private static final String PASTA_BASE = "armazenamento";
    private static final Map<String, String> usuarios = new HashMap<>();

    public static void main(String[] args) {
        usuarios.put("deivid", "senha123");
        usuarios.put("santos", "senha123");

        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            System.out.println("Servidor iniciado na porta " + PORTA);

            while (true) {
                Socket cliente = servidor.accept();
                new Thread(() -> trataCliente(cliente)).start();
            }
        } catch (IOException e) {
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }

    private static void trataCliente(Socket cliente) {
        try (
                DataInputStream in = new DataInputStream(cliente.getInputStream());
                DataOutputStream out = new DataOutputStream(cliente.getOutputStream())
        ) {
            String usuario = in.readUTF();
            String senha = in.readUTF();

            if (!usuarios.containsKey(usuario) || !usuarios.get(usuario).equals(senha)) {
                out.writeUTF("ERRO");
                return;
            }

            out.writeUTF("OK");
            criaPastasUsuario(usuario);

            while (true) {
                out.writeUTF("\nMenu:\n1. Listar arquivos\n2. Baixar arquivo\n3. Enviar arquivo\n4. Sair\nDigite sua opção:");
                String opcao = in.readUTF();

                switch (opcao) {
                    case "1":
                        listaArquivos(usuario, out);
                        break;
                    case "2":
                        baixarArquivo(usuario, in, out);
                        break;
                    case "3":
                        receberArquivo(usuario, in, out);
                        break;
                    case "4":
                        return;
                    default:
                        out.writeUTF("ERRO: Opção inválida");
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao tratar cliente: " + e.getMessage());
        }
    }

    private static void criaPastasUsuario(String usuario) {
        String[] pastas = {"pdf", "jpg", "txt"};
        File dirUsuario = new File(PASTA_BASE, usuario);

        if (!dirUsuario.exists()) {
            dirUsuario.mkdirs();
            for (String pasta : pastas) {
                new File(dirUsuario, pasta).mkdir();
            }
        }
    }

    private static void listaArquivos(String usuario, DataOutputStream out) throws IOException {
        File dirUsuario = new File(PASTA_BASE, usuario);
        File[] pastas = dirUsuario.listFiles();

        StringBuilder resposta = new StringBuilder();
        if (pastas != null) {
            for (File pasta : pastas) {
                resposta.append("\n").append(pasta.getName()).append(":\n");
                File[] arquivos = pasta.listFiles();
                if (arquivos != null) {
                    for (File arq : arquivos) {
                        resposta.append(" - ").append(arq.getName()).append("\n");
                    }
                }
            }
        }
        out.writeUTF(resposta.toString());
    }

    private static void baixarArquivo(String usuario, DataInputStream in, DataOutputStream out) throws IOException {
        out.writeUTF("Digite o tipo do arquivo (pdf, jpg, txt):");
        String tipo = in.readUTF();
        out.writeUTF("Digite o nome do arquivo:");
        String nomeArq = in.readUTF();

        File arquivo = new File(PASTA_BASE + "/" + usuario + "/" + tipo + "/" + nomeArq);

        if (!arquivo.exists()) {
            out.writeUTF("ERRO: Arquivo não encontrado");
            return;
        }

        out.writeUTF("OK");
        out.writeLong(arquivo.length());

        try (FileInputStream fileIn = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[8192];
            int bytesLidos;
            while ((bytesLidos = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLidos);
            }
        }
    }

    private static void receberArquivo(String usuario, DataInputStream in, DataOutputStream out) throws IOException {
        out.writeUTF("Digite o tipo do arquivo (pdf, jpg, txt):");
        String tipo = in.readUTF();
        out.writeUTF("Digite o nome do arquivo:");
        String nomeArq = in.readUTF();

        File destino = new File(PASTA_BASE + "/" + usuario + "/" + tipo + "/" + nomeArq);

        out.writeUTF("OK");
        long tamanhoArquivo = in.readLong();

        try (FileOutputStream fileOut = new FileOutputStream(destino)) {
            byte[] buffer = new byte[8192];
            long totalLido = 0;
            int bytesLidos;
            while (totalLido < tamanhoArquivo && (bytesLidos = in.read(buffer, 0, (int)Math.min(buffer.length, tamanhoArquivo - totalLido))) != -1) {
                fileOut.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;
            }
        }

        out.writeUTF("OK: Arquivo recebido");
    }
}
