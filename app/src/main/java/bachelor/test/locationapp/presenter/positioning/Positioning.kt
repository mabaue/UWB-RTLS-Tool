package bachelor.test.locationapp.presenter.positioning

interface Positioning {
    fun startIMU()
    fun stopIMU()
    fun calculateLocation(byteArray: ByteArray)
}