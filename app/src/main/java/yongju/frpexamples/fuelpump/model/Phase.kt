package yongju.frpexamples.fuelpump.model

/**
 * Created by yongju on 2017. 11. 25..
 */
sealed class Phase {
    object Idle: Phase()
    object Filling: Phase()
    object Pos: Phase()
}