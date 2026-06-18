package com.example.operatingsystempraticalclass;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookMarksActivity extends AppCompatActivity {

    private RecyclerView rvBookMark;
    private TextView tvEmptyBookmarks;
    private PostAdapter adapter;
    private List<Post> booKList = new ArrayList<>();
    private AppDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        setTitle("Bookmarks");

        rvBookMark = findViewById(R.id.rvBookmarks);
        tvEmptyBookmarks = findViewById(R.id.tvEmptyBookmarks);

        rvBookMark.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(booKList);
        rvBookMark.setAdapter(adapter);
        db = AppDatabase.getInstance(this);

        adapter.setListener(post -> {
            Intent intent = new Intent(BookMarksActivity.this, DetailsActivity.class);
            intent.putExtra(DetailsActivity.EXTRA_POST, post);
            startActivity(intent);
        });

        loadBookMarks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookMarks();
    }

    private void loadBookMarks() {
        executor.execute(() -> {
            List<BookMarksEntity> entities = db.bookmarkDao().getAllBookmarks();
            List<Post> posts = new ArrayList<>();
            Set<Integer> bookmarkIds = new HashSet<>();

            for (BookMarksEntity entity : entities) {
                posts.add(new Post(entity.getPostId(), entity.getTitle(), entity.getBody()));
                bookmarkIds.add(entity.getPostId());
            }

            runOnUiThread(() -> {
                booKList.clear();
                booKList.addAll(posts);
                adapter.setBookmarkedPostIds(bookmarkIds);
                adapter.syncDisplayList();
                updateEmptyState(posts.isEmpty());
            });
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        tvEmptyBookmarks.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvBookMark.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
