package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Represents a chat client.
 */
public class ChatClient implements Runnable {
  private Socket socket;
  private Controller controller;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  private InetSocketAddress inetSocketAddress;
  public String username;

  /**
   * Constructs a new ChatClient using the specified Controller.
   *
   * @param controller the Controller to be used for managing chat-related actions
   * @throws IOException if an I/O error occurs when creating the socket
   */
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
            System.out.println("Failed to connect to " 
                + ipAddress + ", will try the next IP address.");
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

  /**
   * Constructs a new ChatClient using the specified host, port, and Controller.
   *
   * @param host       the hostname to connect to
   * @param port       the port number to connect to
   * @param controller the Controller to be used for managing chat-related actions
   * @throws IOException if an I/O error occurs when creating the socket
   */
  public ChatClient(String host, int port, Controller controller) throws IOException {
    this.inetSocketAddress = new InetSocketAddress(host, port);
    this.controller = controller;
    socket = new Socket(host, port);
  }

  /**
   * The main loop of the ChatClient, listening for incoming messages and groups.
   */
  @Override
  public void run() {
    try {
      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());

      while (true) {
        Object receivedObject = in.readObject();
        if (receivedObject instanceof Message) {
          Message message = (Message) receivedObject;
          handleServerMessage(message);
        } else if (receivedObject instanceof Group) {
          Group group = (Group) receivedObject;
          handleServerGroup(group);
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

  /**
   * Sends a message to the server.
   *
   * @param message the message to be sent
   * @throws IOException if an I/O error occurs during writing
   */
  public void sendMessage(Message message) throws IOException {
    out.writeObject(message);
  }

  /**
   * Sends a group to the server.
   *
   * @param group the group to be sent
   * @throws IOException if an I/O error occurs during writing
   */
  public void sendGroup(Group group) throws IOException {
    out.writeObject(group);
  }

  /**
   * Handles messages received from the server.
   *
   * @param message the received message
   * @throws IOException if an I/O error occurs during handling
   */
  public void handleServerMessage(Message message) throws IOException {
    if (message.getSentBy().equals("Server")) {
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
        controller.updateClientList(message.getData()
            .substring("allClientsNames:".length()).split(" "));
        return;
      }
      if (message.getData().equals("Server is shutting down")) {
        controller.onServerShutdown();
        return;
      }
      if (message.getData().startsWith("ClientCount:")){
        controller.updateCurrentOnlineCnt(message.getData()
            .substring("ClientCount:".length()));
      }
    }
    controller.addNewMessage(message);
  }

  /**
   * Handles groups received from the server.
   *
   * @param group the received group
   * @throws IOException if an I/O error occurs during handling
   */
  public void handleServerGroup(Group group) throws IOException {
    // 将group添加到controller中的ListView<ChatGroup> chatList中
    controller.addNewChat(new ChatGroup(group));
  }

  /**
   * Stops the ChatClient, closing all resources.
   */
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
