package com.example.floatingweb.helpers

import android.webkit.CookieManager
import android.util.Log
import android.content.Context

object CookieHelper {

    fun saveCookiesForUser(context: Context, user: String, domain: String) {
        try {
            val cm = CookieManager.getInstance()
            val cookies = cm.getCookie(domain) ?: return
//            DataStorage.saveString(context, "cookies_$user", cookies)
            Log.d("CookieHelper", "Cookies saved for $user")
        } catch (e: Exception) {
            Log.e("CookieHelper", "Save failed: ${e.message}")
        }
    }

    fun loadCookiesForUser(context: Context, user: String, domain: String) {
        try {
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            val cookies =
//                DataStorage.loadString(context, "cookies_$user")
//                    ?: return
//            cookies.split(";").forEach { cookie ->
//                cm.setCookie(domain, cookie.trim())
//            }
            cm.flush()
            Log.d("CookieHelper", "Cookies loaded for $user")
        } catch (e: Exception) {
            Log.e("CookieHelper", "Load failed: ${e.message}")
        }
    }

    fun clearCookies() {
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        cm.flush()
    }
}
