package yongju.frpexamples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_01.*
import yongju.frpexamples.R.layout.fragment_01
import yongju.frpexamples.base.BaseFragment

/**
 * Created by yongju on 2017. 10. 23..
 */
class Fragment01: BaseFragment() {
    override val layoutId: Int = fragment_01

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        RxView.clicks(clear).map {
            ""
        }.subscribe(
            editText::setText,
            Throwable::printStackTrace
        ).apply {
            disposables.add(this)
        }

//        val sClicked = RxView.clicks(clear)
//        val sClearIt = sClicked.map { "" }
//        sClearIt.subscribe(
//                editText::setText,
//                Throwable::printStackTrace
//        )
//
//        RxView.clicks(clear).subscribe({
//            editText.setText("")
//        }, Throwable::printStackTrace)
    }

    override fun onResume() {
        Log.d(TAG, "[onResume]")
        (activity as AppCompatActivity).supportActionBar?.title = TAG
        super.onResume()
    }

    companion object {
        val TAG = "Clear"
    }
}