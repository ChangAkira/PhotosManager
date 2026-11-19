package com.example.photosmanager

import com.example.photosmanager.data.repository.PreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 「已帮您删除超过 xxx 张」这条喜报的分档判断。
 *
 * 只统计跨过整百的那一下：漏了会永远不庆祝，误报更糟——每删一张弹一条喜报，
 * 用户大概不会感谢这个。这条测试钉住几种最容易写错的情形。
 */
class DeletedMilestoneTest {

    @Test
    fun `跨过整百报那一个百`() {
        assertEquals(100, PreferencesRepository.deletedMilestone(95, 105))
        assertEquals(100, PreferencesRepository.deletedMilestone(99, 100))
        assertEquals(100, PreferencesRepository.deletedMilestone(0, 100))
        assertEquals(200, PreferencesRepository.deletedMilestone(150, 210))
    }

    /** 一次删掉好几千，只报最高的那个百；连弹三条一样的喜报没有意义 */
    @Test
    fun `一次跨多个整百只报最高的那个`() {
        assertEquals(300, PreferencesRepository.deletedMilestone(190, 305))
        assertEquals(2500, PreferencesRepository.deletedMilestone(0, 2500))
    }

    @Test
    fun `没跨过整百不报`() {
        assertNull(PreferencesRepository.deletedMilestone(3, 40))
        assertNull(PreferencesRepository.deletedMilestone(101, 150))
        assertNull(PreferencesRepository.deletedMilestone(99, 99))
    }

    /** 计数只该往前走：删除失败回滚、或者哪天加了手动改数，都不能把喜报刷出来 */
    @Test
    fun `数字没变或者变小不报`() {
        assertNull(PreferencesRepository.deletedMilestone(300, 300))
        assertNull(PreferencesRepository.deletedMilestone(300, 120))
        assertNull(PreferencesRepository.deletedMilestone(100, 0))
    }

    @Test
    fun `步长就是整百`() {
        assertEquals(100, PreferencesRepository.DELETED_MILESTONE_STEP)
    }
}
