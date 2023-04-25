package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Group;
import cn.edu.sustech.cs209.chatting.common.GroupType;
import cn.edu.sustech.cs209.chatting.common.Message;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Represents a chat group.
 */
public class ChatGroup implements Comparable<ChatGroup> {

  private String chatName;
  private String creator;
  private GroupType groupType;
  private List<String> chatMembers;
  private ObservableList<Message> messages;
  private boolean hasUnreadMessages;

  /**
   * Constructs a new ChatGroup with the specified creator, chat name, chat members, and group type.
   *
   * @param creator     the creator of the chat group
   * @param chatName    the name of the chat group
   * @param chatMembers the list of chat members
   * @param groupType   the type of the chat group
   */
  public ChatGroup(String creator, String chatName, List<String> chatMembers, GroupType groupType) {
    this.creator = creator;
    this.chatName = chatName;
    this.chatMembers = chatMembers;
    this.messages = FXCollections.observableArrayList();
    this.groupType = groupType;
    this.hasUnreadMessages = false;
  }

  /**
   * Constructs a new ChatGroup based on the provided Group object.
   *
   * @param group the Group object to base the ChatGroup on
   */
  public ChatGroup(Group group) {
    this.creator = group.getCreator();
    this.chatName = group.getGroupName();
    this.chatMembers = group.getGroupMembers();
    this.messages = FXCollections.observableArrayList(group.getGroupMessages());
    this.groupType = group.getGroupType();
    this.hasUnreadMessages = group.isHasUnreadMessages();
  }

  /**
   * Compares this ChatGroup to another ChatGroup based on the timestamp of their last messages.
   *
   * @param other the ChatGroup to compare to
   * @return a negative integer, zero, or a positive integer as this ChatGroup's last message
   * timestamp is less than, equal to, or greater than the other ChatGroup's last message timestamp
   */
  @Override
  public int compareTo(ChatGroup other) {
    return Long.compare(other.getLastMessageTimestamp(), this.getLastMessageTimestamp());
  }

  /**
   * Gets the timestamp of the last message in this ChatGroup.
   *
   * @return the timestamp of the last message, or 0 if there are no messages in the ChatGroup
   */
  public long getLastMessageTimestamp() {
    // Assuming you have a list of messages called messages.
    if (messages.isEmpty()) {
      return 0;
    }
    return messages.get(messages.size() - 1).getTimestamp();
  }

  /**
   * Adds a message to this ChatGroup's list of messages.
   *
   * @param message the message to add
   */
  public void addMessage(Message message) {
    messages.add(message);
  }

  // Getter and setter methods for chatMembers, chatName, creator, groupType, and messages
  public List<String> getChatMembers() {
    return chatMembers;
  }

  public void setChatMembers(List<String> chatMembers) {
    this.chatMembers = chatMembers;
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

  public boolean isHasUnreadMessages() {
    return hasUnreadMessages;
  }

  public void setHasUnreadMessages(boolean hasUnreadMessages) {
    this.hasUnreadMessages = hasUnreadMessages;
  }

  /**
   * Returns a string representation of this ChatGroup,
   * including the chat name, creator, group type, chat members, and messages.
   *
   * @return a string representation of the ChatGroup
   */
  @Override
  public String toString() {
    return "Group{"
        + "chatName='" + chatName + '\''
        + ", creator='" + creator + '\''
        + ", groupType=" + groupType
        + ", chatMembers=" + chatMembers
        + ", messages=" + messages + '}';
  }
}

