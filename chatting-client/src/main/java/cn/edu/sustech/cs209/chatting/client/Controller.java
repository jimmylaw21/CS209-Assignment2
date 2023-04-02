package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.GroupType;
import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// 写Javadoc注释，解释每个类、方法、变量的作用

/**
 * Controller类负责处理JavaFX聊天应用程序的用户界面交互。
 * 它实现了Initializable接口，以便在FXML文件加载后执行初始化操作。
 * 这个类包含了聊天组列表、聊天内容列表和输入框等UI组件，以及处理客户端通信的ChatClient对象。
 */
public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList; //聊天内容列表

    @FXML
    ListView<ChatGroup> chatList; //聊天组列表

    @FXML
    TextArea inputArea; //消息输入框

    ChatClient client; // 负责处理与服务器的通信

    String[] allClientNames; // 存储所有在线客户端的名称

    private final Lock lock = new ReentrantLock(); // 用于同步allClientNames数组的锁
    private final Condition namesUpdated = lock.newCondition(); // 与锁相关的条件变量，用于等待allClientNames更新

    /**
     * 初始化聊天客户端界面，连接到聊天服务器并设置用户名。
     * 在FXML文件加载后初始化UI组件和事件监听器。
     * 该方法将在FXML加载器调用load方法后自动调用。
     * @param url               被初始化的FXML文档的位置，或者如果该位置未知，则为null。
     * @param resourceBundle    用于本地化根对象的资源束，如果根对象未本地化，则为null。
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        try {
            client = new ChatClient("10.25.0.92", 8888, this);
            new Thread(client).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        showUsernameInputDialog();

        chatContentList.setCellFactory(new MessageCellFactory());

        chatList.setCellFactory(new ChatGroupCellFactory());

        chatList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                chatContentList.getItems().clear(); // 清空chatContentList
                chatContentList.getItems().setAll(newValue.getMessages()); // 加载新选择的群组信息
            } else {
                chatContentList.getItems().clear(); // 如果没有选中任何群组，仍然清空chatContentList
            }
        });

    }

    /**
     * 创建一个与选定用户的私人聊天，或在已有的私人聊天中打开选定用户的聊天。
     * 此方法创建一个新的JavaFX舞台，其中包含一个用于选择在线用户的下拉框和一个确定按钮。
     * 用户通过下拉框选择与之建立私人聊天的目标用户，然后点击确定按钮以创建或打开私人聊天。
     * 如果与选定用户的私人聊天已经存在，此方法会直接打开该私人聊天。
     * 否则，将创建一个新的私人聊天，并将其添加到聊天组列表中。
     */
    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>(); //原子引用

        Stage stage = new Stage(); //新建一个舞台
        ComboBox<String> userSel = new ComboBox<>(); //新建一个下拉框

        userSel.getItems().addAll(getFilteredUserList()); //将用户列表添加到下拉框中

        Button okBtn = new Button("OK"); //新建一个按钮
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem()); //设置原子引用的值
            stage.close(); //关闭舞台
        });

        HBox box = new HBox(10); //新建一个水平盒子
        box.setAlignment(Pos.CENTER); //设置盒子的对齐方式
        box.setPadding(new Insets(30, 30, 30, 30)); //设置盒子的内边距
        box.getChildren().addAll(userSel, okBtn);   //将下拉框和按钮添加到盒子中
        stage.setScene(new Scene(box)); //设置舞台的场景
        stage.showAndWait(); //显示舞台并等待

        // TODO: the title should be the selected user's name

        String selectedUser = user.get();
        boolean chatExists = false;
        if (user == null) {
            return;
        }
        for (ChatGroup ChatGroup : chatList.getItems()) {
            //如果ChatGroup.getChatMembers()(String类型)里包含自己和选中的用户，就打开这个聊天
            if (ChatGroup.getChatMembers().contains(client.username) && ChatGroup.getChatMembers().contains(selectedUser) && ChatGroup.getGroupType() == GroupType.PRIVATE) {
                chatExists = true;
                // TODO: Open the chat with the selected user
                chatList.getSelectionModel().select(ChatGroup);
                break;
            }
        }
        if (!chatExists) {
            List<String> chatMembers = new ArrayList<>();
            chatMembers.add(client.username);
            chatMembers.add(selectedUser);

            ChatGroup chatGroup = new ChatGroup(client.username, chatMembers.toString(), chatMembers, GroupType.PRIVATE);
            chatList.getItems().add(chatGroup);
            sendGroup(chatGroup);
            //切换到新建的聊天
            chatList.getSelectionModel().selectLast();
        }
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
        AtomicReference<List<String>> users = new AtomicReference<>(); //原子引用

        Stage stage = new Stage(); //新建一个舞台
        ListView<String> userList = new ListView<>(); //新建一个列表视图
        userList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); //设置列表视图的选择模式为多选
        userList.getItems().addAll(getFilteredUserList()); //将用户列表添加到列表视图中

        Button okBtn = new Button("OK"); //新建一个按钮
        okBtn.setOnAction(e -> {
            users.set(userList.getSelectionModel().getSelectedItems()); //设置原子引用的值
            stage.close(); //关闭舞台
        });

        VBox box = new VBox(10); //新建一个垂直盒子
        box.setAlignment(Pos.CENTER); //设置盒子的对齐方式
        box.setPadding(new Insets(30, 30, 30, 30)); //设置盒子的内边距
        box.getChildren().addAll(userList, okBtn);   //将列表视图和按钮添加到盒子中
        stage.setScene(new Scene(box)); //设置舞台的场景
        stage.showAndWait(); //显示舞台并等待

        // TODO: the title needs to be generated according to the naming rule

        List<String> selectedUsers = users.get();
        if (selectedUsers == null) {
            return;
        }
        boolean chatExists = false;
        for (ChatGroup ChatGroup : chatList.getItems()) {
            if (ChatGroup.getChatMembers().contains(client.username) && ChatGroup.getChatMembers().containsAll(selectedUsers) && ChatGroup.getGroupType() == GroupType.GROUP) {
                chatExists = true;

                chatList.getSelectionModel().select(ChatGroup);
                break;
            }
        }
        if (!chatExists) {
            List<String> chatMembers = new ArrayList<>();
            chatMembers.add(client.username);
            chatMembers.addAll(selectedUsers);

            ChatGroup chatGroup = new ChatGroup(client.username,chatMembers.toString(), chatMembers, GroupType.GROUP);
            chatList.getItems().add(chatGroup);
            sendGroup(chatGroup);
            chatList.getSelectionModel().selectLast();
        }
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() throws IOException {
        // TODO
        String messageText = inputArea.getText().trim();
        if (messageText.isEmpty()) {
            return;
        }
        ChatGroup activeChat = chatList.getSelectionModel().getSelectedItem();
        //sendTo为
        Message message = new Message(System.currentTimeMillis(), client.username, activeChat.getChatName(), messageText);
        activeChat.getMessages().add(message);
        chatContentList.getItems().setAll(activeChat.getMessages());
        client.sendMessage(message);
        inputArea.clear();
    }

    @FXML
    public void doSendFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a file to send");
        File file = fileChooser.showOpenDialog(chatContentList.getScene().getWindow());

        if (file != null) {
            try {
                ChatGroup activeChat = chatList.getSelectionModel().getSelectedItem();
                Message message = new Message(System.currentTimeMillis(), client.username, activeChat.getChatName(), file.getName());

                String fileName = file.getName();
                byte[] fileContent = Files.readAllBytes(file.toPath());
                message.setFileName(fileName);
                message.setFile(fileContent);

                activeChat.getMessages().add(message);
                chatContentList.getItems().setAll(activeChat.getMessages());
                client.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                // Handle the exception, show an error message or take other appropriate actions.
            }
        }

    }

    @FXML
    public void doSendEmoji() {

    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    /**
     * MessageCellFactory是一个私有类，用于定制聊天内容列表中的消息单元格。
     * 它实现了Callback接口，以自定义ListView中消息的显示方式。
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        /**
         * 用于创建列表单元格的回调方法。
         * @param param 消息列表视图。
         * @return 定制后的消息列表单元格。
         */
        @Override
        public ListCell<Message> call(ListView<Message> param) { //回调函数
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) { //更新列表项
                    super.updateItem(msg, empty); //调用父类的方法
                    if (empty || Objects.isNull(msg)) { //如果列表项为空或者消息为空
                        return;
                    }

                    HBox wrapper = new HBox(); //新建一个水平盒子
                    Label nameLabel = new Label(msg.getSentBy()); //新建一个标签
                    Label msgLabel = new Label(msg.getData()); //新建一个标签

                    nameLabel.setPrefSize(50, 20); //设置标签的大小
                    nameLabel.setWrapText(true); //设置标签的文本是否自动换行
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (msg.getFile() != null) {
                        //如果是png或者jpg格式的图片，就显示图片
                        if (msg.getFileName().endsWith(".png") || msg.getFileName().endsWith(".jpg")) {

                            Image image = convertByteArrayToImage(msg.getFile());
                            ImageView imageView = new ImageView(image);
                            imageView.setFitWidth(200);
                            imageView.setPreserveRatio(true);

                            if (client.username.equals(msg.getSentBy())) {
                                wrapper.setAlignment(Pos.TOP_RIGHT);
                                wrapper.getChildren().addAll(imageView, nameLabel);
                            } else {
                                wrapper.setAlignment(Pos.TOP_LEFT);
                                wrapper.getChildren().addAll(nameLabel, imageView);
                            }

                            imageView.setOnMouseClicked(event -> {
                                if (event.getButton() == MouseButton.PRIMARY && msg.getFile() != null) {
                                    FileChooser fileChooser = new FileChooser();
                                    fileChooser.setTitle("Save File");
                                    fileChooser.setInitialFileName(msg.getFileName());
                                    File file = fileChooser.showSaveDialog(null);

                                    if (file != null) {
                                        try {
                                            Files.write(file.toPath(), msg.getFile());
                                            showAlert(Alert.AlertType.INFORMATION, "Success", "File saved successfully!");
                                        } catch (IOException e) {
                                            showAlert(Alert.AlertType.ERROR, "Error", "Error occurred while saving the file.");
                                        }
                                    }
                                }
                            });
                        } else {
                            //如果是其他格式的文件，就显示文件名
                            if (client.username.equals(msg.getSentBy())) {
                                wrapper.setAlignment(Pos.TOP_RIGHT);
                                wrapper.getChildren().addAll(msgLabel, nameLabel);
                                msgLabel.setPadding(new Insets(0, 20, 0, 0));
                            } else {
                                wrapper.setAlignment(Pos.TOP_LEFT);
                                wrapper.getChildren().addAll(nameLabel, msgLabel);
                                msgLabel.setPadding(new Insets(0, 0, 0, 20));
                            }
                        }
                    } else {
                        if (client.username.equals(msg.getSentBy())) {
                            wrapper.setAlignment(Pos.TOP_RIGHT);
                            wrapper.getChildren().addAll(msgLabel, nameLabel);
                            msgLabel.setPadding(new Insets(0, 20, 0, 0));
                        } else {
                            wrapper.setAlignment(Pos.TOP_LEFT);
                            wrapper.getChildren().addAll(nameLabel, msgLabel);
                            msgLabel.setPadding(new Insets(0, 0, 0, 20));
                        }
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);

                    // 添加点击事件监听器
                    msgLabel.setOnMouseClicked(event -> {
                        System.out.println("Clicked on message: " + msg.getData());
                        if (event.getButton() == MouseButton.PRIMARY && msg.getFile() != null) {
                            // 提示用户选择文件保存位置
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle("Save File");
                            fileChooser.setInitialFileName(msg.getFileName()); // 设置默认文件名，从Message对象中获取
                            File file = fileChooser.showSaveDialog(null);

                            if (file != null) {
                                try {
                                    // 将文件数据写入到选择的文件中
                                    Files.write(file.toPath(), msg.getFile());
                                    // 显示成功提示
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("File Saved");
                                    alert.setHeaderText("File saved successfully");
                                    alert.setContentText("The file has been saved to: " + file.getAbsolutePath());
                                    alert.showAndWait();
                                } catch (IOException e) {
                                    // 显示错误提示
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Error");
                                    alert.setHeaderText("File save error");
                                    alert.setContentText("An error occurred while saving the file: " + e.getMessage());
                                    alert.showAndWait();
                                }
                            }
                        }
                    });
                }
            };
        }
    }

    public void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * ChatGroupCellFactory是一个公共类，用于定制聊天组列表中的聊天组单元格。
     * 它实现了Callback接口，以自定义ListView中聊天组的显示方式。
     */
    public class ChatGroupCellFactory implements Callback<ListView<ChatGroup>, ListCell<ChatGroup>> {
        /**
         * 用于创建聊天组列表单元格的回调方法。
         * @param param 聊天组列表视图。
         * @return 定制后的聊天组列表单元格。
         */
        @Override
        public ListCell<ChatGroup> call(ListView<ChatGroup> param) {
            return new ListCell<ChatGroup>() {
                @Override
                protected void updateItem(ChatGroup ChatGroup, boolean empty) {
                    super.updateItem(ChatGroup, empty);
                    if (empty || ChatGroup == null) {
                        setText(null);
                    } else {
                        //用lambda表达式，若ChatName是类似[a, b, c]的形式
                        if (ChatGroup.getChatName().startsWith("[") && ChatGroup.getChatName().endsWith("]")){
                            if (ChatGroup.getGroupType().equals(GroupType.GROUP)){
                                if (ChatGroup.getChatMembers().size() <= 3) {
                                    setText(ChatGroup.getChatName().substring(1, ChatGroup.getChatName().length() - 1));
                                } else {
                                    setText(ChatGroup.getChatName().substring(1, ChatGroup.getChatName().length() - 1).split(", ")[0]
                                            + ", " + ChatGroup.getChatName().substring(1, ChatGroup.getChatName().length() - 1).split(", ")[1]
                                            + ", " + ChatGroup.getChatName().substring(1, ChatGroup.getChatName().length() - 1).split(", ")[2]
                                            + ", ...");
                                }
                            } else if (ChatGroup.getGroupType().equals(GroupType.PRIVATE)){
                                String otherUser = ChatGroup.getChatMembers().get(0).equals(client.username) ? ChatGroup.getChatMembers().get(1) : ChatGroup.getChatMembers().get(0);
                                setText(otherUser);
                            }
                        }else{
                            //否则直接显示ChatName(自定义)
                            setText(ChatGroup.getChatName());
                        }
                    }
                }
            };
        }
    }

    /**
     * 发送消息给指定的接收者。
     * @param messageContent 要发送的消息内容。
     * @param sendTo 消息的接收者。
     */
    public void sendMessage(String messageContent, String sendTo) {
        try {
            Message message = new Message(System.currentTimeMillis(), client.username, sendTo, messageContent);
            client.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送整个聊天组（包括聊天记录）给服务器。
     * @param chatGroup 要发送的聊天组对象。
     */
    public void sendGroup(ChatGroup chatGroup) {
        try {
            Group group = new Group(chatGroup.getCreator(), chatGroup.getChatName(), chatGroup.getChatMembers(), chatGroup.getGroupType());
            //将chatGroup中的消息加进group中
            for (Message message : chatGroup.getMessages()) {
                group.addMessage(message);
            }
            client.sendGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新客户端列表。
     * @param allClientNames 包含所有客户端名称的字符串数组。
     */
    public void updateClientList(String[] allClientNames) {
        lock.lock();
        try {
            this.allClientNames = allClientNames;
            namesUpdated.signal();
        } finally {
            lock.unlock();
        }
    }
    /**
     * 显示用户名输入对话框并验证用户名是否唯一。
     * 如果输入的用户名已经存在，将提示用户更换用户名。
     * 如果用户选择退出，将关闭应用程序。
     */
    private void showUsernameInputDialog() {
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        Label errorMessage = new Label();
        errorMessage.setTextFill(Color.RED);

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(errorMessage, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(() -> username.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return username.getText();
            }
            return null;
        });

        Optional<String> input = dialog.showAndWait();
        sendMessage("AllClientNames","Server");

        lock.lock();

        try {
            namesUpdated.await();
            while (Arrays.asList(allClientNames).contains(input.get())) {
                errorMessage.setText("Username already exists, please choose another one.");
                input = dialog.showAndWait();
                if (!input.isPresent() || input.get().isEmpty()) {
                    System.out.println("Invalid username " + input + ", exiting");
                    Platform.exit();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        client.username = input.get();

        sendMessage("clientName:"+input.get(),"Server");
    }
    /**
     * 从服务器获取所有用户的列表，然后过滤掉当前用户自己。
     */
    public List<String> getFilteredUserList() {
        sendMessage("AllClientNames","Server");

        lock.lock();
        List<String> filteredUserList = new ArrayList<>();
        try {
            namesUpdated.await();
            for (String name : allClientNames) {
                if (!name.equals(client.username)) {
                    filteredUserList.add(name);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        return filteredUserList;
    }

    /**
     * 当收到消息时触发的操作。
     * @param message 收到的消息对象。
     */
    public void onReceiveMessage(Message message) {
        Platform.runLater(() -> {
            ChatGroup activeChat = chatList.getSelectionModel().getSelectedItem();
            if (activeChat == null) {
                return;
            }
            if (activeChat.getChatName().equals(message.getSendTo())) {
                chatContentList.getItems().setAll(activeChat.getMessages());
            }
        });
    }

    private Image convertByteArrayToImage(byte[] imageData) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
        return new Image(inputStream);
    }

    public void stop() {
        client.stop();
    }

    /**
     * 当服务器关闭时触发的操作。
     */
    public void onServerShutdown() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Server is shutting down");
            alert.setContentText("The server is shutting down, please try again later.");
            alert.showAndWait();
            Platform.exit();
        });
    }
}
