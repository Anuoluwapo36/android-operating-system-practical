package com.example.operatingsystempraticalclass;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Post> postOriginal = new ArrayList<>();
    private  List<Post> postsDelay = new ArrayList<>();
    private OnItemClickListener Listener;
    private OnBookmarksClickListener  bookmarksListener;
    private static final int TYPE_POST = 0;
    private static final int TYPE_LOADING =1;
    private static final int TYPE_END = 2;
    private boolean isLoading;
    private boolean isEndOfData;
    private final Set<Integer> bookmarkedPostIds = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClickPost(Post post);
    }
    public interface OnBookmarksClickListener {
        void onBookmarksClick(Post post, boolean iscurrentlyBookmarked);
    }

    public PostAdapter(List<Post> postList) {

        this.postOriginal = postList;
        this.isLoading = false;
        this.isEndOfData = false;
    }

    public void setLoading(boolean loading) {

        if (this.isLoading == loading) return;
        isLoading = loading;

        notifyDataSetChanged();


    }


    public void setEndOfData(boolean end) {
        if (this.isEndOfData == end) return;
        this.isEndOfData = end;
        notifyDataSetChanged();




    }

    public void setListener(OnItemClickListener listener) {
        Listener = listener;
    }

    public void setBookmarksListener(OnBookmarksClickListener bookmarksListener) {
        this.bookmarksListener = bookmarksListener;
    }

    public void setBookmarkedPostIds(Set<Integer> ids) {
        bookmarkedPostIds.clear();
        bookmarkedPostIds.addAll(ids);
        notifyDataSetChanged();
    }

    public void syncDisplayList() {
        postsDelay.clear();
        postsDelay.addAll(postOriginal);
        notifyDataSetChanged();
    }



    public void clear() {
        postOriginal.clear();
        postsDelay.clear();
        notifyDataSetChanged();
    }
    public  void addPost(List<Post> newPost){
        int startPos = postOriginal.size();
        postOriginal.addAll(newPost);


        postsDelay.addAll(newPost);
        notifyItemRangeInserted(startPos, newPost.size());
    }

    public  void  setData(List<Post> newPost){
        postOriginal.clear();
        postsDelay.clear();
        postOriginal.addAll(newPost);
        postsDelay.addAll(newPost);
        notifyDataSetChanged();
    }


    public  void filter(String query){
        postsDelay.clear();
        if (query.isEmpty()){
            postsDelay.addAll(postOriginal);
        }else{
            for (Post p : postOriginal){
                if (p.getTitle().toLowerCase().contains(query.toLowerCase())){
                    postsDelay.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }
    public  void  updateBookMark(int postId, boolean isBookMarked){
        if (isBookMarked) {
            bookmarkedPostIds.add(postId);
        } else {
            bookmarkedPostIds.remove(postId);
        }

        for (int i = 0; i < postsDelay.size(); i++) {
            if (postsDelay.get(i).getId() == postId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    private boolean isBookmarked(int postId) {
        return bookmarkedPostIds.contains(postId);
    }

    private void updateBookmarkButton(ImageButton button, boolean bookmarked) {
        button.setTag(bookmarked);
        button.setImageResource(
                bookmarked ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );
    }

    public  boolean isLoading(){
        return  this.isLoading;
    }

    public  boolean isEndOfData(){
        return  this.isEndOfData;
    }
    @Override
    public int getItemViewType(int position) {
        if (position == postsDelay.size() && isLoading) return TYPE_LOADING;
        if (position == postsDelay.size() && isEndOfData) return TYPE_END;

        return TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        if (i == TYPE_POST) {
            View view = inflater.inflate(R.layout.item_post, viewGroup, false);
            return new ViewHolder(view);
        } else if (i == TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_loading, viewGroup, false);
            return new LoadingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_end_of_data, viewGroup, false);
            return new EndViewHolder(view);
        }


    }



    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder myViewHolder, int i) {


        if (myViewHolder instanceof ViewHolder) {
            Post post = postsDelay.get(i);

            ViewHolder view = (ViewHolder) myViewHolder;
            view.title.setText(post.getTitle());
            view.body.setText(post.getBody());
            myViewHolder.itemView.setOnClickListener(v -> {
                if (Listener != null) Listener.onItemClickPost(post);
            });



            ViewHolder holder = (ViewHolder) myViewHolder;
            updateBookmarkButton(holder.btnBookmark, isBookmarked(post.getId()));
            holder.btnBookmark.setOnClickListener(v -> {
                if (bookmarksListener != null) {
                    bookmarksListener.onBookmarksClick(post, isBookmarked(post.getId()));
                }
            });


        }



    }

    @Override
    public int getItemCount() {

        int count = postsDelay.size();


        if (isLoading) count++;
        else if (isEndOfData && postsDelay.size() > 0) count++;



        return  count;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, body;
        ImageButton btnBookmark;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.tvTitle);
            body = itemView.findViewById(R.id.tvBody);
            btnBookmark= itemView.findViewById(R.id.rvBookmarks);
        }
    }

   public static class LoadingViewHolder extends RecyclerView.ViewHolder {

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class EndViewHolder extends RecyclerView.ViewHolder {

        public EndViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
