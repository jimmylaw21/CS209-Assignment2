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
  private boolean hasUnreadMessages;

  public Group(String creator, String groupName, List<String> groupMembers,
               GroupType groupType, boolean hasUnreadMessages) {
    this.creator = creator;
    this.groupName = groupName;
    this.groupMembers = groupMembers;
    this.groupMessages = new ArrayList<>();
    this.groupType = groupType; // 设置groupType
    this.hasUnreadMessages = hasUnreadMessages;
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

  public boolean isHasUnreadMessages() {
    return hasUnreadMessages;
  }

  public void setHasUnreadMessages(boolean hasUnreadMessages) {
    this.hasUnreadMessages = hasUnreadMessages;
  }


  @Override
  public String toString() {
    return "Group{" + "groupName=" + groupName + ", creator=" + creator
        + ", groupType=" + groupType + ", groupMembers=" + groupMembers
        + ", groupMessages=" + groupMessages + '}';
  }
}
