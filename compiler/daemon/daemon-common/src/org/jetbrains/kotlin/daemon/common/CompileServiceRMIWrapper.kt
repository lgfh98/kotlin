/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.daemon.common.impls.*
import java.io.File

class CompileServiceClientRMIWrapper(
    val asyncCompileService: CompileServiceAsync
) : CompileService {
    // deprecated methods :
    override fun remoteCompile(
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        servicesFacade: CompilerCallbackServicesFacade,
        compilerOutputStream: RemoteOutputStream,
        outputFormat: CompileService.OutputFormat,
        serviceOutputStream: RemoteOutputStream,
        operationsTracer: RemoteOperationsTracer?
    ): CompileService.CallResult<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remoteIncrementalCompile(
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        servicesFacade: CompilerCallbackServicesFacade,
        compilerOutputStream: RemoteOutputStream,
        compilerOutputFormat: CompileService.OutputFormat,
        serviceOutputStream: RemoteOutputStream,
        operationsTracer: RemoteOperationsTracer?
    ): CompileService.CallResult<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun leaseReplSession(
        aliveFlagPath: String?,
        targetPlatform: CompileService.TargetPlatform,
        servicesFacade: CompilerCallbackServicesFacade,
        templateClasspath: List<File>,
        templateClassName: String,
        scriptArgs: Array<out Any?>?,
        scriptArgsTypes: Array<out Class<out Any>>?,
        compilerMessagesOutputStream: RemoteOutputStream,
        evalOutputStream: RemoteOutputStream?,
        evalErrorStream: RemoteOutputStream?,
        evalInputStream: RemoteInputStream?,
        operationsTracer: RemoteOperationsTracer?
    ): CompileService.CallResult<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remoteReplLineCheck(sessionId: Int, codeLine: ReplCodeLine): CompileService.CallResult<ReplCheckResult> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remoteReplLineCompile(
        sessionId: Int,
        codeLine: ReplCodeLine,
        history: List<ReplCodeLine>?
    ): CompileService.CallResult<ReplCompileResult> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remoteReplLineEval(
        sessionId: Int,
        codeLine: ReplCodeLine,
        history: List<ReplCodeLine>?
    ): CompileService.CallResult<ReplEvalResult> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // normal methods:
    override fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        compilationResults: CompilationResults?
    ) = runBlocking {
        asyncCompileService.compile(
            sessionId,
            compilerArguments,
            compilationOptions,
            servicesFacade.toClient(),
            compilationResults?.toClient() // TODO
        )
    }


    override fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        templateClasspath: List<File>,
        templateClassName: String
    ) = runBlocking {
        asyncCompileService.leaseReplSession(
            aliveFlagPath,
            compilerArguments,
            compilationOptions,
            servicesFacade.toClient(),
            templateClasspath,
            templateClassName
        )
    }

    override fun replCreateState(sessionId: Int) = runBlocking {
        asyncCompileService.replCreateState(sessionId)
    }.toRMI()

    override fun getUsedMemory() = runBlocking {
        asyncCompileService.getUsedMemory()
    }

    override fun getDaemonOptions() = runBlocking {
        asyncCompileService.getDaemonOptions()
    }

    override fun getDaemonInfo() = runBlocking {
        asyncCompileService.getDaemonInfo()
    }


    override fun getDaemonJVMOptions() = runBlocking {
        asyncCompileService.getDaemonJVMOptions()
    }

    override fun registerClient(aliveFlagPath: String?) = runBlocking {
        asyncCompileService.registerClient(aliveFlagPath)
    }

    override fun getClients() = runBlocking {
        asyncCompileService.getClients()
    }


    override fun leaseCompileSession(aliveFlagPath: String?) = runBlocking {
        asyncCompileService.leaseCompileSession(aliveFlagPath)
    }


    override fun releaseCompileSession(sessionId: Int) = runBlocking {
        asyncCompileService.releaseCompileSession(sessionId)
    }


    override fun shutdown() = runBlocking {
        asyncCompileService.shutdown()
    }


    override fun scheduleShutdown(graceful: Boolean) = runBlocking {
        asyncCompileService.scheduleShutdown(graceful)
    }

    override fun clearJarCache() = runBlocking {
        asyncCompileService.clearJarCache()
    }


    override fun releaseReplSession(sessionId: Int) = runBlocking {
        asyncCompileService.releaseReplSession(sessionId)
    }


    override fun replCheck(sessionId: Int, replStateId: Int, codeLine: ReplCodeLine) = runBlocking {
        asyncCompileService.replCheck(sessionId, replStateId, codeLine)
    }

    override fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ) = runBlocking {
        asyncCompileService.replCompile(sessionId, replStateId, codeLine)
    }

    override fun checkCompilerId(expectedCompilerId: CompilerId) = runBlocking {
        asyncCompileService.checkCompilerId(expectedCompilerId)
    }

}
fun CompileServiceAsync.toRMI() = CompileServiceClientRMIWrapper(this)