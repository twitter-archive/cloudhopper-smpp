package com.cloudhopper.smpp.util;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2012 Cloudhopper by Twitter
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.type.*;
import com.cloudhopper.smpp.tlv.Tlv;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class ChannelBufferUtil {
    private static final Logger logger = LoggerFactory.getLogger(ChannelBufferUtil.class);

    /**
     * Read and create a new Address from a buffer.  Checks if there is
     * a minimum number of bytes readable from the buffer.
     * @param buffer
     * @return
     * @throws UnrecoverablePduEncodingException
     * @throws RecoverablePduEncodingException
     */
    static public Address readAddress(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        // an address is at least 3 bytes long (ton, npi, and null byte)
        if (buffer.readableBytes() < 3) {
            throw new NotEnoughDataInBufferException("Parsing address", buffer.readableBytes(), 3);
        }
        Address address = new Address();
        address.read(buffer);
        return address;
    }

    /**
     * Writes an address to a buffer.  If the address is null, this method will
     * safely write out the SmppConstants.EMPTY_ADDRESS instance.
     * @param buffer
     * @param value
     * @throws UnrecoverablePduEncodingException
     * @throws RecoverablePduEncodingException
     */
    static public void writeAddress(ChannelBuffer buffer, Address value) throws UnrecoverablePduException, RecoverablePduException {
        if (value == null) {
            SmppConstants.EMPTY_ADDRESS.write(buffer);
        } else {
            value.write(buffer);
        }
    }

    /**
     * Read and create a list of submit_multi addresses from a buffer.
     *
     * @param buffer the buffer to read from
     * @return submit_multi address list
     */
    public static List<SubmitMultiDestinationAddress> readSubmitMultiAddressList(ChannelBuffer buffer)
            throws UnrecoverablePduException, RecoverablePduException {
        /* submit_multi has to have at least 3 bytes:
         * 1 byte for number_of_dests field
         * 1 byte for dest_flag field
         * at least 1 possible null byte in case of empty distribution list name */
        int requiredReadableBytes = 3;
        int readableBytes = buffer.readableBytes();

        if (readableBytes < requiredReadableBytes) {
            throw new NotEnoughDataInBufferException(
                    "Parsing submit_multi destination address", readableBytes, requiredReadableBytes);
        }

        final int numberOfDestinations = buffer.readByte();
        final List<SubmitMultiDestinationAddress> addressList =
                new ArrayList<SubmitMultiDestinationAddress>(numberOfDestinations);

        for (int i = 0; i < numberOfDestinations; i++) {
            final SubmitMultiDestinationAddress address = new SubmitMultiDestinationAddress();
            address.read(buffer);
            addressList.add(address);
        }

        return addressList;
    }

    /**
     * Writes an submit_multi destination address structure to a buffer
     *
     * @param buffer      the buffer to write to
     * @param addressList submit_multi address list
     */
    public static void writeSubmitMultiAddressList(ChannelBuffer buffer, List<SubmitMultiDestinationAddress> addressList)
            throws UnrecoverablePduException, RecoverablePduException {

        if (addressList == null || addressList.isEmpty()) {
            String msg = "submit_multi must contain at least 1 SME address or distribution list name";
            throw new SmppInvalidArgumentException(msg);
        } else if (addressList.size() > 254) {
            String msg = "A maximum of 254 destination addresses are allowed, actual count was " + addressList.size();
            throw new SmppInvalidArgumentException(msg);
        } else {
            buffer.writeByte(addressList.size());
            for (SubmitMultiDestinationAddress address : addressList) {
                address.write(buffer);
            }
        }
    }

    public static void writeSubmitMultiUnsuccessSmeList(ChannelBuffer buffer, List<SubmitMultiUnsuccessSme> list)
            throws UnrecoverablePduException, RecoverablePduException {

        int numberOfUnsuccessSmes = list == null || list.isEmpty() ? 0 : list.size();
        buffer.writeByte(numberOfUnsuccessSmes);
        if (numberOfUnsuccessSmes > 0) {
            for (SubmitMultiUnsuccessSme unsuccessSme : list) {
                unsuccessSme.writeBody(buffer);
            }
        }
    }

    public static List<SubmitMultiUnsuccessSme> readSubmitMultiUnsuccessSmeList(ChannelBuffer buffer)
            throws UnrecoverablePduException, RecoverablePduException {

        List<SubmitMultiUnsuccessSme> list = null;
        byte numberOfUnsuccess = buffer.readByte();

        if (numberOfUnsuccess > 0) {
            list = new ArrayList<SubmitMultiUnsuccessSme>(numberOfUnsuccess);
            for (int i = 0; i < numberOfUnsuccess; i++) {
                SubmitMultiUnsuccessSme unsuccessSme = new SubmitMultiUnsuccessSme();
                unsuccessSme.readBody(buffer);
                list.add(unsuccessSme);
            }
        }

        return list;
    }

    /**
     * Reads a TLV from a buffer. This method is greedy and will read bytes
     * even if it won't be able to successfully complete.  It's assumed this
     * method will only be called if its known ahead of time that all bytes
     * will be available ahead of time.
     * @param buffer The buffer to read from
     * @return A new TLV instance
     * @throws NotEnoughDataInBufferException
     */
    static public Tlv readTlv(ChannelBuffer buffer) throws NotEnoughDataInBufferException {
        // a TLV is at least 4 bytes (tag+length)
        if (buffer.readableBytes() < 4) {
            throw new NotEnoughDataInBufferException("Parsing TLV tag and length", buffer.readableBytes(), 4);
        }

        short tag = buffer.readShort();
        int length = buffer.readUnsignedShort();

        // check if we have enough data for the TLV
        if (buffer.readableBytes() < length) {
            throw new NotEnoughDataInBufferException("Parsing TLV value", buffer.readableBytes(), length);
        }

        byte[] value = new byte[length];
        buffer.readBytes(value);

        return new Tlv(tag, value);
    }
    
    static public void writeTlv(ChannelBuffer buffer, Tlv tlv) throws NotEnoughDataInBufferException {
        // a null is a noop
        if (tlv == null) {
            return;
        }
        buffer.writeShort(tlv.getTag());
        buffer.writeShort(tlv.getLength());
        if (tlv.getValue() != null) {
            buffer.writeBytes(tlv.getValue());
        }
    }

    /**
     * Writes a C-String (null terminated) to a buffer.  If the String is null
     * this method will only write out the NULL byte (0x00) to the buffer.
     * @param buffer
     * @param value
     * @throws UnsupportedEncodingException
     */
    static public void writeNullTerminatedString(ChannelBuffer buffer, String value) throws UnrecoverablePduException {
        if (value != null) {
            try {
                byte[] bytes = value.getBytes("ISO-8859-1");
                buffer.writeBytes(bytes);
            } catch (UnsupportedEncodingException e) {
                throw new UnrecoverablePduException(e.getMessage(), e);
            }
        }
        // always write null byte
        buffer.writeByte((byte)0x00);
    }

    /**
     * Reads a C-String (null terminated) from a buffer.  This method will
     * attempt to find the null byte and read all data up to and including
     * the null byte.  The returned String does not include the null byte.
     * Will throw an exception if no null byte is found and it runs out of data
     * in the buffer to read.
     * @param buffer
     * @return
     * @throws TerminatingNullByteNotFoundException
     */
    static public String readNullTerminatedString(ChannelBuffer buffer) throws TerminatingNullByteNotFoundException {
        // maximum possible length are the readable bytes in buffer
        int maxLength = buffer.readableBytes();

        // if there are no readable bytes, return null
        if (maxLength == 0) {
            return null;
        }

        // the reader index is defaulted to the readerIndex
        int offset = buffer.readerIndex();
        int zeroPos = 0;

        // search for NULL byte until we hit end or find it
        while ((zeroPos < maxLength) && (buffer.getByte(zeroPos+offset) != 0x00)) {
            zeroPos++;
        }

        if (zeroPos >= maxLength) {
            // a NULL byte was not found
            throw new TerminatingNullByteNotFoundException("Terminating null byte not found after searching [" + maxLength + "] bytes");
        }

        // at this point, we found a terminating zero
        String result = null;
        if (zeroPos > 0) {
            // read a new byte array
            byte[] bytes = new byte[zeroPos];
            buffer.readBytes(bytes);
            try {
                result = new String(bytes, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                logger.error("Impossible error", e);
            }
        } else {
            result = "";
        }

        // at this point, we have just one more byte to skip over (the null byte)
        byte b = buffer.readByte();
        if (b != 0x00) {
            logger.error("Impossible error: last byte read SHOULD have been a null byte, but was [" + b + "]");
        }

        return result;
    }

}
