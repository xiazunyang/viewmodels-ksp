package cn.numeron.brick.plugin

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*

class ViewModelsVisitor(private val environment: SymbolProcessorEnvironment) : KSVisitorVoid() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind == ClassKind.CLASS) {
            if(!classDeclaration.isAbstract()) {
                if (classDeclaration.hasSuperType(ViewModelsGenerator.VIEW_MODEL_TYPE_NAME.toString())) {
                    ViewModelsGenerator(environment, classDeclaration)
                }
            }
        }
    }

    override fun visitFile(file: KSFile, data: Unit) {
        file.declarations.forEach {
            it.accept(this, data)
        }
    }

}