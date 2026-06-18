package com.example.operatingsystempraticalclass;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookmarkDao {


    @Query("SELECT * FROM bookmarks ORDER BY postId ASC")
    List<BookMarksEntity> getAllBookmarks();




    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookMarksEntity bookMarks);

    @Delete
    void delete(BookMarksEntity bookMarks);

    @Query("DELETE FROM bookmarks WHERE postId = :postId")
    void deleteByPostId(int postId);


    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE postId=:postId)")
    boolean isBookmarked(int postId);
}
