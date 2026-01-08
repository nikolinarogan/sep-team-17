package com.bank.service;

import org.springframework.stereotype.Service;

@Service
public class LuhnValidator {

    public boolean checkLuhn(String cardNumber) {
        int nDigits = cardNumber.length();
        int nSum = 0;
        boolean isSecond = false;

        for (int i = nDigits - 1; i >= 0; i--) {
            int d = cardNumber.charAt(i) - '0';

            if (isSecond == true)
                d = d * 2;

            // Ako je duplirana cifra > 9, saberi cifre (npr. 14 -> 1+4=5, što je isto što i 14-9)
            nSum += d / 10;
            nSum += d % 10;

            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }
}