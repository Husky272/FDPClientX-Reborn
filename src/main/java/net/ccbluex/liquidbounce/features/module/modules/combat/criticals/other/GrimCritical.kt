package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.other

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.CriticalMode

class GrimCritical : CriticalMode("Grim") {
    private var attacks = 0
    override fun onEnable() {
        attacks = 0
    }
    override fun onAttack(event: AttackEvent) {
        attacks++
        if (attacks > 3) {
            if (!mc.thePlayer.onGround) {
                critical.sendCriticalPacket(yOffset = 0.110314, ground = false)
                critical.sendCriticalPacket(yOffset = 0.0200081, ground = false)
                critical.sendCriticalPacket(yOffset = 0.00000001300009, ground = false)
                critical.sendCriticalPacket(yOffset = 0.000000000022, ground = false)
                critical.sendCriticalPacket(ground = true)
                attacks = 0
            }
        } else {
            critical.antiDesync = false
        }
    }
}