package model.state

import model.Context
import model.api.controller.RequestController

/*
    Общий корень для пользовательских состояний.
*/
abstract class GeneralState(
    protected val context: Context,
    protected val requestController: RequestController
)