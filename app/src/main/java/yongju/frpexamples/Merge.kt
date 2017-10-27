package yongju.frpexamples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_merge.*
import yongju.frpexamples.R.layout.fragment_merge
import yongju.frpexamples.base.BaseFragment

/**
 * Created by yongju on 2017. 10. 24..
 */
class Merge : BaseFragment() {
    override val layoutId: Int = fragment_merge

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val obHello = RxView.clicks(btn_hello).map { "hello" }
        val obThx = RxView.clicks(btn_thx).map { "thank you" }

        obHello.mergeWith(obThx)
            .subscribe(
                textView::setText,
                Throwable::printStackTrace
            ).apply {
                disposables.add(this)
            }
    }

    override fun onResume() {
        Log.d(TAG,"[onResume]")
        (activity as AppCompatActivity).supportActionBar?.title = TAG
        super.onResume()
    }

    companion object {
        val TAG = "Merge"
    }
}