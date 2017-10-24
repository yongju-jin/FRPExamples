package yongju.frpexamples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import kotlinx.android.synthetic.main.fragment_04.*
import yongju.frpexamples.R.layout.fragment_04
import yongju.frpexamples.base.BaseFragment

/**
 * Created by yongju on 2017. 10. 24..
 */
class Fragment04: BaseFragment() {
    override val layoutId: Int = fragment_04

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val combineLatest: Observable<String> = Observable.combineLatest(
                    RxView.clicks(btn_capitalize),
                    RxTextView.textChanges(editText),
                    BiFunction { _, tranText ->  tranText.toString().capitalize() }
                )
        combineLatest.subscribe(
            textView::setText,
            Throwable::printStackTrace
        ).apply {
            disposables.add(this)
        }

//        val withLatestFrom: Observable<String> = RxView.clicks(btn_capitalize)
//                .withLatestFrom(
//                        RxTextView.textChanges(editText),
//                        BiFunction {
//                            _, tranText ->  tranText.toString().capitalize()
//                })
//        withLatestFrom.subscribe(
//                textView::setText,
//                Throwable::printStackTrace
//        ).apply {
//            disposables.add(this)
//        }
    }

    override fun onResume() {
        Log.d(TAG, "[onResume]")
        (activity as AppCompatActivity).supportActionBar?.title = TAG
        super.onResume()
    }

    companion object {
        val TAG = "Capitalize"
    }

}