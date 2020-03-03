package com.appnyang.leafbookshelf.view.page.activity

import android.animation.ObjectAnimator
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.appnyang.leafbookshelf.R
import com.appnyang.leafbookshelf.data.model.bookmark.BookmarkType
import com.appnyang.leafbookshelf.databinding.ActivityPageBinding
import com.appnyang.leafbookshelf.util.afterMeasured
import com.appnyang.leafbookshelf.util.transformer.DepthPageTransformer
import com.appnyang.leafbookshelf.view.page.fragment.PageFragment
import com.appnyang.leafbookshelf.viewmodel.PageViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.dialog_add_bookmark.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class PageActivity : AppCompatActivity() {

    private val viewModel by viewModel<PageViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()

        DataBindingUtil.setContentView<ActivityPageBinding>(this, R.layout.activity_page).apply {
            viewModel = this@PageActivity.viewModel
            lifecycleOwner = this@PageActivity
        }

        registerSystemUiChangeListener()

        // Initialize ViewPager2.
        pager.setPageTransformer(DepthPageTransformer())
        pager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.currentPage.value = position
            }
        })

        // TODO: Handle the screen orientation changes.
        // Open files depends on file type.
        if (savedInstanceState == null) {
            textPainter.afterMeasured {
                openBook()
            }
        }

        subscribeObservers()
    }

    /**
     * On focused to the window, hide status bar.
     *
     * @param hasFocus true when this window is focused.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
        }
    }

    /**
     * When the system ui is appear, hide it after 3 seconds.
     */
    private fun registerSystemUiChangeListener() {
        window.decorView.setOnSystemUiVisibilityChangeListener {
            if (it and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // System bars are visible.
                Handler().postDelayed({
                    hideStatusBar()
                }, 3000)
            }
        }
    }

    override fun onBackPressed() {
        if (viewModel.isAnyMenuOpened()) {
            viewModel.displayMenu()
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()

        // Save a bookmark.
        viewModel.saveCurrentBookmark(getString(R.string.last_read), BookmarkType.LAST_READ)

        viewModel.saveHistory()
    }

    /**
     * Open files depends on file type.
     */
    private fun openBook() {
        intent.extras?.getParcelable<Uri>(KEY_FILE_URI)?.let {
            val layoutParam = PageViewModel.StaticLayoutParam(textPainter.width,
                textPainter.height - 2 * resources.getDimension(R.dimen.page_margin).toInt(),
                textPainter.paint,
                textPainter.lineSpacingMultiplier,
                textPainter.lineSpacingExtra,
                textPainter.includeFontPadding)

            val charIndex = intent.extras?.getLong(KEY_CHAR_INDEX, -1) ?: -1

            viewModel.readBookFromUri(it, applicationContext.contentResolver, layoutParam, charIndex)
        }
    }

    /**
     * Subscribe live data from ViewModel.
     */
    private fun subscribeObservers() {
        // Called when the first chunk is paginated.
        viewModel.pagedBook.observe(this, Observer {
            // Setup ViewPager.
            pager.adapter = TextPagerAdapter(it)

            textPages.text = getPageCountString()
            seekPages.max = it.size - 1
        })

        // Called rest of chunks is paginated.
        viewModel.chunkPaged.observe(this, Observer {
            pager.adapter?.notifyDataSetChanged()

            textPages.text = getPageCountString()
            seekPages.max = viewModel.pagedBook.value!!.size - 1
        })

        // Called the user has navigated to the page.
        viewModel.currentPage.observe(this, Observer {
            // Avoid infinite loop.
            if (pager.currentItem != it) {
                pager.setCurrentItem(it, viewModel.bScrollAnim.get())
                viewModel.bScrollAnim.set(true)
            }

            textPages.text = getPageCountString()
        })

        // Called when the middle of page is clicked.
        viewModel.showMenu.observe(this, Observer {
            showMenu(it)
        })

        // Called when the back button of top-menu clicked.
        viewModel.clickedBack.observe(this, Observer {
            viewModel.displayMenu()
            onBackPressed()
        })

        // Called when the Add Bookmark button of top-menu clicked.
        viewModel.clickedAddBookmark.observe(this, Observer {
            viewModel.displayMenu()
            AddBookmarkDialog(viewModel.pagedBook.value?.get(viewModel.currentPage.value ?: 0)?.substring(0..10)?.trim() ?: "", viewModel)
                .show(supportFragmentManager, "AddBookmark")
        })

        // Called when settings button of top-menu clicked.
        viewModel.showSettings.observe(this, Observer {
            showSettingsMenu(it)
        })

        // Called when the bookmark button of top-menu clicked.
        viewModel.showBookmark.observe(this, Observer {
            showBookmarksMenu(it)
        })

        // Called when TTS chip is clicked.
        viewModel.bTts.observe(this, Observer {
            viewModel.startTtsService(it)
        })

        // Called when Auto chip is clicked.
        viewModel.bAuto.observe(this, Observer {

        })

        // Called when bookmarks from database is updated.
        viewModel.bookmarks.observe(this, Observer {
            // Add bookmark chips.
            chipGroupBookmarks.removeAllViews()
            chipGroupAutoGeneratedBookmarks.removeAllViews()

            it.forEach { bookmark ->
                val group: ChipGroup = if (bookmark.type == BookmarkType.CUSTOM.name) { chipGroupBookmarks } else { chipGroupAutoGeneratedBookmarks }
                val chip = layoutInflater.inflate(R.layout.layout_bookmark_chip, group, false) as Chip
                chip.text = bookmark.title
                chip.setTag(R.string.tag_title, bookmark.title)
                chip.setTag(R.string.tag_index, bookmark.index)
                chip.setOnCloseIconClickListener { view ->
                    viewModel.deleteBookmark(view.getTag(R.string.tag_title).toString(), view.tag.toString().toLong())
                }
                chip.setOnClickListener { view ->
                    if (!viewModel.isPaginating.get()) {
                        viewModel.setCurrentPageToTextIndex(view.tag.toString().toLong())
                    }
                }
                group.addView(chip)
            }
        })
    }

    /**
     * Hide system status bar when focused.
     */
    private fun hideStatusBar() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    /**
     * Show or hide menus based on bShow
     *
     * @param bShow true to show the menus.
     */
    private fun showMenu(bShow: Boolean) {
        if (layoutTopMenu.height != 0 || layoutBottomMenu.height != 0) {
            val animationDuration = 200L

            if (bShow) {
                ObjectAnimator.ofFloat(layoutTopMenu, "translationY", 0f).apply {
                    duration = animationDuration
                    start()
                }
                ObjectAnimator.ofFloat(layoutBottomMenu, "translationY", 0f).apply {
                    duration = animationDuration
                    start()
                }
            } else {
                ObjectAnimator.ofFloat(layoutTopMenu, "translationY", -layoutTopMenu.height.toFloat()).apply {
                    duration = animationDuration
                    start()
                }
                ObjectAnimator.ofFloat(layoutBottomMenu, "translationY", layoutBottomMenu.height.toFloat()).apply {
                    duration = animationDuration
                    start()
                }
            }
        }
    }

    /**
     * Show or hide bookmarks menu.
     *
     * @param bShow true to show the bookmarks menu.
     */
    private fun showBookmarksMenu(bShow: Boolean) {
        if (layoutBookmarkMenu.height != 0) {
            val animationDuration = 200L

            if (bShow) {
                ObjectAnimator.ofFloat(layoutBookmarkMenu, "translationY", 0f).apply {
                    duration = animationDuration
                    start()
                }
            }
            else {
                ObjectAnimator.ofFloat(layoutBookmarkMenu, "translationY", layoutBookmarkMenu.height.toFloat() * 1.5f).apply {
                    duration = animationDuration
                    start()
                }
            }
        }
    }

    /**
     * Show or hide settings menu.
     *
     * @param bShow true to show the settings menu.
     */
    private fun showSettingsMenu(bShow: Boolean) {
        if (layoutSettingsMenu.height != 0) {
            val animationDuration = 200L

            if (bShow) {
                ObjectAnimator.ofFloat(layoutSettingsMenu, "translationY", 0f).apply {
                    duration = animationDuration
                    start()
                }
            }
            else {
                ObjectAnimator.ofFloat(layoutSettingsMenu, "translationY", -layoutSettingsMenu.height.toFloat()).apply {
                    duration = animationDuration
                    start()
                }
            }
        }
    }

    private fun getPageCountString() =
        getString(R.string.page_counter, pager.currentItem + 1, pager.adapter?.itemCount ?: 0)

    companion object {
        const val KEY_FILE_URI = "KEY_FILE_URI"
        const val KEY_CHAR_INDEX = "KEY_CHAR_INDEX"

        const val REQUEST_NOTIFICATION_CLICK = 5000
    }

    /**
     * A Text binder for ViewPager2.
     */
    private inner class TextPagerAdapter(val texts: List<CharSequence>) : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = texts.size

        override fun createFragment(position: Int): Fragment {
            return PageFragment(position)
        }
    }
}

class AddBookmarkDialog(private val title: String, private val viewModel: PageViewModel) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val view = requireActivity().layoutInflater.inflate(R.layout.dialog_add_bookmark, null)
            view.editBookmarkTitle.setText(title)

            val builder = AlertDialog.Builder(it)
                .setView(view)
                .setTitle(R.string.title_add_bookmark)
                .setIcon(R.drawable.ic_bookmark_add)
                .setPositiveButton(R.string.button_add) { _, _ ->
                    viewModel.saveCurrentBookmark(view.editBookmarkTitle.text.toString())
                }
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    dialog?.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException()
    }
}
