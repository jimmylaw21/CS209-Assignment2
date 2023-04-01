package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Controller implements Initializable {

    @FXML
    ListView<Message> chatContentList; //聊天内容列表

    @FXML
    ListView<ChatGroup> chatList; //聊天组列表

    @FXML
    TextArea inputArea; //消息输入框

    ChatClient client;

    String[] allClientNames;

    private final Lock lock = new ReentrantLock();
    private final Condition namesUpdated = lock.newCondition();

    /**
     * 初始化聊天客户端界面，连接到聊天服务器并设置用户名。
     *
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
                chatContentList.getItems().setAll(newValue.getMessages());
            }
        });

    }

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

        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name

        String selectedUser = user.get();
        boolean chatExists = false;
        for (ChatGroup ChatGroup : chatList.getItems()) {
            //如果ChatGroup.getChatMembers()(String类型)里包含自己和选中的用户，就打开这个聊天
            if (ChatGroup.getChatMembers().contains(client.username) && ChatGroup.getChatMembers().contains(selectedUser)) {
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
            ChatGroup chatGroup = new ChatGroup(selectedUser, chatMembers);
            chatList.getItems().add(chatGroup);

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
        Message message = new Message(System.currentTimeMillis(), client.username, activeChat.toString(), messageText);
        activeChat.getMessages().add(message);
        client.sendMessage(message);
        inputArea.clear();
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
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

                    if (client.username.equals(msg.getSentBy())) { //如果当前用户的名字等于消息的发送者
                        wrapper.setAlignment(Pos.TOP_RIGHT); //设置盒子的对齐方式
                        wrapper.getChildren().addAll(msgLabel, nameLabel); //将标签添加到盒子中
                        msgLabel.setPadding(new Insets(0, 20, 0, 0)); //设置标签的内边距
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY); //设置内容的显示方式
                    setGraphic(wrapper); //设置列表项的图形
                }
            };
        }
    }

    public class ChatGroupCellFactory implements Callback<ListView<ChatGroup>, ListCell<ChatGroup>> {
        @Override
        public ListCell<ChatGroup> call(ListView<ChatGroup> param) {
            return new ListCell<ChatGroup>() {
                @Override
                protected void updateItem(ChatGroup ChatGroup, boolean empty) {
                    super.updateItem(ChatGroup, empty);
                    if (empty || ChatGroup == null) {
                        setText(null);
                    } else {
                        setText(ChatGroup.getChatMembers().get(1));
                    }
                }
            };
        }
    }

    public void sendMessage(String messageContent, String sendTo) {
        try {
            Message message = new Message(System.currentTimeMillis(), client.username, sendTo, messageContent);
            client.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        sendMessage("AllClientNames","server");

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

        sendMessage("clientName:"+input.get(),"server");
    }
    /**
     * 从服务器获取所有用户的列表，然后过滤掉当前用户自己。
     */
    public List<String> getFilteredUserList() {
        sendMessage("AllClientNames","server");

        lock.lock();
        List<String> filteredUserList = new ArrayList<>();
        try {
            namesUpdated.await();
            for (String name : allClientNames) {
                if (!name.equals(client.username)) {
                    filteredUserList.add(name);
                }
            }
            System.out.println("filteredUserList: " + filteredUserList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        return filteredUserList;
    }

    public void stop() {
        client.stop();
    }
}
