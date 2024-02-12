package de.julsched.beliefchange.sat.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BinaryCounterTest {

    BinaryCounter binaryCounter;

    @BeforeEach
    public void initBinaryCounter() {
        binaryCounter = new BinaryCounter();
    }

    @Test
    public void testSplitIntoHalves() {
        // List with two elements
        List<Integer> input = Arrays.asList(10, 11);
        List<List<Integer>> result = new ArrayList<List<Integer>>();
        List<Integer> firstHalf = Arrays.asList(10);
        List<Integer> secondHalf = new ArrayList<Integer>();
        List<Integer> separateBit = Arrays.asList(11);
        result.add(firstHalf);
        result.add(secondHalf);
        result.add(separateBit);
        assertEquals(result, binaryCounter.splitIntoHalves(input));

        // List with three elements
        input = Arrays.asList(2, 3, 4);
        result = new ArrayList<List<Integer>>();
        firstHalf = Arrays.asList(2);
        secondHalf = Arrays.asList(3);
        separateBit = Arrays.asList(4);
        result.add(firstHalf);
        result.add(secondHalf);
        result.add(separateBit);
        assertEquals(result, binaryCounter.splitIntoHalves(input));

        // List with four elements
        input = Arrays.asList(1, 2, 3, 4);
        result = new ArrayList<List<Integer>>();
        firstHalf = Arrays.asList(1, 2, 3);
        secondHalf = new ArrayList<Integer>();
        separateBit = Arrays.asList(4);
        result.add(firstHalf);
        result.add(secondHalf);
        result.add(separateBit);
        assertEquals(result, binaryCounter.splitIntoHalves(input));

        // List with eight elements
        input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        result = new ArrayList<List<Integer>>();
        firstHalf = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        secondHalf = new ArrayList<Integer>();
        separateBit = Arrays.asList(8);
        result.add(firstHalf);
        result.add(secondHalf);
        result.add(separateBit);
        assertEquals(result, binaryCounter.splitIntoHalves(input));
    }

    @Test
    public void testCreateEncoding() {
        List<Integer> input = Arrays.asList();
        List<Integer> bits = new ArrayList<Integer>();
        List<String> encoding = new ArrayList<String>();
        assertEquals(encoding, binaryCounter.createEncoding(input));
        assertEquals(bits, binaryCounter.getCounterBitsReversed());

        binaryCounter = new BinaryCounter();
        input = Arrays.asList(1);
        bits = Arrays.asList(1);
        encoding = new ArrayList<String>();
        assertEquals(encoding, binaryCounter.createEncoding(input));
        assertEquals(bits, binaryCounter.getCounterBitsReversed());

        binaryCounter = new BinaryCounter();
        input = Arrays.asList(1, 2);
        bits = Arrays.asList(4, 3);
        encoding = Arrays.asList(new String[] {
            "-1 -2 3 0",
            "1 -2 4 0",
            "-1 2 4 0"
        });
        assertEquals(encoding, binaryCounter.createEncoding(input));
        assertEquals(bits, binaryCounter.getCounterBitsReversed());

        binaryCounter = new BinaryCounter();
        input = Arrays.asList(1, 2, 3, 4, 5);
        bits = Arrays.asList(9, 11, 10);
        encoding = Arrays.asList(new String[] {
            "-2 -3 6 0",
            "-1 -3 6 0",
            "-1 -2 6 0",
            "1 2 -3 7 0",
            "1 -2 3 7 0",
            "-1 2 3 7 0",
            "-1 -2 -3 7 0",
            "-4 -5 8 0",
            "-7 -5 8 0",
            "-7 -4 8 0",
            "7 4 -5 9 0",
            "7 -4 5 9 0",
            "-7 4 5 9 0",
            "-7 -4 -5 9 0",
            "-6 -8 10 0",
            "6 -8 11 0",
            "-6 8 11 0"
        });
        assertEquals(encoding, binaryCounter.createEncoding(input));
        assertEquals(bits, binaryCounter.getCounterBitsReversed());
    }
}
