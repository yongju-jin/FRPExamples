package yongju.frpexamples.fuelpump.model

/**
 * Created by yongju on 2017. 12. 5..
 */
sealed class Speed {
    object FAST: Speed()
    object SLOW: Speed()
    object STOPPED: Speed()
}