package com.appnyang.leafbookshelf.view.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.appnyang.leafbookshelf.R
import com.appnyang.leafbookshelf.data.model.history.History
import com.appnyang.leafbookshelf.databinding.LayoutRecentFileBinding
import com.appnyang.leafbookshelf.view.main.activity.MainActivity
import com.appnyang.leafbookshelf.viewmodel.MainViewModel


/**
 * History Adapter.
 *
 * @author Sangwoo <sangwoo@yesang.com> on 2020-02-22.
 */
class HistoryAdapter(var items: List<History>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: LayoutRecentFileBinding = DataBindingUtil.bind<LayoutRecentFileBinding>(view)!!.apply {
            viewModel = getViewModel(view)
        }

        private fun getViewModel(view: View): MainViewModel? {
            var context: Context = view.context
            while (context is ContextWrapper) {
                if (context is Activity) {
                    return (context as MainActivity).getMainViewModel()
                }
                context = context.baseContext
            }
            return null
        }
    }

    /**
     * Create a new view.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder =
        HistoryViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.layout_recent_file, parent, false)
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.binding.item = items[position]
    }
}