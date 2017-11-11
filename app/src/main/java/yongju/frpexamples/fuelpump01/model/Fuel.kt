package yongju.frpexamples.fuelpump01.model

/**
 * Created by yongju on 2017. 11. 8..
 */
sealed class Fuel {
    class Fuel1(val name: String): Fuel()
    class Fuel2(val name: String): Fuel()
    class Fuel3(val name: String): Fuel()
}

object Empty: Fuel()