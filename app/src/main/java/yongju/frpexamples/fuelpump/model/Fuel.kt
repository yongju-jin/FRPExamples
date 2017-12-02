package yongju.frpexamples.fuelpump.model

/**
 * Created by yongju on 2017. 11. 8..
 */
sealed class Fuel {
    object Fuel1: Fuel()
    object Fuel2: Fuel()
    object Fuel3: Fuel()
    object Empty: Fuel()
}