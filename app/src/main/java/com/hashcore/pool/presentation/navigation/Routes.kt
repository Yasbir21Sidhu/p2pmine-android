package com.hashcore.pool.presentation.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BIOMETRIC_LOCK = "biometric_lock"
    const val MAIN = "main"
    const val HOME = "home"
    const val WORKERS = "workers"
    const val WORKER_DETAIL = "workers/{workerId}"
    const val EARNINGS = "earnings"
    const val PAYOUTS = "payouts"
    const val ACCOUNT = "account"
    const val CHANGE_PASSWORD = "change_password"
    const val TWO_FA = "two_fa"
    const val OBSERVER_LINKS = "observer_links"

    fun workerDetail(id: String) = "workers/$id"
}
