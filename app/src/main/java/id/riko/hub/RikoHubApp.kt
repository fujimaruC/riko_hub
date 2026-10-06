package id.riko.hub

import android.app.Application

class RikoHubApp : Application() {
    val container by lazy { AppContainer(this) }
}
