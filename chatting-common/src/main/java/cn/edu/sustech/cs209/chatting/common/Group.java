package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {
    private String groupName;
    private String creator;
    private GroupType groupType;
    private List<String> groupMembers;
    private List<Message> groupMessages;

    public Group(String creator, String groupName, List<String> groupMembers, GroupType groupType) {
        this.creator = creator;
        this.groupName = groupName;
        this.groupMembers = groupMembers;
        this.groupMessages = new ArrayList<>();
        this.groupType = groupType; // 设置groupType
    }

    public List<String> getGroupMembers() {
        return groupMembers;
    }

    public void setGroupMembers(List<String> chatMembers) {
        this.groupMembers = chatMembers;
    }

    public void addMessage(Message message) {
        groupMessages.add(message);
    }

    public List<Message> getGroupMessages() {
        return groupMessages;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String chatName) {
        this.groupName = chatName;
    }

    public void setGroupMessages(List<Message> messages) {
        this.groupMessages = messages;
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
        return "Group{" + "groupName=" + groupName + ", creator=" + creator + ", groupType=" + groupType + ", groupMembers=" + groupMembers + ", groupMessages=" + groupMessages + '}';
    }
}
