package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;

public class ChatClient implements Runnable{
    private Socket socket;
    private Controller controller;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private InetSocketAddress inetSocketAddress;
    public String username;

    public ChatClient(Controller controller) throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 8888);
        String[] ipAddresses = {"10.25.0.92", "10.12.97.44"};
        int port = 8888;
        int retryCount = 3;
        int connectTimeout = 1000;

        Socket socket = null;
        for (int i = 0; i < retryCount; i++) {
            for (String ipAddress : ipAddresses) {
                try {
                    socket = new Socket();
                    inetSocketAddress = new InetSocketAddress(ipAddress, port);
                    socket.connect(inetSocketAddress, connectTimeout);

                    // 连接成功
                    break;
                } catch (SocketException e) {
                    if (i == retryCount - 1) {
                        // 尝试次数已用完，不再重试
                        System.out.println("Failed to connect to the server.");
                        return;
                    } else {
                        // 尝试使用下一个IP地址
                        System.out.println("Failed to connect to " + ipAddress + ", will try the next IP address.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                // 连接成功，跳出循环
                break;
            } else {
                // 等待一段时间后再进行重试
                try {
                    Thread.sleep(connectTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        this.inetSocketAddress = inetSocketAddress;
        this.controller = controller;
    }

    public ChatClient(String host, int port, Controller controller) throws IOException {
        this.inetSocketAddress = new InetSocketAddress(host, port);
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
                    HandleServerMessage(message);
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


    public void sendMessage(Message message) throws IOException {
        out.writeObject(message);
    }

    public void sendGroup(Group group) throws IOException {
        out.writeObject(group);
    }

    public void HandleServerMessage(Message message) throws IOException {
        if(message.getSentBy().equals("Server")) {
            if (message.getData().startsWith("LoginResult")) {
                controller.getLock().lock();
                try {
                    controller.setLoginSuccess(message.getData().split(":")[1].equals("Success"));
                    controller.getLoginResult().signal();
                } finally {
                    controller.getLock().unlock();
                }
                return;
            }
            if (message.getData().startsWith("allClientsNames")) {
                controller.updateClientList(
                        message.getData().substring("allClientsNames:".length()).split(" "));
                return;
            }
            if (message.getData().equals("Server is shutting down")) {
                controller.onServerShutdown();
                return;
            }
        }
//        // 使用stream获取controller中ListView<ChatGroup> chatList里的群组，其名字与message中的sendTo相同
//        Optional<ChatGroup> chatGroup = controller.chatList.getItems().stream()
//                .filter(group -> group.getChatName().equals(message.getSendTo()))
//                .findFirst();
//        // 如果找到了群组，则将message添加到群组的聊天记录中
//        if (chatGroup.isPresent()) {
//            chatGroup.get().addMessage(message);
//            controller.onReceiveMessage(message);
//        }
        controller.addNewMessage(message);

    }

    public void HandleServerGroup(Group group) throws IOException {
        // 将group添加到controller中的ListView<ChatGroup> chatList
//        controller.chatList.getItems().add(new ChatGroup(group));
        controller.addNewChat(new ChatGroup(group));
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
