package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {
    private String chatName;
    private List<String> chatMembers;
    private List<Message> messages;

    public Group(List<String> chatMembers) {
        this.chatMembers = chatMembers;
        this.messages = new ArrayList<>();
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

    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "Group{" + "chatMembers=" + chatMembers + ", messages=" + messages + '}';
    }
}
