// FILE: a.kt
package a

interface A
interface B : A

private fun validFun() {}
private val validVal = 1

private fun invalidFun0() {}
private val invalidProp0 = 1

// NB invalidFun0 and invalidProp0 are conflicting overloads, since the following is an ambiguity:
fun useInvalidFun0() = invalidFun0()
fun useInvalidProp0() = invalidProp0

private fun invalidFun1() {}
private fun invalidFun1() {}

private fun invalidFun2() {}
public fun invalidFun2() {}

public fun invalidFun3() {}

private fun invalidFun4() {}
public fun invalidFun4() {}

public fun validFun2(a: A) = a
public fun validFun2(b: B) = b

// FILE: b.kt
package a

private fun validFun() {}
private val validVal = 1

private fun invalidFun0() {}

private val invalidProp0 = 1

internal fun invalidFun3() {}
internal fun invalidFun4() {}

// FILE: c.kt
package a

public fun invalidFun0() {}

public val invalidProp0 = 1