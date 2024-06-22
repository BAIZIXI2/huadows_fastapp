package top.niunaijun.blackboxa.view.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 23:33
 */
open class BaseViewModel : ViewModel() {

    fun launchOnUI(block: suspend CoroutineScope.() -> Unit) {
//        Log.d("nfh", "BaseViewModel.launchOnUI")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    block()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

}