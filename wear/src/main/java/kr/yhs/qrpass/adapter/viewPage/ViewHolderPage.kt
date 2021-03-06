package kr.yhs.qrpass.adapter.viewPage

import android.view.View
import android.view.ViewStub
import androidx.recyclerview.widget.RecyclerView
import kr.yhs.qrpass.R

class ViewHolderPage(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var viewStub: ViewStub = itemView.findViewById(R.id.page_viewer_include)

    fun onBind(data: ViewData): View {
        bindingAdapter
        viewStub.layoutResource = data.id
        return viewStub.inflate()
    }
}