package yongju.frpexamples.base

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by yongju on 2017. 10. 23..
 */
abstract class BaseFragment: Fragment() {
    protected val disposables by lazy {
        CompositeDisposable()
    }
    protected abstract val layoutId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        throw NotImplementedError()
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }
}