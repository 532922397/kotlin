/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.text


//@library
public interface Appendable {
    public open fun append(csq: CharSequence?): Appendable
    public open fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    public open fun append(c: Char): Appendable
}

/*@library
public class StringBuilder1(content: String = "") : Appendable, CharSequence {
    override val length: Int = noImpl
    override fun get(index: Int): Char = noImpl
    override fun subSequence(start: Int, end: Int): CharSequence = noImpl
    override fun append(c: Char): StringBuilder = noImpl
    override fun append(csq: CharSequence?): StringBuilder = noImpl
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder = noImpl
    public fun append(obj: Any?): StringBuilder = noImpl
    public fun reverse(): StringBuilder = noImpl
    override fun toString(): String = noImpl
}*/

/*
public inline fun StringBuilder(capacity: Int): StringBuilder = StringBuilder()
public inline fun StringBuilder(content: CharSequence): StringBuilder = StringBuilder(content.toString())
*/


public class StringBuilder(content: String = "") : Appendable, CharSequence {

    private var string: String = content

    public constructor(capacity: Int) : this()
    public constructor(content: CharSequence) : this(content.toString())

    override val length: Int get() = string.length
    override fun get(index: Int): Char = string[index]

    override fun append(c: Char): StringBuilder = apply { string += c }
    public fun append(str: String): StringBuilder = apply { string += str }
    override fun append(csq: CharSequence?): StringBuilder = append(csq.toString())
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder = append(csq.toString().subSequence(start, end))
    public fun append(obj: Any?): StringBuilder = append(obj.toString())
    public fun append(chars: CharArray): StringBuilder = append(stringFromChars(chars))
    public fun append(chars: CharArray, offset: Int, count: Int): StringBuilder =
            append(stringFromChars(chars, offset, count))



    public fun delete(startIndex: Int, endIndex: Int): StringBuilder = apply {
        // TODO: check range
        string = string.substring(0, startIndex) + string.substring(endIndex, string.length)
    }
    public fun deleteCharAt(index: Int): StringBuilder = delete(index, index + 1)

    public fun replace(startIndex: Int, endIndex: Int, str: String): StringBuilder = apply {
        // TODO: check range
        string = string.substring(0, startIndex) + str + string.substring(endIndex, string.length)
    }

    public fun setCharAt(index: Int, char: Char): Unit {
        replace(index, index + 1, char.toString())
    }


    public fun insert(index: Int, str: String) = if (index == length) append(str) else replace(index, index, str)
    public fun insert(index: Int, obj: Any?) = insert(index, obj.toString())
    public fun insert(index: Int, csq: CharSequence?, startIndex: Int, endIndex: Int) =
            insert(index, csq.toString().substring(startIndex, endIndex))
    public fun insert(index: Int, chars: CharArray) = insert(index, stringFromChars(chars))
    public fun insert(index: Int, chars: CharArray, offset: Int, count: Int) =
            insert(index, stringFromChars(chars, offset, count))



    public fun substring(startIndex: Int, endIndex: Int = length): String = string.substring(startIndex, endIndex)
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = substring(startIndex, endIndex)

    public fun reverse(): StringBuilder = apply {
        var result = ""
        var index = string.length
        while (--index >= 0) {
            result += string[index]
        }
        string = result
    }

    override fun toString(): String = string

    // TODO: provide as string constructors
    private fun stringFromChars(chars: CharArray): String = chars.asDynamic().join("")
    private fun stringFromChars(chars: CharArray, offset: Int, count: Int): String {
        // TODO: check range
        var result = ""
        for (i in offset..offset + count - 1) {
            result += chars[i]
        }
        return result
    }
}