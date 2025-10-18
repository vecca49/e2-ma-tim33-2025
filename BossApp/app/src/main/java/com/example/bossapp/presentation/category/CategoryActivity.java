package com.example.bossapp.presentation.category;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bossapp.R;

public class CategoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Kategorije");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CreateCategoryFragment())
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
