package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

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
                Message message = (Message) in.readObject();

                HandleServerMessage(message);
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //send message to the server
    public void sendMessage(Message message) throws IOException {
        out.writeObject(message);
    }

    public void HandleServerMessage(Message message) throws IOException {
        // 如果客户端发送了“clientName:”， 则服务器端将客户端的名字设置为发送的名字
        if (message.getData().startsWith("allClientsNames:")) {
            controller.updateClientList(
                    message.getData().substring("allClientsNames:".length()).split(" "));
            return;
        }

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
