package com.example.operatingsystempraticalclass;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks")
public class BookMarksEntity {
    @PrimaryKey
    private int postId;
    private String title;
    private String body;

    public BookMarksEntity(int postId, String title, String body) {
        this.postId = postId;
        this.title = title;
        this.body = body;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
