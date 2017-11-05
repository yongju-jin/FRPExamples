package yongju.frpexamples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import kotlinx.android.synthetic.main.fragment_capitalize.*
import yongju.frpexamples.R.layout.fragment_capitalize
import yongju.frpexamples.base.BaseFragment

/**
 * Created by yongju on 2017. 10. 24..
 */
class Capitalize : BaseFragment() {
    override val layoutId: Int = fragment_capitalize

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
//        val combineLatest: Observable<String> = Observable.combineLatest(
//                    RxView.clicks(btn_capitalize),
//                    RxTextView.textChanges(editText),
//                    BiFunction { _, tranText ->  tranText.toString().fragment_capitalize() }
//                )
//        combineLatest.subscribe(
//            textView::setText,
//            Throwable::printStackTrace
//        ).apply {
//            disposables.add(this)
//        }

        Observable.combineLatest<Any, CharSequence, String>(
                RxView.clicks(btn_capitalize),
                RxTextView.textChanges(editText3),
                BiFunction { _, tranText ->  tranText.toString().capitalize() }
        ).subscribe(
                textView::setText,
                Throwable::printStackTrace
        ).apply {
            disposables.add(this)
        }

//        val withLatestFrom: Observable<String> = RxView.clicks(btn_capitalize)
//                .withLatestFrom(
//                        RxTextView.textChanges(editText),
//                        BiFunction {
//                            _, tranText ->  tranText.toString().fragment_capitalize()
//                })
//        withLatestFrom.subscribe(
//                textView::setText,
//                Throwable::printStackTrace
//        ).apply {
//            disposables.add(this)
//        }

//        RxView.clicks(btn_capitalize)
//            .withLatestFrom<CharSequence, String>(
//                RxTextView.textChanges(editText),
//                BiFunction {
//                    _, tranText ->  tranText.toString().fragment_capitalize()
//                }
//            ).subscribe(
//                    textView::setText,
//                    Throwable::printStackTrace
//            ).apply {
//                disposables.add(this)
//            }
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