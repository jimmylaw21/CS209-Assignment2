package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Group;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private Set<ClientHandler> clients;
    private Set<Group> groups;

    public ChatServer(int port) {
        this.port = port;
        this.clients = Collections.synchronizedSet(new HashSet<>());
        this.groups = Collections.synchronizedSet(new HashSet<>());
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket();
            serverSocketSetting();

            System.out.println("Server started on host: " + serverSocket.getInetAddress().getHostAddress() + ", port: " + serverSocket.getLocalPort());

            executorService = Executors.newCachedThreadPool();

            while (true) {
                Socket socket = serverSocket.accept();

                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());

                ClientHandler clientHandler;
                       clientHandler = new ClientHandler(this, socket);
                clients.add(clientHandler);
                executorService.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
//                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public void serverSocketSetting() throws IOException {
        serverSocket.bind(new InetSocketAddress(Inet4Address.getLocalHost(), port), 10); //服务器端绑定本地的 IP 地址和端口号
        serverSocket.setReuseAddress(true); // 设置端口复用
        serverSocket.setReceiveBufferSize(64 * 1024 * 1024); // 设置接收缓冲区为 64M
            serverSocket.setSoTimeout(300000); // 服务器端设置超时时间为 300 秒
        serverSocket.setPerformancePreferences(10, 10, 1);
    }

    // 保存群聊信息到本地group.json文件中
    public void saveGroupInfo() throws FileNotFoundException {
        String groupInfo = "";
        for (Group group : groups) {
            groupInfo += group.toString();
        }
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(getClass().getResource("/group.json").getPath()));
        try {
            bufferedOutputStream.write(groupInfo.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 读取本地group.json文件中的群聊信息
    public void readGroupInfo() {

    }

    public Set<ClientHandler> getClients() {
        return clients;
    }
}
