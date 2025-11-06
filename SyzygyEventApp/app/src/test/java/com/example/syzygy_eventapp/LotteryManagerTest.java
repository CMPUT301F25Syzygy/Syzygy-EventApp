package com.example.syzygy_eventapp;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class LotteryManagerTest {
    private LotteryManager lotteryManager;

    @Before
    public void setup() {
        lotteryManager = new LotteryManager(null, null);
    }

    @Test
    public void testSelectWinners_LessThanMaxAttendees() {
        List<String> waitingList = Arrays.asList("A", "B", "C");
        List<String> winners = lotteryManager.selectWinners(waitingList, 5);

        assertEquals(3, winners.size());
        assertTrue(waitingList.containsAll(winners));
    }

    @Test
    public void testSelectWinners_ExactlyMaxAttendees() {
        List<String> waitingList = Arrays.asList("A", "B", "C", "D");
        List<String> winners = lotteryManager.selectWinners(waitingList, 4);

        assertEquals(4, winners.size());
    }

    @Test
    public void testSelectWinners_MoreThanMaxAttendees() {
        List<String> waitingList = Arrays.asList("A", "B", "C", "D", "E");
        List<String> winners = lotteryManager.selectWinners(waitingList, 3);

        assertEquals(3, winners.size());
        assertTrue(waitingList.containsAll(winners));
    }

    @Test
    public void testSelectWinners_EmptyList() {
        List<String> winners = lotteryManager.selectWinners(null, 3);
        assertTrue(winners.isEmpty());
    }
}
