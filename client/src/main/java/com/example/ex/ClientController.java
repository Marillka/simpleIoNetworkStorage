package com.example.ex;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    private String homeDir;

    private byte[] buf;

    private Network network;
    @FXML
    public ListView<String> clientView;
    @FXML
    public ListView<String> serverView;


    private void readLoop() {
        try {
            while (true) {
                // получаем команду от сервера
                String command = network.readString();

                if (command.equals("#list#")) {
                    // чистим список от старых данных
                    Platform.runLater(() -> serverView.getItems().clear());
                    // получили команду лист далее читаем размер файлов
                    int len = network.readInt();
                    // пробегаемся по отправленным файлам и закидываем в ListView
                    for (int i = 0; i < len; i++) {
                        String file = network.readString();
                        Platform.runLater(() -> serverView.getItems().add(file));
                    }
                }

                if (command.equals("#file_request#")) {
                    String fileName = network.getIs().readUTF();
                    long len = network.getIs().readLong();// получаем размер файла
                    File file = Path.of(homeDir).resolve(fileName).toFile();// создаем абстракцию файла
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        for (int i = 0; i < (255 + len) / 256; i++) {
                            int read = network.getIs().read(buf);
                            fos.write(buf);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> updateClientView());
                }
            }
        } catch (Exception e) {
            System.err.println("Connection lost");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            buf = new byte[256];
            homeDir = System.getProperty("user.home");
            updateClientView();
            network = new Network(8189);
            Thread readThread = new Thread(this::readLoop);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void updateClientView() {
        clientView.getItems().clear();
        clientView.getItems().addAll(getFiles(homeDir));
    }

    private List<String> getFiles(String dir) {
        String[] list = new File(dir).list();
        return Arrays.asList(list);
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        network.getOs().writeUTF("#file#");
//        new File(homeDir + "/" + file);// берем Path, а не File, потому что делимитер на винде один, на маке другой.
        String file = clientView.getSelectionModel().getSelectedItem();
//        Path.of(homeDir,file);// склеивает homedir и file (C:/Users/Valery/"file") - можно вот так
//        Path.of(homeDir).resolve(file);// есть такая директория и в этой директории находим вот такой файл.
        // отправляем имя файла
        network.getOs().writeUTF(file);
        File toSend = Path.of(homeDir).resolve(file).toFile();
        network.getOs().writeLong(toSend.length());// дальше отправляем размер файла
        try (FileInputStream fis = new FileInputStream(toSend)) {// тепрь отправляем байты этого файла
            while (fis.available() > 0) {// пока есть что прочитать - читаем
                // запоминаем сколько прочитали
                int read = fis.read(buf);
                // отправляем то количество - сколько прочитали
                network.getOs().write(buf, 0, read);
            }
        }
        network.getOs().flush();
    }

    public void download(ActionEvent actionEvent) throws IOException {
        network.getOs().writeUTF("#file_request#");
        String file = serverView.getSelectionModel().getSelectedItem();
        network.getOs().writeUTF(file);
    }
}