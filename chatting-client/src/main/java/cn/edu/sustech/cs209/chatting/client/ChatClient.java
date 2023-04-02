package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ChatClient implements Runnable{
    private Socket socket;
    private Controller controller;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String host;
    private int port;
    public String username;

    public ChatClient(String host, int port, Controller controller) throws IOException {
        this.host = host;
        this.port = port;
        this.controller = controller;
        socket = new Socket(host, port);
    }
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Object receivedObject = in.readObject();
                if (receivedObject instanceof Message) {
                    Message message = (Message) receivedObject;
                    onReceiveMessage(message);
                }else if (receivedObject instanceof Group) {
                    Group group = (Group) receivedObject;
                    HandleServerGroup(group);
                }
            }
        } catch (SocketException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress().getHostAddress());
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
                controller.onServerShutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //send message to the server
    public void sendMessage(Message message) throws IOException {
        out.writeObject(message);
    }

    public void sendGroup(Group group) throws IOException {
        out.writeObject(group);
    }

    public void onReceiveMessage(Message message) throws IOException {
        if (message.getFile() == null) {
            HandleServerMessage(message);
        } else {
            HandleFileMessage(message);
        }
    }

    public void HandleServerMessage(Message message) throws IOException {
        if(message.getSentBy().equals("Server")) {
            // 如果客户端发送了“clientName:”， 则服务器端将客户端的名字设置为发送的名字
            if (message.getData().startsWith("allClientsNames:")) {
                controller.updateClientList(
                        message.getData().substring("allClientsNames:".length()).split(" "));
                return;
            }
            if (message.getData().equals("Server is shutting down")) {
                controller.onServerShutdown();
                return;
            }
        }
        // 使用stream获取controller中ListView<ChatGroup> chatList里的群组，其名字与message中的sendTo相同
        Optional<ChatGroup> chatGroup = controller.chatList.getItems().stream()
                .filter(group -> group.getChatName().equals(message.getSendTo()))
                .findFirst();
        // 如果找到了群组，则将message添加到群组的聊天记录中
        if (chatGroup.isPresent()) {
            chatGroup.get().addMessage(message);
            controller.onReceiveMessage(message);
        }

    }

    public void HandleFileMessage(Message message) {
        // 使用stream获取controller中ListView<ChatGroup> chatList里的群组，其名字与message中的sendTo相同
        Optional<ChatGroup> chatGroup = controller.chatList.getItems().stream()
                .filter(group -> group.getChatName().equals(message.getSendTo()))
                .findFirst();
        // 如果找到了群组，则将message添加到群组的聊天记录中
        if (chatGroup.isPresent()) {

            chatGroup.get().addMessage(message);
            controller.onReceiveMessage(message);

//            byte[] fileContent = message.getFile();
//            String fileName = message.getFileName();
//
//            // 将文件保存到磁盘上
//            try {
//                Path outputPath = Paths.get("received_files", fileName);
//                Files.createDirectories(outputPath.getParent());
//                Files.write(outputPath, fileContent);
//                System.out.println("File saved to: " + outputPath.toString());
//            } catch (IOException e) {
//                e.printStackTrace();
//                // 处理异常，记录错误日志，或采取其他适当的措施
//            }
        }
    }

    public void HandleServerGroup(Group group) throws IOException {
        // 将group添加到controller中的ListView<ChatGroup> chatList中
        controller.chatList.getItems().add(new ChatGroup(group));
    }

    public void stop() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
