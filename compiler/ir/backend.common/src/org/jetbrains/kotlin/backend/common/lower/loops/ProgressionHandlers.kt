/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.createIrCallMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.singleArgumentExtension
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SimpleType

/** Builds a [HeaderInfo] for progressions built using the `rangeTo` function. */
internal class RangeToHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<SimpleType>) :
    ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        dispatchReceiver { it != null && it.type.toKotlinType() in progressionElementTypes }
        fqName { it.pathSegments().last() == Name.identifier("rangeTo") }
        parameterCount { it == 1 }
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType) =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = call.dispatchReceiver!!,
                last = call.getValueArgument(0)!!,
                step = irInt(1)
            )
        }
}

/** Builds a [HeaderInfo] for progressions built using the `downTo` extension function. */
internal class DownToHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<SimpleType>) :
    ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.downTo"), progressionElementTypes)
        parameterCount { it == 1 }
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = call.extensionReceiver!!,
                last = call.getValueArgument(0)!!,
                step = irInt(-1)
            )
        }
}

/** Builds a [HeaderInfo] for progressions built using the `until` extension function. */
internal class UntilHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<SimpleType>) :
    ProgressionHandler {
    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.until"), progressionElementTypes)
        parameterCount { it == 1 }
        parameter(0) { it.type.toKotlinType() in progressionElementTypes }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = call.extensionReceiver!!,
                last = call.getValueArgument(0)!!,
                step = irInt(1),
                isLastInclusive = false,
                canOverflow = false
            )
        }
}

/** Builds a [HeaderInfo] for progressions built using the `indices` extension property. */
internal class IndicesHandler(val context: CommonBackendContext) : ProgressionHandler {

    override val matcher = SimpleCalleeMatcher {
        // TODO: Handle Collection<*>.indices
        // TODO: Handle CharSequence.indices
        extensionReceiver { it != null && KotlinBuiltIns.isArrayOrPrimitiveArray(it.type.toKotlinType()) }
        fqName { it == FqName("kotlin.collections.<get-indices>") }
        parameterCount { it == 0 }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            // `last = array.size` for the loop `for (i in array.indices)`.
            val arraySizeProperty = call.extensionReceiver!!.type.getClass()!!.properties.first { it.name.asString() == "size" }
            val last = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = call.extensionReceiver
            }

            ProgressionHeaderInfo(
                data,
                first = irInt(0),
                last = last,
                step = irInt(1),
                isLastInclusive = false,
                canOverflow = false  // Cannot overflow because `last` is at most MAX_VALUE - 1
            )
        }
}

// TODO: Handle Array.reversed()
internal class ReversedProgressionHandler(context: CommonBackendContext, val visitor: IrElementVisitor<HeaderInfo?, Nothing?>) :
    ProgressionHandler {

    private val symbols = context.ir.symbols

    override val matcher = SimpleCalleeMatcher {
        fqName { it == FqName("kotlin.ranges.reversed") }
        extensionReceiver { it != null && it.type.toKotlinType() in symbols.progressionClassesTypes }
        parameterCount { it == 0 }
    }

    override fun build(call: IrCall, data: ProgressionType): HeaderInfo? {
        // Get HeaderInfo from underlying progression.
        val nestedInfo = call.extensionReceiver!!.accept(visitor, null)
                as? ProgressionHeaderInfo ?: return null

        // Swap first and last (and their inclusiveness), and negate step.
        return ProgressionHeaderInfo(
            data,
            first = nestedInfo.last,
            last = nestedInfo.first,
            step = nestedInfo.step.negate(),
            isFirstInclusive = nestedInfo.isLastInclusive,
            isLastInclusive = nestedInfo.isFirstInclusive,
            isReversed = !nestedInfo.isReversed,
            canOverflow = null,  // Value from underlying progression can't be used since the bounds changed.
            additionalVariables = nestedInfo.additionalVariables
        )
    }

    /** Return the negated value if the expression is const, otherwise call unaryMinus(). */
    private fun IrExpression.negate(): IrExpression {
        val stepValue = (this as? IrConst<*>)?.value as? Number
        return when (stepValue) {
            is Int -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, -stepValue)
            is Long -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, -stepValue)
            else -> {
                val unaryMinusFun = type.getClass()!!.functions.first { it.name.asString() == "unaryMinus" }
                IrCallImpl(startOffset, endOffset, type, unaryMinusFun.symbol, unaryMinusFun.descriptor).apply {
                    dispatchReceiver = this@negate
                }
            }
        }
    }
}

/** Builds a [HeaderInfo] for arrays. */
internal class ArrayIterationHandler(private val context: CommonBackendContext) : HeaderInfoHandler<Nothing?> {

    override val matcher = createIrCallMatcher {
        origin { it == IrStatementOrigin.FOR_LOOP_ITERATOR }
        // TODO: Support rare cases like `T : IntArray`
        dispatchReceiver { it != null && KotlinBuiltIns.isArrayOrPrimitiveArray(it.type.toKotlinType()) }
    }

    override fun build(call: IrCall, data: Nothing?): HeaderInfo? =
        with(context.createIrBuilder(call.symbol, call.startOffset, call.endOffset)) {
            // Consider the case like:
            //
            //   for (elem in A) { f(elem) }`
            //
            // If we lower it to:
            //
            //   for (i in A.indices) { f(A[i]) }
            //
            // ...then we will break program behaviour if `A` is an expression with side-effect. Instead, we lower it to:
            //
            //   val a = A
            //   for (i in a.indices) { f(a[i]) }
            //
            // This also ensures that the semantics of re-assignment of array variables used in the loop is consistent with the semantics
            // proposed in https://youtrack.jetbrains.com/issue/KT-21354.
            val arrayReference = scope.createTemporaryVariable(
                call.dispatchReceiver!!, nameHint = "array",
                origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
            )

            // `last = array.size` for the loop `for (i in array.indices)`.
            val arraySizeProperty = arrayReference.type.getClass()!!.properties.first { it.name.asString() == "size" }
            val last = irCall(arraySizeProperty.getter!!).apply {
                dispatchReceiver = irGet(arrayReference)
            }

            ArrayHeaderInfo(
                first = irInt(0),
                last = last,
                step = irInt(1),
                arrayVariable = arrayReference
            )
        }
}