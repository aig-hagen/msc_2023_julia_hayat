package de.julsched.beliefchange.sat.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BinaryCounter {

    private int numMax = 0;
    private List<String> encoding = new ArrayList<String>();
    private List<Integer> counterBitsReversed = new ArrayList<Integer>();

    public static int log2(int N){
        return (int)(Math.log(N) / Math.log(2));
    }

    public int getNumMax() {
        return numMax;
    }

    public List<String> getEncoding() {
        return encoding;
    }

    public List<Integer> getCounterBitsReversed() {
        return this.counterBitsReversed;
    }

    public List<String> createEncoding(List<Integer> variablesToCount) {
        if (variablesToCount == null || variablesToCount.size() == 0) {
            return new ArrayList<String>();
        }
        Collections.sort(variablesToCount);
        numMax = variablesToCount.get(variablesToCount.size() - 1);

        this.counterBitsReversed = count(variablesToCount);
        return this.encoding;
    }

    private List<Integer> count(List<Integer> variables) {
        if (variables.size() <= 1) {
            return variables;
        }

        List<Integer> result = new ArrayList<Integer>();
        List<List<Integer>> halves = splitIntoHalves(variables);
        List<Integer> half1 = count(halves.get(0));
        List<Integer> half2 = count(halves.get(1));
        int separateBit = halves.get(2).get(0);

        int carry = separateBit;
        for (int i = 0; half1.size() > i; i++) {
            int result2Bit = (half2.size() > i) ? half2.get(i) : 0;
            int newCarry = numMax + 1;
            int newSum = numMax + 2;
            List<String> clauses;
            if (result2Bit == 0) {
                clauses = HalfAdder.getClauses(half1.get(i), carry, newCarry, newSum);
            } else {
                clauses = FullAdder.getClauses(half1.get(i), result2Bit, carry, newCarry, newSum);
            }
            for (String clause : clauses) {
                encoding.add(clause);
            }
            numMax = newSum;
            result.add(newSum);
            carry = newCarry;
        }
        result.add(carry);
        return result;
    }

    List<List<Integer>> splitIntoHalves(List<Integer> list) {
        List<List<Integer>> result = new ArrayList<List<Integer>>();
        result.add(getFirstHalf(list));
        result.add(getSecondHalf(list));
        result.add(getSeparateBit(list));
        return result;
    }

    private List<Integer> getFirstHalf(List<Integer> list) {
        int n = list.size();
        int m = log2(n);
        // First half needs to consist of 2^m - 1 elements
        int border = (int) Math.pow(2, m);
        return list.subList(0, border - 1);
    }

    private List<Integer> getSecondHalf(List<Integer> list) {
        int n = list.size();
        int m = log2(n);
        if (n == (int) Math.pow(2, m)) {
            // Second half is empty when list has length 2^m
            // since first half always contains 2^m - 1 elements
            // and one separate bit is mandatory
            return new ArrayList<Integer>();
        } else {
            int border = (int) Math.pow(2, m);
            return list.subList(border - 1, n - 1); //last index of list will not be included
        }
    }

    private List<Integer> getSeparateBit(List<Integer> list) {
        List<Integer> result = new ArrayList<Integer>();
        result.add(list.get(list.size() - 1));
        return result;
    }
}
