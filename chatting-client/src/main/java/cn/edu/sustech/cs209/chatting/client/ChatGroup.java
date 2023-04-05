package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.GroupType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class ChatGroup implements Comparable<ChatGroup> {

    private String chatName;
    private String creator;
    private GroupType groupType;
    private List<String> chatMembers;
    private ObservableList<Message> messages;


    public ChatGroup(String creator, String chatName, List<String> chatMembers, GroupType groupType) {
        this.creator = creator;
        this.chatName = chatName;
        this.chatMembers = chatMembers;
        this.messages = FXCollections.observableArrayList();
        this.groupType = groupType;
    }

    public ChatGroup(Group group) {
        this.creator = group.getCreator();
        this.chatName = group.getGroupName();
        this.chatMembers = group.getGroupMembers();
        this.messages = FXCollections.observableArrayList(group.getGroupMessages());
        this.groupType = group.getGroupType();
    }

    @Override
    public int compareTo(ChatGroup other) {
        // Assuming you have a method called getLastMessageTimestamp() that returns the timestamp of the last message in the group.
        return Long.compare(other.getLastMessageTimestamp(), this.getLastMessageTimestamp());
    }

    public long getLastMessageTimestamp() {
        // Assuming you have a list of messages called messages.
        if (messages.isEmpty()) {
            return 0;
        }
        return messages.get(messages.size() - 1).getTimestamp();
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

    public GroupType getGroupType() {
        return groupType;
    }

    public void setGroupType(GroupType groupType) {
        this.groupType = groupType;
    }

    @Override
    public String toString() {
        return "Group{" + "chatName='" + chatName + '\'' + ", creator='" + creator + '\'' + ", groupType=" + groupType + ", chatMembers=" + chatMembers + ", messages=" + messages + '}';
    }
}

