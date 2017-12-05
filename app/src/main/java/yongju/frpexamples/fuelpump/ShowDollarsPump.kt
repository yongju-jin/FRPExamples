package yongju.frpexamples.fuelpump

import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.widget.checkedChanges
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function4
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_fuelpump.*
import yongju.frpexamples.R.layout.fragment_fuelpump
import yongju.frpexamples.base.BaseFragment
import yongju.frpexamples.fuelpump.model.Fuel.Empty
import yongju.frpexamples.fuelpump.model.Fuel
import java.util.concurrent.TimeUnit

/**
 * Created by yongju on 2017. 11. 20..
 */
class ShowDollarsPump : BaseFragment() {
    private val TAG = "ShowDollarsPump"
    override val layoutId: Int = fragment_fuelpump

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val obLiter = BehaviorSubject.create<String>().apply {
            observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        tv_liters_count.text = it
                    }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        val obDollars = BehaviorSubject.create<String>().apply {
            observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        tv_dollars_count.text = it
                    },Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        val obPrice1 = et_fuel1.textChanges().filter { it.isNotEmpty() }
        val obPrice2 = et_fuel2.textChanges().filter { it.isNotEmpty() }
        val obPrice3 = et_fuel3.textChanges().filter { it.isNotEmpty() }

        val obFuel1 = tb_fuel1.checkedChanges().skip(1).share()
        val obFuel2 = tb_fuel2.checkedChanges().skip(1).share()
        val obFuel3 = tb_fuel3.checkedChanges().skip(1).share()

        val obFuel1Down = whenFuelDown(obFuel1, Fuel.Fuel1)
        val obFuel2Down = whenFuelDown(obFuel2, Fuel.Fuel2)
        val obFuel3Down = whenFuelDown(obFuel3, Fuel.Fuel3)

        val obFuel1Lift = whenFuelLift(obFuel1, Fuel.Fuel1)
        val obFuel2Lift = whenFuelLift(obFuel2, Fuel.Fuel2)
        val obFuel3Lift = whenFuelLift(obFuel3, Fuel.Fuel3)

        val fillActive = BehaviorSubject.createDefault<Fuel>(Empty)

        val obFuelDown = obFuel1Down.mergeWith(obFuel2Down).mergeWith(obFuel3Down)
        val obEnd = obFuelDown.withLatestFrom<Fuel, Fuel>(fillActive,
                BiFunction { fuel, fActive ->
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
                    when (fActive) {
                        is Empty -> fuel
                        else -> Empty
                    }
                }).filter {
            it != Empty
        }.share()

        val obPulses = Observable.interval(200, TimeUnit.MILLISECONDS)
                .withLatestFrom<Fuel, Fuel>(fillActive,
                        BiFunction { _, t2 ->
                            t2
                        }).filter {
            it != Empty
        }
                .map { FuelPulses }

        val obCali = BehaviorSubject.createDefault(Calibration)
        val obTotal = BehaviorSubject.createDefault(0)

        obStart.map { 0 }.mergeWith(
                obPulses.withLatestFrom<Int, Int>(obTotal,
                        BiFunction { pulse, total ->
                            pulse + total
                        })
        ).subscribe({
            obTotal.onNext(it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        obEnd.mergeWith(obStart)
                .subscribe({
                    fillActive.onNext(it)
                }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        val obCaliTotal = Observable.combineLatest<Double, Int, Double>(obCali, obTotal,
                BiFunction { cali, total ->
                    cali * total
                }).share()

        obCaliTotal.subscribe({
            obLiter.onNext(it.toString())
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        val obPrice = obStart.withLatestFrom<CharSequence, CharSequence, CharSequence, Double>(obPrice1, obPrice2, obPrice3,
                Function4 { start, price1, price2, price3 ->
                    (when (start) {
                        is Fuel.Fuel1 -> price1
                        is Fuel.Fuel2 -> price2
                        is Fuel.Fuel3 -> price3
                        else -> throw IllegalStateException("not selected fuel")
                    }).toString().toDouble()
                })

        obCaliTotal.withLatestFrom<Double, Double>(obPrice,
                BiFunction { caliTotal, price ->
                    caliTotal * price })
                .subscribe({
                    obDollars.onNext(it.toString())
                }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }
    }

    private fun whenFuelLift(obFuel: Observable<Boolean>, fuel: Fuel): Observable<Fuel>
            = obFuel.filter { it }
            .map { fuel }

    private fun whenFuelDown(obFuel: Observable<Boolean>, fuel: Fuel)
            = obFuel.filter { !it }
            .map { fuel }

    companion object {
        val FuelPulses = 40
        val Calibration = 0.01
    }
}