package yongju.frpexamples.fuelpump

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.widget.checkedChanges
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.functions.Function4
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_fuelpump.*
import yongju.frpexamples.R.layout.fragment_fuelpump
import yongju.frpexamples.base.BaseFragment
import yongju.frpexamples.fuelpump.model.Empty
import yongju.frpexamples.fuelpump.model.Fuel
import yongju.frpexamples.fuelpump.model.base.Phase
import java.util.concurrent.TimeUnit

/**
 * Created by yongju on 2017. 11. 23..
 */
class ClearSalePump: BaseFragment() {
    private val TAG = "ClearSalePump"

    override val layoutId: Int = fragment_fuelpump

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        // liter 스트림
        val obLiter = BehaviorSubject.create<String>().apply {
            observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        tv_liters_count.text = it
                    }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        // 가격(Dollars)의 스트림
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

        val fuel1 = Fuel.Fuel1("1")
        val fuel2 = Fuel.Fuel2("2")
        val fuel3 = Fuel.Fuel3("3")

        val obFuel1Down = whenFuelDown(obFuel1, fuel1)
        val obFuel2Down = whenFuelDown(obFuel2, fuel2)
        val obFuel3Down = whenFuelDown(obFuel3, fuel3)

        val obFuel1Lift = whenFuelLift(obFuel1, fuel1)
        val obFuel2Lift = whenFuelLift(obFuel2, fuel2)
        val obFuel3Lift = whenFuelLift(obFuel3, fuel3)

        // 노즐에서 흘러가는지 나타내는 스트림
        val obFuelFlowing = BehaviorSubject.createDefault<Fuel>(Empty)
        // 주유 중인지 나타내는 스트림
        val obFillActive = BehaviorSubject.createDefault<Fuel>(Empty).apply {
            filter {
                it == Empty
            }.subscribe({
                obLiter.onNext("Liter")
                obDollars.onNext("Dollars")
                et_fuel1.isSelected = false
                et_fuel2.isSelected = false
                et_fuel3.isSelected = false
            }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        val obFuelDown = obFuel1Down.mergeWith(obFuel2Down).mergeWith(obFuel3Down)
        // 노즐을 내려놓았을 때 스트림.
        val obNozzleDown = obFuelDown.withLatestFrom<Fuel, Fuel>(obFuelFlowing,
                BiFunction { fuel, fActive ->
                    when (fuel) {
                        fActive -> fuel
                        else -> Empty
                    }
                }).filter {
            it != Empty
        }

        val obFuelLift = obFuel1Lift.mergeWith(obFuel2Lift).mergeWith(obFuel3Lift)
        val obNozzleUp = obFuelLift.withLatestFrom<Fuel, Fuel>(obFuelFlowing,
                BiFunction { fuel, fActive ->
                    when (fActive) {
                        is Empty -> fuel
                        else -> Empty
                    }
                }).filter {
            it != Empty
        }

        val obPulses = Observable.interval(200, TimeUnit.MILLISECONDS)
                .withLatestFrom<Fuel, Fuel>(obFuelFlowing,
                        BiFunction { _, t2 ->
                            t2
                        }).filter {
            it != Empty
        }.map { FuelPulses }

        val obCali = BehaviorSubject.createDefault(Calibration)
        val obTotal = BehaviorSubject.createDefault(0)

        val phase = BehaviorSubject.createDefault(Phase.IDLE)
        val obClearSale = BehaviorSubject.create<Fuel>()

        val obStart = obNozzleUp.withLatestFrom<Phase, Fuel>(phase,
                BiFunction { nozzle, _phase ->
                    Log.d(TAG, "[obStart] nozzle: $nozzle, _phase: $_phase")
                    when(_phase) {
                        Phase.IDLE -> nozzle
                        else -> Empty
                    }
                }).filter {
            it != Empty
        }.share()

        val obEnd = obNozzleDown.withLatestFrom<Phase, Fuel>(phase,
                BiFunction { nozzle, _phase ->
                    Log.d(TAG, "[obEnd] nozzle: $nozzle, _phase: $_phase")
                    when(_phase) {
                        Phase.FILLING -> nozzle
                        else -> Empty
                    }
                }).filter {
            it != Empty
        }.map { Empty }.share()

        // 결과를 확인하거나 주유가 시작할 때 상태를 스트림에 데이터를 emit
        obClearSale.mergeWith(obStart)
                .subscribe({
                    obFillActive.onNext(it)
                }, Throwable::printStackTrace)
                .apply {
                    disposables.add(this)
                }

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

        obStart.mergeWith(obEnd)
                .subscribe({
                    Log.d(TAG, "[obStart_obEnd] it: $it")
                    obFuelFlowing.onNext(it)
                }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        obStart.map { Phase.FILLING }
                .mergeWith(
                    obEnd.map { Phase.POS }
                )
                .mergeWith(
                    obClearSale.map { Phase.IDLE }
                ).subscribe({
            phase.onNext(it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        obEnd.withLatestFrom<String, String, String>(obDollars, obLiter,
                Function3 { _, dollar, liter ->
                    "Dollar: $dollar, \nliter: $liter"
                }).subscribe({
            showCompleteDialog(obClearSale, it)
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
                        else -> IllegalStateException("not selected fuel")
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

    private fun showCompleteDialog(obClearSale: Observer<Fuel>, msg: String)
        = AlertDialog.Builder(activity)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", { _, _ ->
                    obClearSale.onNext(Empty)
                })
                .create()
                .show()


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