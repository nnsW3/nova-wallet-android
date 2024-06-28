package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.custom

data class ConnectionStateModel(
    val name: String?,
    val chainStatusColor: Int?,
    val chainStatusIcon: Int,
    val chainStatusIconColor: Int?,
    val showShimmering: Boolean
)
