/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.classId
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirClassUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.FirSuperTypeScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.jvm.JvmMappedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

fun ConeClassLikeLookupTagImpl.bindSymbolToLookupTag(provider: FirSymbolProvider, symbol: FirClassLikeSymbol<*>?) {
    boundSymbol = Pair(provider, symbol)
}

abstract class FirSymbolProvider : FirSessionComponent {

    abstract fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>?

    fun getSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): FirClassLikeSymbol<*>? {
        (lookupTag as? ConeClassLikeLookupTagImpl)
            ?.boundSymbol?.takeIf { it.first === this }?.let { return it.second }

        return getClassLikeSymbolByFqName(lookupTag.classId).also {
            (lookupTag as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(this, it)
        }
    }

    fun getSymbolByLookupTag(lookupTag: ConeClassifierLookupTag): FirClassifierSymbol<*>? {
        return when (lookupTag) {
            is ConeClassLikeLookupTag -> getSymbolByLookupTag(lookupTag)
            is ConeClassifierLookupTagWithFixedSymbol -> lookupTag.symbol
            else -> error("Unknown lookupTag type: ${lookupTag::class}")
        }
    }

    abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>>

    abstract fun getNestedClassifierScope(classId: ClassId): FirScope?

    fun buildDefaultUseSiteMemberScope(klass: FirClass<*>, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope {
        TODO("REMOVE")
    }

    open fun getAllCallableNamesInPackage(fqName: FqName): Set<Name> = emptySet()
    open fun getClassNamesInPackage(fqName: FqName): Set<Name> = emptySet()

    open fun getAllCallableNamesInClass(classId: ClassId): Set<Name> = emptySet()
    open fun getNestedClassesNamesInClass(classId: ClassId): Set<Name> = emptySet()

    abstract fun getPackage(fqName: FqName): FqName? // TODO: Replace to symbol sometime

    // TODO: should not retrieve session through the FirElement::session
    fun getSessionForClass(classId: ClassId): FirSession? = getClassLikeSymbolByFqName(classId)?.fir?.session

    companion object {
        fun getInstance(session: FirSession) = session.firSymbolProvider
    }
}

fun FirSymbolProvider.getClassDeclaredCallableSymbols(classId: ClassId, name: Name): List<FirCallableSymbol<*>> {
    val classSymbol = getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol ?: return emptyList()
    val declaredMemberScope = declaredMemberScope(classSymbol.fir)
    val result = mutableListOf<FirCallableSymbol<*>>()
    val processor: (FirCallableSymbol<*>) -> ProcessorAction = {
        result.add(it)
        ProcessorAction.NEXT
    }

    declaredMemberScope.processFunctionsByName(name, processor)
    declaredMemberScope.processPropertiesByName(name, processor)

    return result
}

