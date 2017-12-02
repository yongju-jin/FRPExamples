package yongju.frpexamples.fuelpump

import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.widget.checkedChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_fuelpump.*
import yongju.frpexamples.R.layout.fragment_fuelpump
import yongju.frpexamples.base.BaseFragment
import yongju.frpexamples.fuelpump.model.Fuel.Empty
import yongju.frpexamples.fuelpump.model.Fuel
import java.util.concurrent.TimeUnit

/**
 * Created by yongju on 2017. 11. 12..
 */
class AccumulatePulsesPump : BaseFragment() {
    private val TAG = "AccumulatePulsesPump"

    override val layoutId: Int = fragment_fuelpump

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        // liter text view 에 표시될 data의 stream.
        val obLiter = BehaviorSubject.create<String>().apply {
                observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                    tv_liters_count.text = it
                }, Throwable::printStackTrace).apply {
                    disposables.add(this)
                }
        }

        // toggle button이 변경되었을 때 event의 stream.
        val obFuel1 = tb_fuel1.checkedChanges().skip(1).share()
        val obFuel2 = tb_fuel2.checkedChanges().skip(1).share()
        val obFuel3 = tb_fuel3.checkedChanges().skip(1).share()

        // toggle button이 off 되었을 때의 event stream.
        val obFuel1Down = whenFuelDown(obFuel1, Fuel.Fuel1)
        val obFuel2Down = whenFuelDown(obFuel2, Fuel.Fuel2)
        val obFuel3Down = whenFuelDown(obFuel3, Fuel.Fuel3)

        // toggle button이 on 되었을 떄의 event stream.
        val obFuel1Lift = whenFuelLift(obFuel1, Fuel.Fuel1)
        val obFuel2Lift = whenFuelLift(obFuel2, Fuel.Fuel2)
        val obFuel3Lift = whenFuelLift(obFuel3, Fuel.Fuel3)

        // 현재 주유 상태 data의 stream.
        val fillActive = BehaviorSubject.createDefault<Fuel>(Empty)

        // 세 가지의 toggle button 이 off 되었을 때 stream 을 합침.
        // stream의 data로 fuel의 종류가 나와서 어떤 종류의 fuel이 off 되었는지 알 수 있음.
        val obFuelDown = obFuel1Down.mergeWith(obFuel2Down).mergeWith(obFuel3Down)
        // 주유가 끝났음을 알려주는 event stream.
        // obFuelDown이 event를 emit 할 때 fillActive stream의 마지막 data를 가져와서
        // 주유 중일 경우에만 event를 emit.
        val obEnd = obFuelDown.withLatestFrom<Fuel, Fuel>(fillActive,
                BiFunction { fuel, fActive ->
                    when (fuel) {
                        fActive -> Empty
                        else -> fuel
                    }
                }).filter {
            it == Empty
        }

        // 세 가지의 toggle button 이 on 되었을 때 stream 을 합침.
        // stream의 data로 fuel의 종류가 나와서 어떤 종류의 fuel이 on 되었는지 알 수 있음.
        val obFuelLift = obFuel1Lift.mergeWith(obFuel2Lift).mergeWith(obFuel3Lift)
        // 주유의 시작을 알려주는 event stream.
        // obFuelLift가 event를 emit 할 때 fillActive stream의 마지막 data를 가져와서
        // 주유 중이 아닐 경우에만 event를 emit
        val obStart = obFuelLift.withLatestFrom<Fuel, Fuel>(fillActive,
                BiFunction { fuel, fActive ->
                    when (fActive) {
                        is Empty -> fuel
                        else -> Empty
                    }
                }).filter {
            it != Empty
        }

        // 200ms 마다 주기적으로 event를 emit 하는 stream.
        // 주유 중인 상태일 떄만 event를 emit
        // 200ms 마다 40만큼의 양이 주유 된다.
        val obPulses = Observable.interval(200, TimeUnit.MILLISECONDS)
                .withLatestFrom<Fuel, Fuel>(fillActive,
                    BiFunction { _, t2 ->
                    t2
                }).filter {
                    it != Empty
                }
                .map { FuelPulses }

        // calibration data의 stream.
        val obCali = BehaviorSubject.createDefault(Calibration)
        // 총 리터 수 data의 stream.
        val obTotal = BehaviorSubject.createDefault(0)

        // obStart stream에 event가 emit 된 경우는 0,
        // obPulses stream에 data가 emit 된 경우는 obTotal stream의 마지막 data에
        // obPulses에 emit된 data를 더한 data를 obTotal stream에 emit
        obStart.map { 0 }.mergeWith(
            obPulses.withLatestFrom<Int, Int>(obTotal,
                    BiFunction { pulse, total -> pulse + total
                    })
        ).subscribe({
            obTotal.onNext(it)
        }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        // obEnd, obStart stream 을 합쳐서
        // 주유 상태를 나타내는 stream에 event를 emit
        obEnd.mergeWith(obStart)
                .subscribe({
                    fillActive.onNext(it)
                }, Throwable::printStackTrace).apply {
            disposables.add(this)
        }

        // obCali, obTotal stream에 event가 emit 될 경우
        // 두 stream의 마지막 data의 곱을
        // calibration이 적용된 liter의 양을 표시하는 stream에 data를 emit
        Observable.combineLatest<Double, Int, Double>(obCali, obTotal,
                BiFunction { cali, total ->
                    cali * total})
                .subscribe({
                    obLiter.onNext(it.toString())
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