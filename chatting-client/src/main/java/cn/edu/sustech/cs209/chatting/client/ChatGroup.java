package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class ChatGroup {

    private String chatName;
    private String creator;
    private List<String> chatMembers;
    private ObservableList<Message> messages;

    public ChatGroup(String creator, String chatName, List<String> chatMembers) {
        this.creator = creator;
        this.chatName = chatName;
        this.chatMembers = chatMembers;
        this.messages = FXCollections.observableArrayList();
    }

    public ChatGroup(Group group) {
        this.creator = group.getCreator();
        this.chatName = group.getGroupName();
        this.chatMembers = group.getGroupMembers();
        this.messages = FXCollections.observableArrayList(group.getGroupMessages());
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

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public void setMessages(ObservableList<Message> messages) {
        this.messages = messages;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}

