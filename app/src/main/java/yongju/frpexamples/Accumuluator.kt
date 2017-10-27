package yongju.frpexamples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_accumuluator.*
import yongju.frpexamples.R.layout.fragment_accumuluator
import yongju.frpexamples.base.BaseFragment

/**
 * Created by yongju on 2017. 10. 24..
 */
class Accumuluator: BaseFragment() {
    override val layoutId: Int = fragment_accumuluator
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        val plusStream = RxView.clicks(btn_plus).map { +1 }
        val minusStream = RxView.clicks(btn_minus).map { -1 }
        val mergeWith = plusStream.mergeWith(minusStream)

        val createDefault = BehaviorSubject.createDefault(0).apply {
            filter { it > -1 }.subscribe {
                textView.text = it.toString()
            }.apply {
                disposables.add(this)
            }
        }

        mergeWith.withLatestFrom<Int, Int>(createDefault,
                BiFunction { t1, t2 ->
//                    val value = t1 + t2
//                    createDefault.onNext(value)
//                    value.toString()
                    t1 + t2
                }).subscribe {
            createDefault.onNext(it)
        }.apply {
            disposables.add(this)
        }
    }

    override fun onResume() {
        Log.d(TAG, "[onResume]")
        (activity as AppCompatActivity).supportActionBar?.title = TAG
        super.onResume()
    }

    companion object {
        val TAG = "Accumulator"
    }
}