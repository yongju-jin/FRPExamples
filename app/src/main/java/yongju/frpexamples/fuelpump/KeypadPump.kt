package yongju.frpexamples.fuelpump

import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_keypadpump.*
import yongju.frpexamples.R.layout.fragment_keypadpump
import yongju.frpexamples.base.BaseFragment

/**
 * Created by yongju on 2017. 11. 26..
 */
class KeypadPump: BaseFragment() {
    override val layoutId: Int = fragment_keypadpump

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val value = BehaviorSubject.createDefault(0.0).apply {
            subscribe({
                tv_preset.text = it.toString()
            }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        btn_0.clicks().map { 0 }.mergeWith(
        btn_1.clicks().map { 1 }).mergeWith(
        btn_2.clicks().map { 2 }).mergeWith(
        btn_3.clicks().map { 3 }).mergeWith(
        btn_4.clicks().map { 4 }).mergeWith(
        btn_5.clicks().map { 5 }).mergeWith(
        btn_6.clicks().map { 6 }).mergeWith(
        btn_7.clicks().map { 7 }).mergeWith(
        btn_8.clicks().map { 8 }).mergeWith(
        btn_9.clicks().map { 9 }).withLatestFrom<Double, Double>(value, BiFunction { number, _value ->
            val x10 = _value * 10.0
            when {
                x10 >= 1000.0 -> -1.0
                else -> x10 + number
            }
        }).filter {
            it > 0
        }.mergeWith(
            btn_clear.clicks().map { 0.0 }
        ).subscribe({
            value.onNext(it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }
    }
}


