package cn.numeron.brick.plugin

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.IOException

class ViewModelsGenerator(
    environment: SymbolProcessorEnvironment,
    private val classDeclaration: KSClassDeclaration
) {

    private val className = classDeclaration.simpleName.asString()
    private val classType = classDeclaration.asTypeName()

    private val parameters: List<ParameterSpec>
    private val properties: List<PropertySpec>

    init {
        classDeclaration.primaryConstructor!!.parameters
            .map {
                val parameterName = it.name!!.asString()
                val parameterType = it.asTypeName()
                val parameterSpec = ParameterSpec.builder(parameterName, parameterType)
                    .build()
                val propertySpec = PropertySpec.builder(parameterName, parameterType)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(parameterName)
                    .build()
                parameterSpec to propertySpec
            }
            .unzip()
            .let {
                parameters = it.first
                properties = it.second
            }

        val packageName = classDeclaration.packageName.asString()
        val classSimpleName = className + "s"
        //获取应该写入的文件流
        val writer = environment.codeGenerator.createNewFile(Dependencies(false), packageName, classSimpleName)
            .bufferedWriter()
        //创建Kt文件并写入到流
        FileSpec.builder(packageName, classSimpleName)
            .addFunction(generateLazyFunction())    //lazy方法
            .addFunction(generateGetFunction())     //get方法
            .addType(generateFactoryClass())        //ViewModelFactory
            .addType(generateLazyClass())           //LazyViewModel
            .build()
            .writeTo(writer)
        try {
            writer.close()
        } catch (e: IOException) {
        }
    }

    private fun generateLazyFunction(): FunSpec {
        val lazyParameterizedTypeName = LAZY_INTERFACE_TYPE_NAME.parameterizedBy(classType)
        classDeclaration.typeParameters.joinToString {
            it.name.asString()
        }
        val parameters = parameters.joinToString(transform = ParameterSpec::name).let {
            if (it.isEmpty()) it else ", $it"
        }
        return FunSpec.builder("lazy$className")
            .receiver(VIEW_MODEL_STORE_OWNER_TYPE_NAME)
            .addParameters(this.parameters)
            .returns(lazyParameterizedTypeName)
            .addStatement("return Lazy$className(this$parameters)")
            .build()
    }

    private fun generateLazyClass(): TypeSpec {

        val lazyClassName = "Lazy$className"

        val constructorFunSpec = FunSpec.constructorBuilder()
            .addParameter("owner", VIEW_MODEL_STORE_OWNER_TYPE_NAME)
            .addParameters(parameters)
            .build()

        val parameters = parameters.joinToString(transform = ParameterSpec::name)

        val valueGetterFunSpec = FunSpec.getterBuilder()
            .beginControlFlow("if(_value == null)")
            .addStatement("_value = owner.get$className(${parameters})")
            .endControlFlow()
            .addStatement("return _value!!")
            .build()

        val valuePropertySpec = PropertySpec.builder("value", classType)
            .addModifiers(KModifier.OVERRIDE)
            .getter(valueGetterFunSpec)
            .build()

        val valuePrivatePropertySpec = PropertySpec.builder("_value", classType.copy(true))
            .addModifiers(KModifier.PRIVATE)
            .initializer("null")
            .mutable()
            .build()

        val isInitializedFunSpec = FunSpec.builder("isInitialized")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Boolean::class.java)
            .addStatement("return _value != null")
            .build()

        val lazyParameterizedTypeName = LAZY_INTERFACE_TYPE_NAME.parameterizedBy(classType)

        val ownerPropertySpec = PropertySpec.builder("owner", VIEW_MODEL_STORE_OWNER_TYPE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .initializer("owner")
            .build()

        return TypeSpec.classBuilder(lazyClassName)
            .addProperty(valuePrivatePropertySpec)
            .addProperty(valuePropertySpec)
            .addFunction(isInitializedFunSpec)
            .primaryConstructor(constructorFunSpec)
            .addModifiers(KModifier.PRIVATE)
            .addProperty(ownerPropertySpec)
            .addProperties(properties)
            .addSuperinterface(lazyParameterizedTypeName)
            .build()
    }

    private fun generateGetFunction(): FunSpec {
        return FunSpec.builder("get$className")
            .addParameters(parameters)
            .receiver(VIEW_MODEL_STORE_OWNER_TYPE_NAME)
            .addStatement("val factory = ${className}Factory(${parameters.joinToString(transform = ParameterSpec::name)})")
            .addStatement("return %T(this, factory).get(%T::class.java)", VIEW_MODEL_PROVIDER_TYPE_NAME, classType)
            .returns(classType)
            .build()
    }

    private fun generateFactoryClass(): TypeSpec {

        val suppressAnnotationSpec = AnnotationSpec.builder(Suppress::class)
            .addMember("%S", "UNCHECKED_CAST")
            .build()

        val factoryClassName = className + "Factory"

        val typeVariableName = TypeVariableName("VM", VIEW_MODEL_TYPE_NAME)

        val parameterizedTypeName = CLASS_TYPE_NAME.parameterizedBy(typeVariableName)

        val createFunSpec = FunSpec.builder("create")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("clazz", parameterizedTypeName)
            .addAnnotation(suppressAnnotationSpec)
            .addTypeVariable(typeVariableName)
            .returns(typeVariableName)
            .addStatement("return ${className}(${parameters.joinToString(transform = ParameterSpec::name)}) as VM")
            .build()

        val constructorSpec = FunSpec.constructorBuilder()
            .addParameters(parameters)
            .build()

        return TypeSpec.classBuilder(factoryClassName)
            .addSuperinterface(VIEW_MODEL_FACTORY_INTERFACE_TYPE_NAME)
            .addProperties(properties)
            .addModifiers(KModifier.PRIVATE)
            .primaryConstructor(constructorSpec)
            .addFunction(createFunSpec)
            .build()
    }

    companion object {

        val CLASS_TYPE_NAME = Class::class.asClassName()
        val LAZY_INTERFACE_TYPE_NAME = ClassName("kotlin", "Lazy")
        val VIEW_MODEL_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModel")
        val VIEW_MODEL_PROVIDER_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelProvider")
        val VIEW_MODEL_STORE_OWNER_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelStoreOwner")
        val VIEW_MODEL_FACTORY_INTERFACE_TYPE_NAME = ClassName("androidx.lifecycle", "ViewModelProvider", "Factory")

    }

}