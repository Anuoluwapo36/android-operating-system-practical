package com.example.operatingsystempraticalclass;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private PostAdapter postAdapter;
    private RecyclerView recyclerView;
    private List<Post> postList = new ArrayList<>();
    private TextView itemCount, lastUpdateView,cacheTime;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayoutManager layoutManager;
    private  AppDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private SearchView searchView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        init();
    }

    private void init() {
        recyclerView = findViewById(R.id.rvPost);
        itemCount = findViewById(R.id.itemCount);
        lastUpdateView = findViewById(R.id.tvLastUpdate);
        progressBar= findViewById(R.id.progressBar);
        cacheTime= findViewById(R.id.tvCacheTime);
        searchView = findViewById(R.id.searchView);
        layoutManager = new LinearLayoutManager(this);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright);
        db = AppDatabase.getInstance(this);


        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                if (!isLoading && !isLastPage){
                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount-1){
                        loadPost(false);
                    }
                }

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        postAdapter.filter(newText);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        postAdapter.filter(query);
                        return true;
                    }
                });
            }
        });

        postAdapter.setListener(post -> {
            Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
            intent.putExtra(DetailsActivity.EXTRA_POST, post);
            startActivity(intent);
        });
        postAdapter.setBookmarksListener((post, iscurrentlyBookmarked) -> {
            executor.execute(() -> {
                try {
                    if (iscurrentlyBookmarked) {
                        db.bookmarkDao().deleteByPostId(post.getId());
                    } else {
                        BookMarksEntity entity = new BookMarksEntity(
                                post.getId(), post.getTitle(), post.getBody()
                        );
                        db.bookmarkDao().insert(entity);
                    }
                    mainHandler.post(() -> {
                        postAdapter.updateBookMark(post.getId(), !iscurrentlyBookmarked);
                        Toast.makeText(
                                MainActivity.this,
                                iscurrentlyBookmarked ? "Removed from bookmarks" : "Saved to bookmarks",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                } catch (Exception e) {
                    mainHandler.post(() ->
                            Toast.makeText(MainActivity.this, "Bookmark failed", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        });
        loadBookmarkIds();
        loadCache();
        loadPost(true);

    }

    private void loadBookmarkIds() {
        executor.execute(() -> {
            List<BookMarksEntity> bookmarks = db.bookmarkDao().getAllBookmarks();
            Set<Integer> ids = new HashSet<>();
            for (BookMarksEntity bookmark : bookmarks) {
                ids.add(bookmark.getPostId());
            }
            mainHandler.post(() -> postAdapter.setBookmarkedPostIds(ids));
        });
    }

    private void loadCache() {
        executor.execute(() ->{
            List<PostEntity> entities = db.postDao().getAll();

            if (!entities.isEmpty()){
                List<Post> cachePost = new ArrayList<>();
                for (PostEntity entity : entities){
                    cachePost.add(new Post(entity.getId(), entity.getTitle(), entity.getBody()));

                }

                mainHandler.post(() ->{
                    postList.clear();
                    postList.addAll(cachePost);
                    postAdapter.syncDisplayList();
                    updateItem(cachePost.size());

                    updateCacheTimeDisplay();


                    if (cachePost.size() < PAGE_SIZE){
                        isLastPage = true;
                        postAdapter.setEndOfData(true);
                    }else{
                        currentPage = (cachePost.size() / PAGE_SIZE);
                    }
                });
            }
        });
    }

    private void updateCacheTimeDisplay() {
        long timestamp = CacheManager.getCacheTimestamp(this);
        if (timestamp == 0){
            cacheTime.setText("Cache: Never");
        }else{
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String cache_time = sdf.format(new Date());
            cacheTime.setText("Cache: " + cache_time );

        }

    }


    private int currentPage = 1;
    private static final int PAGE_SIZE = 2;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    private void loadPost(boolean isRefresh) {


        if (isRefresh) {
            currentPage = 1;
            isLastPage = false;
            isLoading= false;
            postList.clear();
            postAdapter.clear();
            postAdapter.setEndOfData(false);
            postAdapter.setLoading(false);
            progressBar.setVisibility(View.VISIBLE);

            executor.execute(()-> db.postDao().clearAll());
        }
        if (isLoading || isLastPage) return;


        isLoading = true;


        if (isRefresh) {
            postAdapter.setLoading(true);
        }


        Call<List<Post>> call = RetrofitClient.getApiService().getPosts(currentPage, PAGE_SIZE);
        call.enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                postAdapter.setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> posts = response.body();
                    final List<Post> postsToCache = new ArrayList<>(posts);
                    executor.execute(() -> {
                        List<PostEntity> entities = new ArrayList<>();
                        for (Post p : postsToCache) {
                            entities.add(new PostEntity(p.getId(), p.getTitle(), p.getBody()));
                        }
                        db.postDao().insertAll(entities);
                        long now = System.currentTimeMillis();
                        CacheManager.saveCacheTimeStamp(MainActivity.this, now);
                        mainHandler.post(() -> updateCacheTimeDisplay());
                    });

                    if (isRefresh || currentPage == 1){
                        postAdapter.setData(posts);
                        updateLastUpdateTime();
                        updateItem(posts.size());



                    }else{
                        postAdapter.addPost(posts);
                        updateItem(postAdapter.getItemCount() - (postAdapter.isLoading() ? 1 : 0) - (postAdapter.isEndOfData() ? 1 : 0));
                    }



                    if (posts.size() < PAGE_SIZE){
                        isLastPage = true;
                        postAdapter.setEndOfData(true);
                    }else{
                        currentPage ++;
                    }


                }else{
                    Toast.makeText(MainActivity.this, "fail to load", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable throwable) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                postAdapter.setLoading(false);
                Toast.makeText(MainActivity.this, "error" + throwable.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void updateLastUpdateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String time = sdf.format(new Date());
        lastUpdateView.setText("LastUpdate" + time);
    }

    private void updateItem(int count) {
        itemCount.setText("total items: " + count);
    }

    @Override
    public void onRefresh() {
        loadPost(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_bookmark){
            startActivity(new Intent(MainActivity.this, BookMarksActivity.class));
            return  true;
        } else if (item.getItemId() == R.id.action_dark_mode) {

            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            recreate();
            return  true;

        }
        return super.onOptionsItemSelected(item);
    }
}
