package com.example.operatingsystempraticalclass;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PostDao {

    @Query("SELECT * FROM posts  ORDER BY id  ASC ")
    List<PostEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PostEntity> posts);


    @Query("DELETE FROM posts")
    void  clearAll();



}
