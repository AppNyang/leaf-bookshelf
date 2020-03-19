package com.appnyang.leafbookshelf.view.bookshelf.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.appnyang.leafbookshelf.R
import com.appnyang.leafbookshelf.databinding.ActivityBookshelfBinding
import com.appnyang.leafbookshelf.view.page.activity.PageActivity
import com.appnyang.leafbookshelf.viewmodel.BookshelfViewModel
import kotlinx.android.synthetic.main.activity_bookshelf.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class BookshelfActivity : AppCompatActivity() {

    private val viewModel by viewModel<BookshelfViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivityBookshelfBinding>(this, R.layout.activity_bookshelf).apply {
            viewModel = this@BookshelfActivity.viewModel
            lifecycleOwner = this@BookshelfActivity
        }

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerHistories.layoutManager = GridLayoutManager(this, 3)

        // Register observer.
        viewModel.historyClicked.observe(this, Observer {
            startActivity(Intent(this, PageActivity::class.java).apply {
                putExtra(PageActivity.KEY_FILE_URI, Uri.parse(it.first))
                putExtra(PageActivity.KEY_CHAR_INDEX, it.second)
            })
        })
    }
}