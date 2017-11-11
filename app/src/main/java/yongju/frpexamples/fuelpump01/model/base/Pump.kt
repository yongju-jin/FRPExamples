package yongju.frpexamples.fuelpump01.model.base

/**
 * Created by yongju on 2017. 11. 4..
 */
interface Pump {
    fun create(input: Input): Output
}