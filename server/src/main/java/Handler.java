import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Handler implements Runnable {

    private final String serverDir = "Z:\\gb_IoClientAndServer\\server\\server_files";
    private DataInputStream is;
    private DataOutputStream os;


    public Handler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        System.out.println("Client accepted");
        sendListOfFiles(serverDir);
    }

    private void sendListOfFiles(String dir) throws IOException {
        // сначала шлем команду о том что будем пересылать
        os.writeUTF("#list#");
        // получаем список файлов из нужной нам директории
        List<String> files = getFiles(serverDir);
        // дальше посылаем размер
        os.writeInt(files.size());
        // дальше пересылаем файлы
        for (String file : files) {
            os.writeUTF(file);
        }
        os.flush();
    }


    private List<String> getFiles(String dir) {
        String[] list = new File(dir).list();
        return Arrays.asList(list);
    }


    @Override
    public void run() {
        byte[] buffer = new byte[256];
        try {
            while (true) {
                String command = is.readUTF();// читаем команду с клиента
                System.out.println("received: " + command);

                if (command.equals("#file#")) {
                    String fileName = is.readUTF();// сначала получаем имя файла
                    long len = is.readLong();// получаем размер файла
                    File file = Path.of(serverDir).resolve(fileName).toFile();// создаем абстракцию файла
                    try (FileOutputStream fos = new FileOutputStream(file)) {// создаем fos
                        // если пришло 256 байт, то все нормаль но выполнится
                        // если придет уже 257 байт, то цикл надо будет выполнить дважды
                        // поэтому делаем такую фичу (256 + 255) / 256 = 1.99
                        for (int i = 0; i < (len + 255) / 256; i++) {
                            int read = is.read(buffer);// читаем
                            // пишем в файл
                            fos.write(buffer);// flush не надо потому что try with resources
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sendListOfFiles(serverDir);
                }

                if (command.equals("#file_request#")) {
                    os.writeUTF("#file_request#");
                    String fileName = is.readUTF();// считаваем имя файла
                    File fileToSend = Path.of(serverDir).resolve(fileName).toFile();// создаем абстрацию файла
                    os.writeUTF(fileName);
                    long len = fileToSend.length();
                    os.writeLong(len);
                    try (FileInputStream fis = new FileInputStream(fileToSend)) {
                        while (fis.available() > 0) {
                            int read = fis.read(buffer);// запоминаем сколько прочитали
                            os.write(buffer, 0, read);// отправляем, сколько прочитали
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    os.flush();
                }
            }

        } catch (Exception e) {
            System.err.println("Connection was broken");
        }
    }


}
