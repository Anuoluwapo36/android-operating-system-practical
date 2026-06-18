package com.example.operatingsystempraticalclass;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class DetailsActivity extends AppCompatActivity {

    public static final String EXTRA_POST = "post";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);


        Post post = (Post) getIntent().getSerializableExtra(EXTRA_POST);


        if (post == null) return;


        TextView tvTitle = (TextView) findViewById(R.id.tvDetailsTitle);
        TextView tvBody = (TextView) findViewById(R.id.tvDetailsBody);
        ImageView ivImage = (ImageView)  findViewById(R.id.ivImage);



        tvTitle.setText(post.getTitle());
        tvBody.setText(post.getBody());

        String imagUri = "https://picsum.photos/id/" + (post.getId() % 100) + "/400/200";
        Glide.with(this).load(imagUri).placeholder(R.mipmap.ic_launcher).into(ivImage);
    }



}
