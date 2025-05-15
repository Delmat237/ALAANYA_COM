package model;

import javafx.scene.image.Image;

public class Contact {
    private String phone_number;
    private String name;
    private String lastMessage;
    private String date;
    private int unreadCount;
    private Image profileImage;

    public Contact(String phone_number,String name,String lastMessage, String date, int unreadCount, Image profileImage) {
        this.phone_number = phone_number;
        this.name = name;
        this.lastMessage = lastMessage;
        this.date = date;
        this.unreadCount = unreadCount;
        this.profileImage = profileImage;
    }

    // Getters...
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getDate() { return date; }
    public int getUnreadCount() { return unreadCount; }
    public Image getProfileImage() { return profileImage; }

    public String getPhone_number() {return phone_number;}

    public void setPhone_number(String phone_number) {this.phone_number = phone_number;}

    @Override
    public String toString() {
        return "Contact : Username " + getName() + ", Phone number " + getPhone_number()+"last message "+getLastMessage();
    }
}
