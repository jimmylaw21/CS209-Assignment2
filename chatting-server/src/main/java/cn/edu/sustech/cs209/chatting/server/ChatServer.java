package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
  private int port;
  private ServerSocket serverSocket;
  private ExecutorService executorService;
  private Set<ClientHandler> clients;
  private Set<Group> groups;
  private Map<String, String> usersCredentials;

  public ChatServer(int port) {
    this.port = port;
    this.clients = Collections.synchronizedSet(new HashSet<>());
    this.groups = Collections.synchronizedSet(new HashSet<>());
    this.usersCredentials = Collections.synchronizedMap(new HashMap<>());
  }

  public void startServer() {
    try {
      //Server started on host: 10.25.0.92, port: 8888
      serverSocket = new ServerSocket();
      serverSocketSetting();

      System.out.println("Server started on host: "
          + serverSocket.getInetAddress().getHostAddress()
          + ", port: " + serverSocket.getLocalPort());

      executorService = Executors.newCachedThreadPool();

      readGroupInfo();

      readUsersCredentials();

      while (true) {
        Socket socket = serverSocket.accept();

        System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());

        ClientHandler clientHandler = new ClientHandler(this, socket);
        clients.add(clientHandler);
        executorService.execute(clientHandler);
      }
    } catch (SocketTimeoutException e) {
      System.out.println("Server timeout");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        for (ClientHandler client : clients) {
          removeClient(client);
        }
        executorService.shutdown();
        serverSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void removeClient(ClientHandler clientHandler) {
    clients.remove(clientHandler);
  }

  public void serverSocketSetting() throws IOException {
    serverSocket.bind(new InetSocketAddress("10.25.0.92", port),
        10); //服务器端绑定本地的 IP 地址和端口号
    serverSocket.setReuseAddress(true); // 设置端口复用
    serverSocket.setReceiveBufferSize(64 * 1024 * 1024); // 设置接收缓冲区为 64M
    serverSocket.setSoTimeout(300000); // 服务器端设置超时时间为 300 秒
    serverSocket.setPerformancePreferences(10, 10, 1);
  }

  public void saveGroupInfo() {
    try {
      // 保存群聊信息到本地resource文件夹中的group.txt文件中
      String filePath = "chatting-server/src/main/resources/groups.txt";
      File file = new File(filePath);
      if (!file.exists()) {
        file.createNewFile();
      }

      try (FileOutputStream fos = new FileOutputStream(file);
           ObjectOutputStream oos = new ObjectOutputStream(fos)) {
        oos.writeObject(groups);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void readGroupInfo() {
    String filePath = "chatting-server/src/main/resources/groups.txt";
    File file = new File(filePath);
    if (file.exists() && file.length() > 0) {
      try (FileInputStream fis = new FileInputStream(file);
           ObjectInputStream ois = new ObjectInputStream(fis)) {
        // 读取群组信息
        groups = (Set<Group>) ois.readObject();
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  public void saveUsersCredentials() throws IOException {
    String filePath = "chatting-server/src/main/resources/usersCredentials.txt";
    File file = new File(filePath);
    if (!file.exists()) {
      file.createNewFile();
    }
    try (FileOutputStream fos = new FileOutputStream(file);
         ObjectOutputStream oos = new ObjectOutputStream(fos)) {
      oos.writeObject(usersCredentials);
    }
  }

  public void readUsersCredentials() throws IOException {
    String filePath = "chatting-server/src/main/resources/usersCredentials.txt";
    File file = new File(filePath);
    if (file.exists() && file.length() > 0) {
      try (FileInputStream fis = new FileInputStream(file);
           ObjectInputStream ois = new ObjectInputStream(fis)) {
        usersCredentials = (Map<String, String>) ois.readObject();
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  public void registerUser(String username, String password) throws IOException {
    usersCredentials.put(username, password);
    saveUsersCredentials();
    System.out.println("User " + username + " registered");
  }

  public boolean validateUser(String username, String password) {
    String storedPassword = usersCredentials.get(username);
    return storedPassword != null && storedPassword.equals(password);
  }

  public Set<ClientHandler> getClients() {
    return clients;
  }

  public Set<Group> getGroups() {
    return groups;
  }

  public void updateClientsCnt() {
    synchronized (clients) {
      for (ClientHandler client : clients) {
        try {
          if (isClientConnected(client)) {
            client.sendClientCount(clients.size());
          } else {
            clients.remove(client);
          }
        } catch (IOException e) {
          System.err.println("Error sending client count: " + e.getMessage());
          clients.remove(client);
        }
      }
    }
  }

  private boolean isClientConnected(ClientHandler client) {
    try {
      return client.getSocket().getInputStream().available() >= 0;
    } catch (SocketException e) {
      return false;
    } catch (IOException e) {
      System.err.println("Error checking client connection: " + e.getMessage());
      return false;
    }
  }

}
