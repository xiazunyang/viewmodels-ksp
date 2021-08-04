package cn.numeron.brick.plugin

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/** 判断一个[KSType]的声明是否可空 */
internal fun KSType.isNullable(): Boolean = nullability == Nullability.NULLABLE

/** 把[KSType]转成[TypeName] */
internal fun KSType.asTypeName(): TypeName {
    return declaration.asTypeName().copy(nullable = isNullable())
}

/** 把[KSReferenceElement]转成[TypeName] */
internal fun KSReferenceElement.asTypeName(isNullable: Boolean = false): TypeName {
    return when (this) {
        is KSCallableReference -> {
            val returnType = returnType.resolve().asTypeName()
            val receiverType = receiverType?.resolve()?.asTypeName()
            val parameters = functionParameters.map(KSValueParameter::type)
                .map(KSTypeReference::resolve)
                .map(KSType::asTypeName)
                .toTypedArray()
            LambdaTypeName
                .get(receiver = receiverType, parameters = parameters, returnType = returnType)
                .copy(nullable = isNullable)
        }
        is KSClassifierReference -> {
            val packageName = when (referencedName()) {
                "Iterable", "MutableIterable", "Collection", "MutableCollection",
                "List", "MutableList", "Set", "MutableSet", "Map", "MutableMap" -> "kotlin.collections"
                else -> "kotlin"
            }
            val className = ClassName(packageName, referencedName())
            if (typeArguments.isNotEmpty()) {
                return className
                    .parameterizedBy(typeArguments.map(KSTypeArgument::asTypeName))
                    .copy(nullable = isNullable)
            }
            className.copy(nullable = isNullable)
        }
        else -> throw IllegalStateException("Unsupported type: $this, class = $javaClass")
    }
}

internal fun KSTypeArgument.asTypeName(): TypeName {
    return type!!.asTypeName()
}

internal fun KSTypeReference.asTypeName(): TypeName {
    val element = element
    val ksType = resolve()
    if (element != null) {
        return element.asTypeName(ksType.isNullable())
    }
    return ksType.asTypeName()
}

/** 把[KSValueParameter]转成[TypeName] */
internal fun KSValueParameter.asTypeName(): TypeName {
    return type.asTypeName()
}

/** 把[KSDeclaration]转成[TypeName] */
internal fun KSDeclaration.asTypeName(): TypeName {
    val simpleNames = mutableListOf(simpleName.asString())
    var parent: KSDeclaration? = parentDeclaration
    while (parent != null && parent is KSClassDeclaration) {
        simpleNames.add(0, parent.simpleName.asString())
        parent = parent.parentDeclaration
    }
    val packageName = packageName.asString()
    val className = ClassName(packageName, simpleNames)
    if (typeParameters.isNotEmpty()) {
        //泛型
        val typeParameters = typeParameters.map(KSTypeParameter::asTypeName)
        return className.parameterizedBy(typeParameters)
    }
    return className
}

internal fun KSClassDeclaration.hasSuperType(superTypeQualifiedName: String): Boolean {
    superTypes.map(KSTypeReference::resolve).forEach {
        val ksDeclaration = it.declaration
        val classQualifiedName = ksDeclaration.packageName.asString() + "." + ksDeclaration.simpleName.asString()
        if (classQualifiedName == superTypeQualifiedName) {
            return true
        }
        if (ksDeclaration is KSClassDeclaration && ksDeclaration.classKind == ClassKind.CLASS) {
            return ksDeclaration.hasSuperType(superTypeQualifiedName)
        }
    }
    return false
}