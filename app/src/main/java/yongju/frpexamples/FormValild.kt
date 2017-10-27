package yongju.frpexamples

import android.os.Bundle
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function6
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_form_valid.*
import yongju.frpexamples.R.layout.fragment_form_valid
import yongju.frpexamples.base.BaseFragment

/**
 * Created by yongju on 2017. 10. 27..
 */
class FormValild: BaseFragment() {
    private val TAG = "FormValid"

    override val layoutId: Int = fragment_form_valid

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val editEmails = listOf(
                edit_email_1,
                edit_email_2,
                edit_email_3,
                edit_email_4
        )

        val validName = BehaviorSubject.createDefault(false)

        // Name
        RxTextView.textChanges(edit_name).map {
            val trimmedVal = it.trim()
            when {
                trimmedVal.isEmpty() -> Pair(false, "enter something")
                !trimmedVal.contains(" ") -> Pair(false, "must contain space")
                else -> Pair(true, "no error")
            }
        }.subscribe({
            validName.onNext(it.first)
            text_name_error.text = it.second
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        val validEditEmail1 = BehaviorSubject.createDefault(true)
        val validEditEmail2 = BehaviorSubject.createDefault(false)
        val validEditEmail3 = BehaviorSubject.createDefault(false)
        val validEditEmail4 = BehaviorSubject.createDefault(false)

        val validEditEmails = listOf(
                validEditEmail1,
                validEditEmail2,
                validEditEmail3,
                validEditEmail4
        )

        val validEmailCount = BehaviorSubject.createDefault(true)

        val emailCount = BehaviorSubject.createDefault(1).apply {
                map {
                    when {
                        it < 1 || it > 4 -> Pair(it, "must be 1 to 4")
                        else -> Pair(it, "no error")
                    }
                }.subscribe {
                    text_email_count.text = it.first.toString()
                    text_email_count_error.text = it.second

                    editEmails.forEachIndexed { index, editText ->
                        val enabled = index < it.first
                        validEditEmails[index].onNext(enabled)
                        editText.isEnabled = enabled
                    }
            }.apply {
                disposables.add(this)
            }
        }

        RxView.clicks(btn_plus).map { +1 }
                .mergeWith(
                        RxView.clicks(btn_minus).map { -1 }
                ).withLatestFrom<Int, Int>(emailCount,
                BiFunction { t1, t2 -> t1 + t2 }
        ).subscribe({
            emailCount.onNext(it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        val validEmail1 = BehaviorSubject.createDefault(false)
        val validEmail2 = BehaviorSubject.createDefault(false)
        val validEmail3 = BehaviorSubject.createDefault(false)
        val validEmail4 = BehaviorSubject.createDefault(false)

        val validEmails = listOf(
                validEmail1,
                validEmail2,
                validEmail3,
                validEmail4
        )

        editEmails.forEachIndexed { index, editText ->
            Observable.combineLatest<CharSequence, Boolean, Pair<CharSequence, Boolean>>(RxTextView.textChanges(editText), validEditEmails[index],
                    BiFunction { t1, t2 -> Pair(t1, t2) })
                    .subscribe({
                        when(it.second) {
                            true -> {
                                text_email_valid_error.text = when {
                                    it.first.contains("@") -> {
                                        validEmails[index].onNext(true)
                                        "no Error"
                                    }
                                    else -> {
                                        validEmails[index].onNext(false)
                                        "#${index+1} must have @"
                                    }
                                }
                            }
                            false -> validEmails[index].onNext(true)
                        }
                    }, Throwable::printStackTrace)
        }

        Observable.combineLatest<Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean>(validName, validEmailCount, validEmail1, validEmail2, validEmail3, validEmail4,
                Function6 { t1, t2, t3, t4, t5, t6 ->
                    t1 && t2 && t3 && t4 && t5 && t6 })
                .subscribe {
                    btn_valid.isEnabled = it
                }.apply {
                    disposables.add(this)
                }
    }
}