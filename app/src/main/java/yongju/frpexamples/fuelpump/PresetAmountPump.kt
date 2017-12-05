package yongju.frpexamples.fuelpump

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
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
import kotlinx.android.synthetic.main.fragment_keypadpump.view.*
import yongju.frpexamples.R.layout.fragment_fuelpump
import yongju.frpexamples.R.layout.fragment_keypadpump
import yongju.frpexamples.base.BaseFragment
import yongju.frpexamples.fuelpump.model.Fuel
import yongju.frpexamples.fuelpump.model.Phase
import yongju.frpexamples.fuelpump.model.Speed
import java.util.concurrent.TimeUnit

/**
 * Created by yongju on 2017. 12. 3..
 */
class PresetAmountPump: BaseFragment() {
    private val TAG = "PresetAmountPump"

    override val layoutId: Int = fragment_fuelpump

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        val obPresetCount = BehaviorSubject.createDefault(0.0).apply {
            subscribe({
                tv_preset_count.text = it.toString()
            }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        // 주유 speed 스트림
        val obSpeed = BehaviorSubject.create<Speed>()

        // liter 스트림
        val obLiter = BehaviorSubject.create<Double>().apply {
            observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        tv_liters_count.text = it.toString()
                    }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        // 가격(Dollars)의 스트림
        val obDollars = BehaviorSubject.create<Double>().apply {
            observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        tv_dollars_count.text = it.toString()
                    },Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        // 가격 스트림
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

        // 노즐에서 흘러가는지 나타내는 스트림
        val obFuelFlowing = BehaviorSubject.createDefault<Fuel>(Fuel.Empty)

        // 주유 중인지 나타내는 스트림
        val obFillActive = BehaviorSubject.createDefault<Fuel>(Fuel.Empty).apply {
            filter {
                it == Fuel.Empty
            }.subscribe({
                obLiter.onNext(0.0)
                obDollars.onNext(0.0)
                obPresetCount.onNext(0.0)
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
                        else -> Fuel.Empty
                    }
                }).filter {
            it != Fuel.Empty
        }

        val obFuelLift = obFuel1Lift.mergeWith(obFuel2Lift).mergeWith(obFuel3Lift)
        val obNozzleUp = obFuelLift.withLatestFrom<Fuel, Fuel>(obFuelFlowing,
                BiFunction { fuel, fActive ->
                    when (fActive) {
                        is Fuel.Empty -> fuel
                        else -> Fuel.Empty
                    }
                }).filter {
            it != Fuel.Empty
        }

        val obPulses = Observable.interval(200, TimeUnit.MILLISECONDS)
                .withLatestFrom<Fuel, Fuel>(obFuelFlowing,
                        BiFunction { _, fuelFlowing ->
                            fuelFlowing
                        }).filter {
                    it != Fuel.Empty
                }.withLatestFrom(obSpeed, BiFunction <Fuel, Speed, Double>{ _, speed ->
                        Log.d(TAG, "speed: $speed")
                        when(speed) {
                            Speed.FAST -> FastFuelPulses.toDouble()
                            Speed.SLOW -> SlowFuelPulses.toDouble()
                            else ->  -1.0
                        }
                }).filter {
                    it > 0
                }

        val obCali = BehaviorSubject.createDefault(Calibration)
        val obTotal = BehaviorSubject.createDefault<Double>(0.0)

        val phase = BehaviorSubject.createDefault<Phase>(Phase.Idle)
        val obClearSale = BehaviorSubject.create<Fuel>()

        val obStart = obNozzleUp.withLatestFrom<Phase, Fuel>(phase,
                BiFunction { nozzle, _phase ->
//                    Log.d(TAG, "[obStart] nozzle: $nozzle, _phase: $_phase")
                    when(_phase) {
                        Phase.Idle -> nozzle
                        else -> Fuel.Empty
                    }
                }).filter {
            it != Fuel.Empty
        }.share()

        val obEnd = obNozzleDown.withLatestFrom<Phase, Fuel>(phase,
                BiFunction { nozzle, _phase ->
//                    Log.d(TAG, "[obEnd] nozzle: $nozzle, _phase: $_phase")
                    when(_phase) {
                        Phase.Filling -> nozzle
                        else -> Fuel.Empty
                    }
                }).filter {
            it != Fuel.Empty
        }.map { Fuel.Empty }.share()

        // 결과를 확인하거나 주유가 시작할 때 상태를 스트림에 데이터를 emit
        obClearSale.mergeWith(obStart)
                .subscribe({
                    obFillActive.onNext(it)
                }, Throwable::printStackTrace)
                .apply {
                    disposables.add(this)
                }

        obStart.map { 0.0 }.mergeWith(
                obPulses.withLatestFrom<Double, Double>(obTotal,
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
//                    Log.d(TAG, "[obStart_obEnd] it: $it")
                    obFuelFlowing.onNext(it)
                }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        obStart.map<Phase>(
                { Phase.Filling }
        )
                .mergeWith(
                        obEnd.map { Phase.Pos }
                )
                .mergeWith(
                        obClearSale.map { Phase.Idle}
                ).subscribe({
            phase.onNext(it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        obEnd.withLatestFrom<Double, Double, String>(obDollars, obLiter,
                Function3 { _, dollar, liter ->
                    "Dollar: $dollar, \nliter: $liter"
                }).subscribe({
            showCompleteDialog(obClearSale, it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        val obCaliTotal = Observable.combineLatest<Double, Double, Double>(obCali, obTotal,
                BiFunction { cali, total ->
                    cali * total
                }).share()

        obCaliTotal.subscribe({
            obLiter.onNext(it)
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
                    obDollars.onNext(it)
                }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        Observable.combineLatest<Double, Double, Double, Double, Speed>(obPresetCount, obPrice, obDollars, obLiter,
                Function4 { preset, price, dollars, liters ->
                    Log.d(TAG, "preset: $preset, price: $price, dollars: $dollars, liters: $liters")
                    if (preset < 0.1) Speed.FAST
                    else {
                        if (dollars>= preset) Speed.STOPPED
                        else {
                            val slowLiters = preset / price - 0.10
                            if (liters >= slowLiters) Speed.SLOW
                            else Speed.FAST
                        }
                    }
                }).subscribe({
            Log.d(TAG, "it: $it")
            obSpeed.onNext(it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        // show preset alertdialog
        tv_preset_name.clicks().mergeWith(
            tv_preset_count.clicks()
        ).withLatestFrom<Fuel, Fuel>(obFillActive,
            BiFunction { _, fillActive ->
                fillActive
            }
        ).filter {
            it == Fuel.Empty
        }.subscribe({
            showKeypadPumpFragment(obPresetCount)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }
    }

    private fun showKeypadPumpFragment(presetCount: Observer<Double>) =
        AlertDialog.Builder(activity)
                .setTitle("Preset")
                .setView(presetDialogView(presetCount))
                .setPositiveButton(android.R.string.ok, null)
                .create().show()

    private fun presetDialogView(presetCount: Observer<Double>): View {
        val view = layoutInflater.inflate(fragment_keypadpump, null)
        val value = BehaviorSubject.createDefault(0.0).apply {
            subscribe({
                view.tv_preset_name.text = it.toString()
                presetCount.onNext(it)
            }, Throwable::printStackTrace).apply {
                disposables.add(this)
            }
        }

        view.btn_0.clicks().map { 0 }.mergeWith(
        view.btn_1.clicks().map { 1 }).mergeWith(
        view.btn_2.clicks().map { 2 }).mergeWith(
        view.btn_3.clicks().map { 3 }).mergeWith(
        view.btn_4.clicks().map { 4 }).mergeWith(
        view.btn_5.clicks().map { 5 }).mergeWith(
        view.btn_6.clicks().map { 6 }).mergeWith(
        view.btn_7.clicks().map { 7 }).mergeWith(
        view.btn_8.clicks().map { 8 }).mergeWith(
        view.btn_9.clicks().map { 9 }).withLatestFrom<Double, Double>(value, BiFunction { number, _value ->
            val x10 = _value * 10.0
            when {
                x10 >= 1000.0 -> -1.0
                else -> x10 + number
            }
        }).filter {
            it > 0
        }.mergeWith(
                view.btn_clear.clicks().map { 0.0 }
        ).subscribe({
            value.onNext(it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }
        return view
    }

    private fun showCompleteDialog(obClearSale: Observer<Fuel>, msg: String)
            = AlertDialog.Builder(activity)
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("OK", { _, _ ->
                obClearSale.onNext(Fuel.Empty)
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
        val SlowFuelPulses = 2
        val FastFuelPulses = 40
        val Calibration = 0.001
    }
}