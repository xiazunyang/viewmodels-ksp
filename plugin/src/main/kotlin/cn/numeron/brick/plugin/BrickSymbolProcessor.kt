package cn.numeron.brick.plugin

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

class BrickSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val instanceOfTypeVisitor = ViewModelsVisitor(environment)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getNewFiles().forEach {
            it.accept(instanceOfTypeVisitor, Unit)
        }
        return emptyList()
    }

}