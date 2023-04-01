package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class ChatGroup {

    private String chatName;
    private List<String> chatMembers;
    private ObservableList<Message> messages;

    public ChatGroup(String chatName,List<String> chatMembers) {
        this.chatName = chatName;
        this.chatMembers = chatMembers;
        this.messages = FXCollections.observableArrayList();
    }

    public List<String> getChatMembers() {
        return chatMembers;
    }

    public void setChatMembers(List<String> chatMembers) {
        this.chatMembers = chatMembers;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public ObservableList<Message> getMessages() {
        return messages;
    }
}

