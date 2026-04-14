package com.voicewolf.app

/**
 * Represents a game setup/configuration with predefined roles
 */
data class GameSetup(
    val name: String,              // 版型名称
    val goodRoles: List<SetupRole>, // 好人阵营角色
    val evilRoles: List<SetupRole>  // 狼人阵营角色
) {
    // 获取所有角色（用于角色选择弹窗）
    fun getAllRoles(): List<SetupRole> = goodRoles + evilRoles

    // 获取版型显示摘要（好人阵营角色列表）
    fun getGoodRolesSummary(): String = goodRoles.map { "${it.role.getMarkedRoleDisplayName()}${if (it.count > 1) it.count else ""}" }.joinToString("/")

    // 获取版型显示摘要（狼人阵营角色列表）
    fun getEvilRolesSummary(): String = evilRoles.map { "${it.role.getMarkedRoleDisplayName()}${if (it.count > 1) it.count else ""}" }.joinToString("/")

    // 判断版型是否包含某个角色
    fun hasRole(role: Player.MarkedRole): Boolean = getAllRoles().any { it.role == role }
}

/**
 * Represents a role in the setup with count
 */
data class SetupRole(
    val role: Player.MarkedRole,
    val count: Int
)

/**
 * Preset game setups
 */
object GameSetupPresets {
    // 狼王守卫版型
    val WOLF_KING_GUARD = GameSetup(
        name = "狼王守卫",
        goodRoles = listOf(
            SetupRole(Player.MarkedRole.SEER, 1),
            SetupRole(Player.MarkedRole.WITCH, 1),
            SetupRole(Player.MarkedRole.GUARD, 1),
            SetupRole(Player.MarkedRole.HUNTER, 1),
            SetupRole(Player.MarkedRole.VILLAGER, 4)
        ),
        evilRoles = listOf(
            SetupRole(Player.MarkedRole.WEREWOLF, 3),
            SetupRole(Player.MarkedRole.WOLF_KING, 1)
        )
    )

    // 预女猎白版型
    val SEER_WITCH_HUNTER_IDIOT = GameSetup(
        name = "预女猎白",
        goodRoles = listOf(
            SetupRole(Player.MarkedRole.SEER, 1),
            SetupRole(Player.MarkedRole.WITCH, 1),
            SetupRole(Player.MarkedRole.HUNTER, 1),
            SetupRole(Player.MarkedRole.IDIOT, 1),
            SetupRole(Player.MarkedRole.VILLAGER, 4)
        ),
        evilRoles = listOf(
            SetupRole(Player.MarkedRole.WEREWOLF, 4)
        )
    )

    // 狼美骑士版型
    val WOLF_BEAUTY_KNIGHT = GameSetup(
        name = "狼美骑士",
        goodRoles = listOf(
            SetupRole(Player.MarkedRole.SEER, 1),
            SetupRole(Player.MarkedRole.WITCH, 1),
            SetupRole(Player.MarkedRole.GUARD, 1),
            SetupRole(Player.MarkedRole.KNIGHT, 1),
            SetupRole(Player.MarkedRole.VILLAGER, 4)
        ),
        evilRoles = listOf(
            SetupRole(Player.MarkedRole.WEREWOLF, 3),
            SetupRole(Player.MarkedRole.WOLF_BEAUTY, 1)
        )
    )

    // 通灵师机械狼版型
    val SEER_MIRROR_MECH_WOLF = GameSetup(
        name = "通灵师机械狼",
        goodRoles = listOf(
            SetupRole(Player.MarkedRole.SEER_MIRROR, 1),
            SetupRole(Player.MarkedRole.WITCH, 1),
            SetupRole(Player.MarkedRole.HUNTER, 1),
            SetupRole(Player.MarkedRole.GUARD, 1),
            SetupRole(Player.MarkedRole.VILLAGER, 4)
        ),
        evilRoles = listOf(
            SetupRole(Player.MarkedRole.WEREWOLF, 3),
            SetupRole(Player.MarkedRole.MECHANICAL_WOLF, 1)
        )
    )

    // 所有预置版型列表
    val ALL = listOf(
        WOLF_KING_GUARD,
        SEER_WITCH_HUNTER_IDIOT,
        WOLF_BEAUTY_KNIGHT,
        SEER_MIRROR_MECH_WOLF
    )

    // 根据名称获取版型
    fun getByName(name: String): GameSetup? = ALL.find { it.name == name }
}