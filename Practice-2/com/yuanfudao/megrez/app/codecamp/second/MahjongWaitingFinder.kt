/**
 * @(#)Solution1.java, 7月 15, 2022.
 *
 *
 * Copyright 2022 yuanfudao.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.yuanfudao.megrez.app.codecamp.second

import java.lang.IllegalArgumentException
import java.util.ArrayList

object MahjongWaitingFinder {
    private val allCards =
        Character.values().map { it.num + it.internalOffset } +
                Circle.values().map { it.num + it.internalOffset } +
                Line.values().map { it.num + it.internalOffset }

    /**
     * @param cards 输入的麻将牌
     * @param arbitraryCount 输入的任意牌
     * @return 可以听的麻将牌
     *
     * @throws IllegalArgumentException 当arbitraryCount小于0时会抛出异常。
     * arbitraryCount和cards的个数之和必须等于13，否则抛出异常
     * */
    fun whichToWinMahjong(cards: List<Int>, arbitraryCount: Int): List<Mahjong> {
        return kotlin.runCatching {
            whichToWinUnderArbitraryOf(cards, arbitraryCount)
                .mapNotNull { it.innerIndexToMahjong() }
        }.onFailure { print(it.message + " -> ") }
            .getOrDefault(emptyList())
    }

    /**
     * @param cards 输入麻将牌的innerIndex
     * @param arbitraryCount 输入的任意牌
     * @return 可以听的麻将牌的innerIndex
     *
     * @throws IllegalArgumentException 当arbitraryCount小于0时会抛出异常。
     * arbitraryCount和cards的个数之和必须等于13，否则抛出异常
     * */
    private fun whichToWinUnderArbitraryOf(cards: List<Int>, arbitraryCount: Int): List<Int> {
        if (arbitraryCount < 0) throw IllegalArgumentException("输入任意牌数量不能小于0")
        else if (arbitraryCount + cards.size != 13) throw IllegalArgumentException("输入手牌不足13")
        val waitingCard: MutableList<Int> = ArrayList()
        val remain = IntArray(30)
        for (c in cards) {
            remain[c]++
        }
        for (other in allCards) {
            if (remain[other] >= 0 && remain[other] < 4) {
                remain[other]++
                val complete14Card = check14(remain, arbitraryCount)
                remain[other]--
                if (complete14Card) {
                    // 保存所有可能听的牌
                    waitingCard.add(other)
                }
            }
        }
        return waitingCard
    }

    private fun Int.innerIndexToMahjong(): Mahjong? {
        val innerIndex = this
        val num = innerIndex % 10
        return if (innerIndex > Line.ONE.internalOffset) {
            Line.values().find { it.num == num }
        } else if (innerIndex > Circle.ONE.internalOffset) {
            Circle.values().find { it.num == num }
        } else if (innerIndex > Character.ONE.internalOffset) {
            Character.values().find { it.num == num }
        } else null
    }

    /**
     * 判断是否胡牌
     */
    private fun check14(pai: IntArray, arbitraryCount: Int): Boolean {
        // 利用回溯来处理多种情况的判断
        return dfs(arbitraryCount, pai, 1, 4, 1)
    }

    /**
     * 递归
     * c3 刻/顺的个数
     * c2 对的数量
     */
    private fun dfs(
        arbitraryCount: Int,
        remain: IntArray,
        depth: Int,
        threeCount: Int,
        twoCount: Int
    ): Boolean {
        val lacks = remain.fold(0) { acc, each -> if (each < 0) acc + each else acc }
        if (arbitraryCount > 0 && Math.abs(lacks) > arbitraryCount) {
            return false
        } else if (threeCount < 0 || twoCount < 0) {
            return false
        }
        var i = depth
        val len = remain.size
        while (i < len && remain[i] <= 0) {
            i++;
        }
        if (i >= len) {
            return when {
                threeCount == 0 && twoCount == 0 -> {
                    Math.abs(lacks) == arbitraryCount
                }
                threeCount == 1 && twoCount == 0 -> {
                    Math.abs(lacks) + 3 == arbitraryCount
                }
                threeCount == 0 && twoCount == 0 -> {
                    Math.abs(lacks) + 2 == arbitraryCount
                }
                else -> false
            }
        }
        return judgeByThisCardRemain(
            thisCardRemain = remain[i],
            arbitraryCount = arbitraryCount,
            remain = remain,
            depth = i,
            threeCount = threeCount,
            twoCount = twoCount
        )
    }

    private fun judgeByThisCardRemain(
        thisCardRemain: Int,
        arbitraryCount: Int,
        remain: IntArray,
        depth: Int,
        threeCount: Int,
        twoCount: Int,
    ): Boolean {
        if (thisCardRemain < 0) {
            return false
        }
        val i = depth
        val len = remain.size
        when (thisCardRemain) {
            1 -> {
                // 赊账自己，凑成一对拿走
                remain[i] = remain[i] - 2
                val twins = dfs(arbitraryCount, remain, i + 1, threeCount, twoCount - 1)
                remain[i] = remain[i] + 2
                if (twins) {
                    return true
                }
                // 赊账2张连续的牌，拿一个顺
                // 可能是一左一右，可能是以i为起点连续的三张牌
                if (i + 2 < len) {
                    remain[i]--
                    remain[i + 1]--
                    remain[i + 2]--
                    val threeSequence = dfs(arbitraryCount, remain, i + 1, threeCount - 1, twoCount)
                    remain[i]++
                    remain[i + 1]++
                    remain[i + 2]++
                    if (threeSequence) return true
                }
                return false
            }
            2 -> {
                // 赊账拿走一个刻。虽然目前只有一对，但是尝试赊账一张任意牌成为顺
                remain[i] = remain[i] - 3
                val triple = dfs(arbitraryCount, remain, i + 1, threeCount - 1, twoCount)
                remain[i] = remain[i] + 3
                if (triple) {
                    return true
                }
                // 拿走一对
                remain[i] = remain[i] - 2
                val twins = dfs(arbitraryCount, remain, i + 1, threeCount, twoCount - 1)
                remain[i] = remain[i] + 2
                if (twins) {
                    return true
                }
                // 拿走两个顺
                if (i + 2 >= len) {
                    return false
                }
                remain[i] = remain[i] - 2
                remain[i + 1] = remain[i + 1] - 2
                remain[i + 2] = remain[i + 2] - 2
                val doubleThreeSequence =
                    dfs(arbitraryCount, remain, i + 1, threeCount - 2, twoCount)
                remain[i] = remain[i] + 2
                remain[i + 1] = remain[i + 1] + 2
                remain[i + 2] = remain[i + 2] + 2
                return doubleThreeSequence
            }
            else -> {
                // 拿走一个刻，可能多余3张，所以下标i不挪
                remain[i] = remain[i] - 3
                val triple = dfs(arbitraryCount, remain, i, threeCount - 1, twoCount)
                remain[i] = remain[i] + 3
                if (triple) {
                    return true
                }
                // 拿走一对
                // 不可能再出现3条顺的情况 因为等价于3个刻
                remain[i] = remain[i] - 2
                // i不用加1
                val twins = dfs(arbitraryCount, remain, i, threeCount, twoCount - 1)
                remain[i] = remain[i] + 2
                return twins
            }
        }
    }
}