package yongju.frpexamples.fuelpump01

import android.os.Bundle
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.widget.checkedChanges
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_fuelpump.*
import yongju.frpexamples.R.layout.fragment_fuelpump
import yongju.frpexamples.base.BaseFragment
import yongju.frpexamples.fuelpump01.model.Empty
import yongju.frpexamples.fuelpump01.model.Fuel

/**
 * Created by yongju on 2017. 11. 4..
 */
class FuelPump01 : BaseFragment() {
    private val TAG = "FuelPump01"

    override val layoutId: Int = fragment_fuelpump

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val obLiter = BehaviorSubject.createDefault("").apply {
            subscribe({
                tv_liters_count.text = it
            }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        val obFuel1 = tb_fuel1.checkedChanges().skip(1)
        val obFuel2 = tb_fuel2.checkedChanges().skip(1)
        val obFuel3 = tb_fuel3.checkedChanges().skip(1)

        val fuel1 = Fuel.Fuel1("1")
        val fuel2 = Fuel.Fuel2("2")
        val fuel3 = Fuel.Fuel3("3")

        val obFuel1Down = whenFuelDown(obFuel1, fuel1)
        val obFuel2Down = whenFuelDown(obFuel2, fuel2)
        val obFuel3Down = whenFuelDown(obFuel3, fuel3)

        val obFuel1Lift = whenFuelLift(obFuel1, fuel1)
        val obFuel2Lift = whenFuelLift(obFuel2, fuel2)
        val obFuel3Lift = whenFuelLift(obFuel3, fuel3)

        val fillActive = BehaviorSubject.createDefault<Fuel>(Empty).apply {
            subscribe({
                Log.d(TAG, "[fillActive] $it")
                obLiter.onNext(it.toString())
            }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        val obFuelDown = obFuel1Down.mergeWith(obFuel2Down).mergeWith(obFuel3Down)
        val obEnd = obFuelDown.withLatestFrom<Fuel, Fuel>(fillActive,
                BiFunction { fuel, fActive ->
                    Log.d(TAG, "[obEnd] fuel3: $fuel, fActive: $fActive")
                    when (fuel) {
                        fActive -> Empty
                        else -> fuel
                    }
                }).filter {
                    it == Empty
                }

        val obFuelLift = obFuel1Lift.mergeWith(obFuel2Lift).mergeWith(obFuel3Lift)
        val obStart = obFuelLift.withLatestFrom<Fuel, Fuel>(fillActive,
                BiFunction { fuel, fActive ->
                    Log.d(TAG, "[obStart] fuel3: $fuel, fActive: $fActive")
                    when (fActive) {
                        is Empty -> fuel
                        else -> Empty
                    }
                }).filter {
                    it != Empty
                }

        obEnd.mergeWith(obStart)
                .subscribe({
                    Log.d(TAG, "[obStartEnd] $it")
                    fillActive.onNext(it)
                }, Throwable::printStackTrace).apply {
                    disposables.add(this)
                }
    }

    private fun whenFuelLift(obFuel: Observable<Boolean>, fuel: Fuel): Observable<Fuel>
        = obFuel.filter {
                Log.d(TAG, "[whenFueLift] $it")
                it
            }
            .map { fuel }

    private fun whenFuelDown(obFuel: Observable<Boolean>, fuel: Fuel)
        = obFuel.filter {
                Log.d(TAG, "[whenFueDown] $it")
                !it
            }
            .map { fuel }
}