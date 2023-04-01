package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

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
                Message message = (Message) in.readObject();

                HandleClientMessage(message);
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

    public void HandleClientMessage(Message message) throws IOException {
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
    }

}
