/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.cloudhopper.smpp.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

/**
 * The Class StatisticsSample.
 */
public final class StatisticsSample {

    private static final String SEPARATOR = ";";
    private static final String EMPTY_VALUE = "";
    private static final String NUMBER_FORMAT = "0.00";
    private static final int BUFF_LENGTH = 64;
    private static final int FRACTION_DIGITS = 2;

    private long itsSum;
    private long itsCount;
    private long itsMinimum;
    private long itsMaximum;

    private final NumberFormat itsNumberFormat;

    /**
     * Instantiates a new statistics sample.
     */
    public StatisticsSample() {
        final DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        itsNumberFormat = new DecimalFormat(NUMBER_FORMAT, dfs);
        itsNumberFormat.setMinimumIntegerDigits(1);
        itsNumberFormat.setMinimumFractionDigits(FRACTION_DIGITS);
        itsNumberFormat.setMaximumFractionDigits(FRACTION_DIGITS);
    }

    /**
     * Sample.
     *
     * @param aValue the value
     */
    public void sample(final long aValue) {
        synchronized (this) {
            itsCount++;
            itsSum += aValue;
            if (aValue < itsMinimum) {
                itsMinimum = aValue;
            }
            if (aValue > itsMaximum) {
                itsMaximum = aValue;
            }
        }
    }

    /**
     * Gets the value and reset.
     *
     * @return the value
     */
    public String getAndReset() {
        synchronized (this) {
            final String r = (new StringBuilder(BUFF_LENGTH)).append(itsCount).append(SEPARATOR).append(getMinimum())
                    .append(SEPARATOR).append(getMaximum()).append(SEPARATOR).append(getAverage()).toString();
            itsSum = 0L;
            itsCount = 0L;
            itsMinimum = Long.MAX_VALUE;
            itsMaximum = Long.MIN_VALUE;
            return r;
        }
    }

    /**
     * Reset.
     */
    public void reset() {
        synchronized (this) {
            itsSum = 0L;
            itsCount = 0L;
            itsMinimum = Long.MAX_VALUE;
            itsMaximum = Long.MIN_VALUE;
        }
    }

    private String getMinimum() {
        if (itsMinimum == Long.MAX_VALUE) {
            return EMPTY_VALUE;
        }
        return String.valueOf(itsMinimum);
    }

    private String getMaximum() {
        if (itsMaximum == Long.MIN_VALUE) {
            return EMPTY_VALUE;
        }
        return String.valueOf(itsMaximum);
    }

    private String getAverage() {
        if (itsCount == 0L) {
            return EMPTY_VALUE;
        }
        return itsNumberFormat.format((double) itsSum / itsCount);
    }

}
