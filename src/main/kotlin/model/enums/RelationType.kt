package model.enums


import kotlinx.serialization.Serializable

@Serializable
enum class RelationType {
    MENTOR,
    DISCIPLE,
    RIVAL,
    ARCH_ENEMY,
    BONDED_ALLY,
    CONTRACT_HOLDER,
    CREATOR,
    FRIEND,
    SIBLING,
    FAMILY
}