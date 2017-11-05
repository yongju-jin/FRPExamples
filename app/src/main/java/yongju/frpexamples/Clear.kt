package yongju.frpexamples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_clear.*
import yongju.frpexamples.R.layout.fragment_clear
import yongju.frpexamples.base.BaseFragment

/**
 * Created by yongju on 2017. 10. 23..
 */
class Clear : BaseFragment() {
    override val layoutId: Int = fragment_clear

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        RxView.clicks(clear).map {
            ""
        }.subscribe(
            editText1::setText,
            Throwable::printStackTrace
        ).apply {
            disposables.add(this)
        }

//        val sClicked = RxView.clicks(fragment_clear)
//        val sClearIt = sClicked.map { "" }
//        sClearIt.subscribe(
//                editText::setText,
//                Throwable::printStackTrace
//        )
//
//        RxView.clicks(fragment_clear).subscribe({
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