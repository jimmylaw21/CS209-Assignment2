package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.GroupType;
import cn.edu.sustech.cs209.chatting.common.Message;
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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;


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

  @FXML
  Label currentUsername; //用户名标签

  @FXML
  Label currentOnlineCnt; //当前聊天组标签

  ChatClient client; // 负责处理与服务器的通信

  String[] allClientNames; // 存储所有在线客户端的名称

  private Popup emojiPopup;

  private List<String> emojiList;

  private final Lock lock = new ReentrantLock(); // 用于同步allClientNames数组的锁
  private final Condition namesUpdated = lock.newCondition();
  private final Condition loginResult = lock.newCondition();
  private boolean loginSuccess = false;

  /**
   * 初始化聊天客户端界面，连接到聊天服务器并设置用户名。
   * 在FXML文件加载后初始化UI组件和事件监听器。
   * 该方法将在FXML加载器调用load方法后自动调用。
   *
   * @param url            被初始化的FXML文档的位置，或者如果该位置未知，则为null。
   * @param resourceBundle 用于本地化根对象的资源束，如果根对象未本地化，则为null。
   */
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    //  try {
    //    client = new ChatClient(this);
    //    new Thread(client).start();
    //  } catch (IOException e) {
    //    throw new RuntimeException(e);
    //  }

    try {
      client = new ChatClient("10.12.97.44", 8888, this);
      new Thread(client).start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


    showUsernameInputDialog();

    chatContentList.setCellFactory(new MessageCellFactory());

    chatList.setCellFactory(new ChatGroupCellFactory());

    chatList.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue != null) {
            //foreach打印chatContentList聊天内容
            for (Message message : chatContentList.getItems()) {
              System.out.println(message);
            }
            chatContentList.getItems().clear();
            chatContentList.getItems().setAll(newValue.getMessages());
            newValue.setHasUnreadMessages(false);
          } else {
            chatContentList.getItems().clear(); // 如果没有选中任何群组，仍然清空chatContentList
          }
        });

    initEmoji();
    initEmojiPopup();

    inputArea.setOnContextMenuRequested(event -> {
      if (emojiPopup.isShowing()) {
        emojiPopup.hide();
      } else {
        emojiPopup.show(inputArea, event.getScreenX(), event.getScreenY() - emojiPopup.getHeight());
      }
      event.consume();
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

    String selectedUser = user.get();
    boolean chatExists = false;
    if (selectedUser == null || selectedUser.equals(client.username)) {
      return;
    }
    for (ChatGroup chatGroup : chatList.getItems()) {
      //如果ChatGroup.getChatMembers()(String类型)里包含自己和选中的用户，就打开这个聊天
      if (chatGroup.getChatMembers().contains(client.username) 
          && chatGroup.getChatMembers().contains(selectedUser) 
          && chatGroup.getGroupType() == GroupType.PRIVATE) {
        chatExists = true;
        chatList.getSelectionModel().select(chatGroup);
        break;
      }
    }
    if (!chatExists) {
      List<String> chatMembers = new ArrayList<>();
      chatMembers.add(client.username);
      chatMembers.add(selectedUser);

      ChatGroup chatGroup = new ChatGroup(client.username, 
          chatMembers.toString(), chatMembers, GroupType.PRIVATE);
      chatList.getItems().add(chatGroup);
      sendGroup(chatGroup);
      //切换到新建的聊天
      chatList.getSelectionModel().selectLast();
    }
  }

  /**
   * 创建一个新的群聊，或在已有的群聊中打开选定用户的聊天。
   * 此方法创建一个新的JavaFX舞台，其中包含一个用于选择在线用户的列表视图和一个确定按钮。
   * 用户通过列表视图选择与之建立群聊的目标用户，然后点击确定按钮以创建或打开群聊。
   * 如果与选定用户的群聊已经存在，此方法会直接打开该群聊。
   * 否则，将创建一个新的群聊，并将其添加到聊天组列表中。
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

    List<String> selectedUsers = users.get();
    if (selectedUsers.isEmpty() || selectedUsers == null) {
      System.out.println("Group creation dialog closed unexpectedly, skipping");
      return;
    }

    boolean chatExists = false;
    for (ChatGroup chatGroup : chatList.getItems()) {
      if (chatGroup.getChatMembers().contains(client.username) 
          && chatGroup.getChatMembers().containsAll(selectedUsers) 
          && chatGroup.getGroupType() == GroupType.GROUP) {
        chatExists = true;

        chatList.getSelectionModel().select(chatGroup);
        break;
      }
    }
    if (!chatExists) {
      List<String> chatMembers = new ArrayList<>();
      chatMembers.add(client.username);
      chatMembers.addAll(selectedUsers);

      ChatGroup chatGroup = new ChatGroup(client.username, 
          chatMembers.toString(), chatMembers, GroupType.GROUP);
      chatList.getItems().add(chatGroup);
      sendGroup(chatGroup);
      chatList.getSelectionModel().selectLast();
    }
  }

  @FXML
  public void showGroupMember() {
    ListView<String> userList = new ListView<>(); //新建一个列表视图
    userList.getItems().addAll(chatList.getSelectionModel().getSelectedItem()
        .getChatMembers()); //将用户列表添加到列表视图中

    //展示选中群组成员
    VBox box = new VBox(10); //新建一个垂直盒子
    box.setAlignment(Pos.CENTER); //设置盒子的对齐方式
    box.setPadding(new Insets(30, 30, 30, 30)); //设置盒子的内边距
    box.getChildren().addAll(userList);   //将列表视图添加到盒子中
    Stage stage = new Stage(); //新建一个舞台
    stage.setScene(new Scene(box)); //设置舞台的场景
    stage.showAndWait(); //显示舞台并等待
  }


  /**
   * 向选定的聊天组发送消息。
   * 如果消息文本为空，则不执行任何操作。
   * 示例：在消息文本中添加emoji（这里以 😊 为例）。
   *
   * @throws IOException 当发送消息失败时抛出异常
   */
  @FXML
  public void doSendMessage() throws IOException {
    String messageText = inputArea.getText().trim();
    if (messageText.isEmpty()) {
      return;
    }
    ChatGroup activeChat = chatList.getSelectionModel().getSelectedItem();
    //sendTo为
    Message message = new Message(System.currentTimeMillis(), 
        client.username, activeChat.getChatName(), messageText);
    activeChat.getMessages().add(message);
    chatContentList.getItems().clear();
    chatContentList.getItems().setAll(activeChat.getMessages()); // 加载新选择的群组信息
    client.sendMessage(message);
    updateGroupOrder();
    inputArea.clear();
  }

  private void initEmoji() {
    emojiList = new ArrayList<>();
    String[] emojis = {"😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇"};
    emojiList.addAll(Arrays.asList(emojis));
  }

  @FXML
  public void addEmojiToText() {
    //打开一个页面，选择emoji，然后将其添加到消息文本中
    Stage stage = new Stage(); //新建一个舞台
    ListView<String> emojiListView = new ListView<>(); //新建一个列表视图
    emojiListView.getItems().addAll(emojiList);
    emojiListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE); //设置列表视图的选择模式为单选

    emojiListView.setOnMouseClicked(e -> {
      String emoji = emojiListView.getSelectionModel().getSelectedItem();
      inputArea.appendText(emoji);
      stage.close();
    });

    stage.setScene(new Scene(emojiListView)); //设置舞台的场景
    stage.showAndWait(); //显示舞台并等待
  }

  /**
   * 向选定的聊天组发送文件。
   * 如果未选择文件，则不执行任何操作。
   *
   * @throws IOException 当发送消息失败时抛出异常
   */
  @FXML
  public void doSendFile() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select a file to send");
    File file = fileChooser.showOpenDialog(chatContentList.getScene().getWindow());

    if (file != null) {
      try {
        ChatGroup activeChat = chatList.getSelectionModel().getSelectedItem();
        Message message = new Message(System.currentTimeMillis(), 
            client.username, activeChat.getChatName(), file.getName());

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


  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
    @Override
    public ListCell<Message> call(ListView<Message> param) { //回调函数
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) { //更新列表项
          super.updateItem(msg, empty); //调用父类的方法
          if (empty || Objects.isNull(msg)) { //如果列表项为空或者消息为空
            setText(null); //设置文本为空
            setGraphic(null); //设置图形为空
            setStyle("-fx-background-color: #FFFFFF;"); // 例如，白色
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
                      showAlert(Alert.AlertType.ERROR, "Error", 
                          "Error occurred while saving the file.");
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

          // 设置单元格的背景颜色
          if (client.username.equals(msg.getSentBy())) {
            // 如果发送者是当前用户，设置一种背景颜色
            // 奇偶行的背景颜色不同，以便区分
            if (getIndex() % 2 == 0) {
              setStyle("-fx-background-color: #D6EAF8;"); // 例如，浅蓝色
            } else {
              setStyle("-fx-background-color: #E8F6F3;"); // 例如，浅绿色
            }
          } else {
            // 如果发送者是其他用户，设置另一种背景颜色
            // 奇偶行的背景颜色不同，以便区分
            if (getIndex() % 2 == 0) {
              setStyle("-fx-background-color: #F2F3F4;"); // 例如，浅灰色
            } else {
              setStyle("-fx-background-color: #F5EBEB;"); // 例如，浅粉色
            }
          }

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
                  alert.setContentText("An error occurred while saving the file: " 
                      + e.getMessage());
                  alert.showAndWait();
                }
              }
            }
          });
        }
      };
    }
  }


  public class ChatGroupCellFactory implements Callback<ListView<ChatGroup>, ListCell<ChatGroup>> {
    @Override
    public ListCell<ChatGroup> call(ListView<ChatGroup> param) {
      return new ListCell<ChatGroup>() {
        @Override
        protected void updateItem(ChatGroup chatGroup, boolean empty) {
          super.updateItem(chatGroup, empty);
          if (empty || chatGroup == null) {
            setText(null);
            if (getIndex() % 2 == 0) {
              setStyle("-fx-background-color: #F2F3F4;"); // 例如，浅灰色
            } else {
              setStyle("-fx-background-color: #E4D0D0;"); // 普通颜色，例如粉白色
            }
          } else {
            //用lambda表达式，若ChatName是类似[a, b, c]的形式
            if (chatGroup.getChatName().startsWith("[") && chatGroup.getChatName().endsWith("]")) {
              if (chatGroup.getGroupType().equals(GroupType.GROUP)) {
                if (chatGroup.getChatMembers().size() <= 3) {
                  setText(chatGroup.getChatName()
                      .substring(1, chatGroup.getChatName().length() - 1));
                } else {
                  setText(chatGroup.getChatName()
                      .substring(1, chatGroup.getChatName().length() - 1).split(", ")[0]
                      + ", " + chatGroup.getChatName()
                      .substring(1, chatGroup.getChatName().length() - 1).split(", ")[1]
                      + ", " + chatGroup.getChatName()
                      .substring(1, chatGroup.getChatName().length() - 1).split(", ")[2]
                      + ", ...");
                }
              } else if (chatGroup.getGroupType().equals(GroupType.PRIVATE)) {
                String otherUser = chatGroup.getChatMembers().get(0)
                    .equals(client.username)
                    ? chatGroup.getChatMembers().get(1) : chatGroup.getChatMembers().get(0);
                setText(otherUser);
              }
            } else {
              //否则直接显示ChatName(自定义)
              setText(chatGroup.getChatName());
            }
          }
          // 设置单元格的背景颜色
          if (isSelected()) {
            setStyle("-fx-background-color: #ADD8E6;"); // 选中颜色，例如浅蓝色
          } else if (chatGroup != null && chatGroup.isHasUnreadMessages()) {
            // 例如，粉红色
            setStyle("-fx-background-color: #D14D72;");
          } else {
            // 奇偶行的背景颜色不同，以便区分
            if (getIndex() % 2 == 0) {
              setStyle("-fx-background-color: #F2F3F4;"); // 例如，浅灰色
            } else {
              setStyle("-fx-background-color: #E4D0D0;"); // 普通颜色，例如粉白色
            }
          }
        }

        // 重写 updateSelected 方法，以便在单元格被选中或取消选中时更新背景颜色
        @Override
        public void updateSelected(boolean selected) {
          super.updateSelected(selected);
          // 设置单元格的背景颜色
          if (isSelected()) {
            setStyle("-fx-background-color: #ADD8E6;"); // 选中颜色，例如浅蓝色
          } else {
            // 奇偶行的背景颜色不同，以便区分
            if (getIndex() % 2 == 0) {
              setStyle("-fx-background-color: #F2F3F4;"); // 例如，浅灰色
            } else {
              setStyle("-fx-background-color: #E4D0D0;"); // 普通颜色，例如粉白色
            }
          }
        }
      };
    }
  }


  public void sendMessage(String messageContent, String sendTo) {
    try {
      Message message = new Message(System.currentTimeMillis(),
          client.username, sendTo, messageContent);
      client.sendMessage(message);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public void sendGroup(ChatGroup chatGroup) {
    try {
      Group group = new Group(chatGroup.getCreator(),
          chatGroup.getChatName(), chatGroup.getChatMembers(), chatGroup.getGroupType(),
          chatGroup.isHasUnreadMessages());
      //将chatGroup中的消息加进group中
      for (Message message : chatGroup.getMessages()) {
        group.addMessage(message);
      }
      client.sendGroup(group);
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


  private void showUsernameInputDialog() {
    Dialog<Pair<String, String>> dialog = new Dialog<>();
    dialog.setTitle("Login / Register");
    dialog.setHeaderText(null);

    ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
    ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes()
        .addAll(loginButtonType, registerButtonType, ButtonType.CANCEL);

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    TextField username = new TextField();
    username.setPromptText("Username");
    PasswordField password = new PasswordField();
    password.setPromptText("Password");
    Label errorMessage = new Label();
    errorMessage.setTextFill(Color.RED);

    grid.add(new Label("Username:"), 0, 0);
    grid.add(username, 1, 0);
    grid.add(new Label("Password:"), 0, 1);
    grid.add(password, 1, 1);
    grid.add(errorMessage, 1, 2);

    dialog.getDialogPane().setContent(grid);

    Platform.runLater(() -> username.requestFocus());

    AtomicReference<ButtonType> clickedButtonType = new AtomicReference<>();

    dialog.setResultConverter(dialogButton -> {
      clickedButtonType.set(dialogButton);
      if (dialogButton == loginButtonType) {
        return new Pair<>(username.getText(), password.getText());
      } else if (dialogButton == registerButtonType) {
        return new Pair<>(username.getText(), password.getText());
      }
      return null;
    });

    Optional<Pair<String, String>> input = dialog.showAndWait();

    if (!input.isPresent()) {
      System.out.println("Dialog closed unexpectedly, exiting");
      Platform.exit();
      System.exit(0);
      return;
    }

    sendMessage("AllClientNames", "Server");

    lock.lock();

    try {
      namesUpdated.await();
      while (Arrays.asList(allClientNames).contains(input.get().getKey())) {
        errorMessage.setText("Username already exists, please choose another one.");
        input = dialog.showAndWait();
        if (!input.isPresent() || input.get().getKey().isEmpty()) {
          System.out.println("Invalid username " + input + ", exiting");
          Platform.exit();
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lock.unlock();
    }

    // Send username and password to the server for login or registration
    String operation = clickedButtonType.get() == loginButtonType ? "login" : "register";

    String userAndPassword = operation + ":" + input.get().getKey() + ":" + input.get().getValue();
    sendMessage(userAndPassword, "Server");

    lock.lock();
    try {
      loginResult.await();
      if (!loginSuccess) {
        errorMessage.setText("Invalid username or password. Please try again.");
        showUsernameInputDialog();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lock.unlock();
    }

    client.username = input.get().getKey();

    updateCurrentUsername(client.username);

    sendMessage("clientName:" + input.get().getKey(), "Server");
  }


  public List<String> getFilteredUserList() {
    sendMessage("AllClientNames", "Server");

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

  public void onReceiveMessage(Message message) {
    Platform.runLater(() -> {
      ChatGroup activeChat = chatList.getSelectionModel().getSelectedItem();
      if (activeChat == null) {
        return;
      }
      if (activeChat.getChatName().equals(message.getSendTo())) {
        chatContentList.getItems().setAll(activeChat.getMessages());
      }
      updateGroupOrder();
    });
  }

  private Image convertByteArrayToImage(byte[] imageData) {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
    return new Image(inputStream);
  }

  public void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  public void stop() {
    client.stop();
  }


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

  public void updateGroupOrder() {

    // Sort the list based on the timestamps of the last messages.
    FXCollections.sort(chatList.getItems());

    // Refresh the ListView to display the updated order.
    chatList.refresh();
  }

  private void initEmojiPopup() {
    emojiPopup = new Popup();

    FlowPane emojiPane = new FlowPane();
    emojiPane.setHgap(5);
    emojiPane.setVgap(5);
    emojiPane.setPadding(new Insets(5, 5, 5, 5));

    // 示例：添加一些emoji
    String[] emojis = {"😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇"};
    for (String emoji : emojis) {
      Button emojiButton = new Button(emoji);
      emojiButton.setStyle("-fx-background-color: transparent;");
      emojiButton.setOnAction(event -> {
        inputArea.appendText(emoji);
        emojiPopup.hide();
      });
      emojiPane.getChildren().add(emojiButton);
    }

    emojiPopup.getContent().add(emojiPane);
  }

  public void addNewChat(ChatGroup chatGroup) {
    Platform.runLater(() -> {
      chatList.getItems().add(chatGroup);
      if (chatList.getSelectionModel().getSelectedItem() == null) {
        chatList.getSelectionModel().select(chatGroup);
        chatContentList.getItems().setAll(chatGroup.getMessages());
      }
    });
  }

  public void addNewMessage(Message message) {
    Platform.runLater(() -> {
      chatList.getItems().forEach(chatGroup -> {
        if (chatGroup.getChatName().equals(message.getSendTo())) {
          chatGroup.addMessage(message);
          chatGroup.setHasUnreadMessages(true);
          onReceiveMessage(message);
        }
      });
    });
  }

  public void updateCurrentUsername(String username) {
    Platform.runLater(() -> {
      currentUsername.setText("User: " + username);
    });
  }

  public void updateCurrentOnlineCnt(String cnt) {
    Platform.runLater(() -> {
      currentOnlineCnt.setText("Online: " + cnt);
    });
  }

  public Lock getLock() {
    return lock;
  }

  public Condition getLoginResult() {
    return loginResult;
  }

  public void setLoginSuccess(boolean loginSuccess) {
    this.loginSuccess = loginSuccess;
  }

}
