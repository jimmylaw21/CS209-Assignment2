package cn.edu.sustech.cs209.chatting.common;

import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Message implements Serializable {
    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;

    private String fileName;

    private byte[] file;

    public Message(Long timestamp, String sentBy, String sendTo, String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Message{" + "timestamp=" + timestamp + ", sentBy=" + sentBy + ", sendTo=" + sendTo + ", data=" + data + '}';
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
