package oleginvoke.com.composium

internal data class ParamActivation(
    val isActive: Boolean,
)

internal sealed interface ParamDescriptor {
    val name: String
    val typeName: String
    val activation: ParamActivation?
}

internal data class BooleanParamDescriptor(
    override val name: String,
    override val typeName: String,
    override val activation: ParamActivation?,
    val value: Boolean,
) : ParamDescriptor

internal data class StringParamDescriptor(
    override val name: String,
    override val typeName: String,
    override val activation: ParamActivation?,
    val value: String,
) : ParamDescriptor

internal data class ObjectParamDescriptor(
    override val name: String,
    override val typeName: String,
    override val activation: ParamActivation?,
    val selectedOption: ParamOption<Any?>,
    val options: List<ParamOption<Any>>,
) : ParamDescriptor
