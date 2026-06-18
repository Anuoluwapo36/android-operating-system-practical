package com.example.operatingsystempraticalclass;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PostEntity.class, BookMarksEntity.class}, version = 2,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {


    public abstract PostDao postDao();
    public  abstract  BookmarkDao bookmarkDao();

    public static volatile  AppDatabase INSTANCE;


    public static AppDatabase getInstance(Context context){
        if (INSTANCE == null){
            synchronized (AppDatabase.class){
                INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                AppDatabase.class, "post_db")
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
        return INSTANCE;
    }
}
