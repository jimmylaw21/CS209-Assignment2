package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.Message;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;

public class ClientHandler implements Runnable {
    private ChatServer server;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String clientName;

    public ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());

            while (true) {
                // 分辨客户端发送的object是什么类型
                Object receivedObject = in.readObject();
                if (receivedObject instanceof Message) {
                    Message message = (Message) receivedObject;
                    if (message.getFile() != null) {
                        // This is a file message, handle it accordingly.
                        HandleMessageWithFile(message);
                    }else{
                        HandleClientMessage(message);
                    }
                }else if (receivedObject instanceof Group) {
                    Group group = (Group) receivedObject;
                    HandleClientGroup(group);
                }
            }
        } catch (SocketException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress().getHostAddress());
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress().getHostAddress());
        }catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server.removeClient(this);
        }
    }

    public void sendMessageToClient(Message message) throws IOException {
        out.writeObject(message);
    }

    public void sendGroupToClient(Group group) throws IOException {
        out.writeObject(group);
    }

    public void HandleClientMessage(Message message) throws IOException {
        if (message.getSendTo().equals("Server")) {
            // 如果客户端发送了“clientName:”， 则服务器端将客户端的名字设置为发送的名字
            if (message.getData().startsWith("clientName:")) {
                clientName = message.getData().substring("clientName:".length());
                return;
            }
            // 如果客户端发送了“AllClientNames”， 则服务器端返回所有客户端的名字
            if (message.getData().equals("AllClientNames")) {
                StringBuilder allClientNames = new StringBuilder();
                for (ClientHandler clientHandler : server.getClients()) {
                    allClientNames.append(clientHandler.clientName).append(" ");
                }
                Message message1 = new Message(System.currentTimeMillis(), "Server", message.getSentBy(), "allClientsNames:"+allClientNames.toString());
                sendMessageToClient(message1);
                return;
            }
            System.out.println("At "+message.getTimestamp()+", Received message from " + message.getSentBy() + " to " + message.getSendTo() + ": " + message.getData());
            return;
        }
        // 使用stream获取server中的群组，其名字与message中的sendTo相同
        Optional<Group> group = server.getGroups().stream().filter(g -> g.getGroupName().equals(message.getSendTo())).findFirst();
        // 如果群组名字存在，则将消息发送给群组中的所有客户端
        if (group.isPresent()) {
            for (ClientHandler clientHandler : server.getClients()) {
                if (clientHandler.clientName.equals(message.getSentBy())) {
                    continue;
                }
                if (group.get().getGroupMembers().contains(clientHandler.clientName)) {
                    clientHandler.sendMessageToClient(message);
                }
                // Message也要保留在server这边的group中
                group.get().addMessage(message);
            }
            return;
        }
        // 如果群组名字不存在，则将消息发送给指定的客户端
        for (ClientHandler clientHandler : server.getClients()) {
            if (clientHandler.clientName.equals(message.getSentBy())) {
                continue;
            }
            if (clientHandler.clientName.equals(message.getSendTo())) {
                clientHandler.sendMessageToClient(message);
            }
        }
    }

    public void HandleClientGroup(Group group) throws IOException {
        server.getGroups().add(group);
        // 将收到的群组发送给群组包含的所有客户端
        for (ClientHandler clientHandler : server.getClients()) {
            if (clientHandler.clientName.equals(group.getCreator())) {
                continue;
            }
            if (group.getGroupMembers().contains(clientHandler.clientName)) {
                clientHandler.sendGroupToClient(group);
            }
        }
    }

    public void HandleMessageWithFile(Message message) throws IOException {
        // TODO 检查文件类型、大小

        HandleClientMessage(message);
    }

    public String getClientName() {
        return clientName;
    }
}
