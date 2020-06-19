package bachelor.test.locationapp.view

import bachelor.test.locationapp.presenter.BasePresenter
import bachelor.test.locationapp.presenter.InputData
import bachelor.test.locationapp.presenter.LocationData

interface MainScreenContract {
    interface View : BaseView<Presenter> {
        fun showPosition(locationData: LocationData)
        fun enableConnectButton(enabled: Boolean)
        fun swapStartButton(start: Boolean)
        fun showMessage(message: String?)
        fun showRecordingDialog()
        fun showRecordStopScreen()
        fun dismissRecordStopScreen()
    }

    interface Presenter: BasePresenter{
        fun start()
        fun stop()
        fun onConnectClicked()
        fun onDisconnectClicked()
        fun onStartClicked()
        fun onStopClicked()
        fun onRegularDataTransferStart()
        fun onRecordStartClicked(inputData: InputData)
        fun onRecordStopClicked()
        fun onTimerDone()
    }
}