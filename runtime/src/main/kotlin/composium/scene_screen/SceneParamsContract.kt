package oleginvoke.com.composium.scene_screen

import oleginvoke.com.composium.ParamDescriptor
import oleginvoke.com.composium.ParamOption

internal data class SceneParamsState(
    val params: List<ParamDescriptor>,
)

internal interface SceneParamsCallbacks {
    fun onParamActivationChange(paramName: String, isActive: Boolean)
    fun onBooleanParamChange(paramName: String, value: Boolean)
    fun onStringParamChange(paramName: String, value: String)
    fun onObjectParamChange(paramName: String, option: ParamOption<Any>)
}
